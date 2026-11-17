package com.binaris.wizardry.content.loot;

import com.binaris.wizardry.api.content.spell.*;
import com.binaris.wizardry.api.content.util.RegistryUtils;
import com.binaris.wizardry.core.EBLogger;
import com.binaris.wizardry.api.content.data.SpellManagerData;
import com.binaris.wizardry.content.item.ScrollItem;
import com.binaris.wizardry.content.item.SpellBookItem;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.EBLootFunctions;
import com.binaris.wizardry.setup.registries.Spells;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/// A loot table function that assigns a randomly chosen spell to a spell book or scroll,
/// with optional filtering by spell lists, elements, tiers, and a bias toward undiscovered
/// spells for the looting player.
///
/// The selection pipeline narrows the full spell registry through four optional filters:
/// an explicit spell whitelist, a tier whitelist, an element whitelist, and item-context
/// checks ({@code SpellContexts.BOOK} / {@code SpellContexts.SCROLL}). If the looting
/// context has a player and {@code undiscoveredBias > 0}, the function reads the player's
/// {@code SpellManagerData} and applies a bias that can either favor or penalize already-discovered
/// spells. The bias is amplified by +0.4 (capped at 0.9) if the player has the
/// spell discovery charm equipped. Falls back to {@code Spells.MAGIC_MISSILE} if no valid
/// spell remains.
public class RandomSpellFunction extends LootItemConditionalFunction {
    public static final com.mojang.serialization.MapCodec<RandomSpellFunction> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> commonFields(instance).and(instance.group(
                    ResourceLocation.CODEC.listOf().optionalFieldOf("spells", List.of())
                            .forGetter((RandomSpellFunction f) -> f.spells == null ? List.of() : f.spells.stream().map(Spell::getLocation).toList()),
                    com.mojang.serialization.Codec.BOOL.optionalFieldOf("ignore_weighting", false).forGetter(f -> f.ignoreWeighting),
                    com.mojang.serialization.Codec.FLOAT.optionalFieldOf("undiscovered_bias", 0.0f).forGetter(f -> f.undiscoveredBias),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("tiers", List.of())
                            .forGetter((RandomSpellFunction f) -> f.tiers == null ? List.of() : f.tiers.stream().map(SpellTier::getOrCreateLocation).toList()),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("elements", List.of())
                            .forGetter((RandomSpellFunction f) -> f.elements == null ? List.of() : f.elements.stream().map(Element::getLocation).toList())
            )).apply(instance, (conditions, spells, weighting, bias, tiers, elements) ->
                    new RandomSpellFunction(conditions,
                            spells.stream().map(Services.REGISTRY_UTIL::getSpell).filter(Objects::nonNull).toList(), weighting, bias,
                            tiers.stream().map(Services.REGISTRY_UTIL::getTier).filter(Objects::nonNull).toList(),
                            elements.stream().map(Services.REGISTRY_UTIL::getElement).filter(Objects::nonNull).toList())));
    private final @Nullable List<Spell> spells;
    private final @Nullable List<Element> elements;
    private final @Nullable List<SpellTier> tiers;
    private final boolean ignoreWeighting;
    private final float undiscoveredBias;

    protected RandomSpellFunction(List<LootItemCondition> conditions, @Nullable List<Spell> spells, boolean ignoreWeighting, float undiscoveredBias, @Nullable List<SpellTier> tiers, @Nullable List<Element> elements) {
        super(conditions);
        this.spells = spells;
        this.ignoreWeighting = ignoreWeighting;
        this.undiscoveredBias = undiscoveredBias;
        this.tiers = tiers;
        this.elements = elements;
    }

    /// Creates a builder for a {@code RandomSpellFunction} with the given parameters.
    /// Used by data generation to construct loot table entries programmatically.
    ///
    /// @param spells optional whitelist of allowed spells, or null/empty for no filter.
    /// @param ignoreWeighting if true, ignores spell weighting.
    /// @param undiscoveredBias bias toward undiscovered spells (0 to disable).
    /// @param tiers optional whitelist of allowed tiers, or null/empty for no filter.
    /// @param elements optional whitelist of allowed elements, or null/empty for no filter.
    /// @return a loot function builder configured with these parameters.
    public static LootItemConditionalFunction.Builder<?> setRandomSpell(List<Spell> spells, boolean ignoreWeighting, float undiscoveredBias, List<SpellTier> tiers, List<Element> elements) {
        return simpleBuilder((conditions) ->
                new RandomSpellFunction(conditions, spells, ignoreWeighting, undiscoveredBias, tiers, elements));
    }

    @Override
    public @NotNull LootItemFunctionType getType() {
        return EBLootFunctions.RANDOM_SPELL;
    }

    @Override
    protected @NotNull ItemStack run(ItemStack stack, @NotNull LootContext lootContext) {
        if (!(stack.getItem() instanceof SpellBookItem) && !(stack.getItem() instanceof ScrollItem))
            EBLogger.warn("Applying the random_spell loot function to an item that isn't a spell book or scroll.");

        SpellContext context = !lootContext.hasParam(LootContextParams.THIS_ENTITY) ? SpellContexts.TREASURE : SpellContexts.LOOTING;
        Player player = null;
        if (lootContext.hasParam(LootContextParams.THIS_ENTITY) && lootContext.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player player1) {
            player = player1;
        }

        Spell spell = pickRandomSpell(stack, lootContext.getRandom(), context, player);

        if (spell == Spells.NONE) return RegistryUtils.setSpell(stack, Spells.MAGIC_MISSILE);
        return RegistryUtils.setSpell(stack, spell);
    }

    /// Selects a random spell from the registry after applying all configured filters
    /// and the undiscovered spell bias.
    ///
    /// The filtering pipeline applies in order: explicit spell whitelist, context-enabled
    /// check, tier filter, element filter, item-type context filter (BOOK/SCROLL), and
    /// finally the undiscovered spell bias. The bias works by randomly keeping either
    /// only discovered or only undiscovered spells with probability {@code 0.5 + 0.5 * bias}.
    /// The bias is increased if the player wears the spell discovery charm.
    ///
    /// @param stack the loot item (spell book or scroll) being modified.
    /// @param random the random source for selection.
    /// @param context the spell context (LOOTING or TREASURE).
    /// @param player the looting player, or null if no player is involved.
    /// @return the chosen spell, or Spells.NONE if no spell matched (converted to
    ///         MAGIC_MISSILE by the caller).
    private Spell pickRandomSpell(ItemStack stack, RandomSource random, SpellContext context, @Nullable Player player) {
        ArrayList<Spell> possibleSpells = new ArrayList<>(Services.REGISTRY_UTIL.getSpells());

        // Checking spells, if the spells list is specified
        if (spells != null && !spells.isEmpty()) possibleSpells.retainAll(spells);

        possibleSpells.removeIf(possibleSpell -> !possibleSpell.isEnabled(context));

        // Checking tiers, if the tiers list is specified
        if (tiers != null && !tiers.isEmpty()) {
            possibleSpells.removeIf(possibleSpell -> !tiers.contains(possibleSpell.getTier()));
        }

        // Checking elements, if the elements list is specified
        if (elements != null && !elements.isEmpty()) {
            possibleSpells.removeIf(possibleSpell -> !elements.contains(possibleSpell.getElement()));
        }

        if (stack.getItem() instanceof SpellBookItem)
            possibleSpells.removeIf(spell -> !spell.isEnabled(SpellContexts.BOOK));
        if (stack.getItem() instanceof ScrollItem)
            possibleSpells.removeIf(spell -> !spell.isEnabled(SpellContexts.SCROLL));

        if (player != null && undiscoveredBias > 0) {
            float bias = undiscoveredBias;
            if (ArtifactChannel.isEquipped(player, EBItems.CHARM_SPELL_DISCOVERY.get()))
                bias = Math.min(bias + 0.4f, 0.9f);
            if (bias > 0) {
                SpellManagerData data = Services.OBJECT_DATA.getSpellManagerData(player);
                int discoveredCount = (int) possibleSpells.stream().filter(data::hasSpellBeenDiscovered).count();
                if (discoveredCount > 0 && discoveredCount < possibleSpells.size()) {
                    boolean keepDiscovered = random.nextFloat() > 0.5f + 0.5f * undiscoveredBias;
                    possibleSpells.removeIf(s -> keepDiscovered != data.hasSpellBeenDiscovered(s));
                }
            }
        }

        if (possibleSpells.isEmpty()) return Spells.NONE; // don't worry, this is converted to Magic Missile in run();
        return possibleSpells.get(random.nextInt(possibleSpells.size()));
    }

    /// Serializer for {@code RandomSpellFunction} that handles JSON (de)serialization
    /// and is registered as {@code EBLootFunctions.RANDOM_SPELL}.
    ///
    /// Supports serializing optional fields: {@code spells}, {@code tiers}, and
    /// {@code elements} as lists of resource locations (encoded via Codec), and the
    /// scalar fields {@code ignore_weighting} and {@code undiscovered_bias}. All fields
    /// are optional — omitted fields default to null/0/false. On deserialization,
    /// invalid resource locations in the tiers or elements lists are logged as warnings
    /// and filtered out rather than throwing.
}
