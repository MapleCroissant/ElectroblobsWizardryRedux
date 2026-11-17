package com.binaris.wizardry.gametest;

import com.binaris.wizardry.api.content.spell.SpellCondition;
import com.binaris.wizardry.api.content.util.CastItemDataHelper;
import com.binaris.wizardry.api.content.util.EnchantmentUtil;
import com.binaris.wizardry.api.content.util.ItemData;
import com.binaris.wizardry.content.ConditionalAttributeModifier;
import com.binaris.wizardry.content.WizardryAttributeModifier;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;
import java.util.UUID;

/** Regression coverage for representations changed by the 1.21 migration. */
public class MigrationTest {
    @GameTest(template = "ebwizardry:empty")
    public static void legacyCustomStacksAndTradesLoad(GameTestHelper helper) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CompoundTag legacy = net.minecraft.nbt.TagParser.parseTag("{id:'ebwizardry:spectral_sword',Count:1b,tag:{cardinal_components:{'ebwizardry:conjure':{is_summoned:1b,duration:600,expire_time:900L}}}}");
        var stack = ItemData.load(helper.getLevel().registryAccess(), legacy);
        var data = Services.OBJECT_DATA.getConjureData(stack);
        if (data == null || !data.isSummoned() || data.getExpireTime() != 900) {
            helper.fail("Legacy custom stack lost its CCA conjure data");
        }
        var wizard = com.binaris.wizardry.setup.registries.EBEntities.WIZARD.get().create(helper.getLevel());
        CompoundTag saved = wizard.saveWithoutId(new CompoundTag());
        saved.put("trades", net.minecraft.nbt.TagParser.parseTag("{Recipes:[{buy:{id:'minecraft:gold_ingot',Count:3b},sell:{id:'ebwizardry:spell_book',Count:1b,tag:{migration_test:17}},uses:2,maxUses:12,xp:5,priceMultiplier:0.2f}]}"));
        wizard.load(saved);
        var offer = wizard.getOffers().getFirst();
        if (offer.getUses() != 2 || ItemData.read(offer.getResult()).getInt("migration_test") != 17) {
            helper.fail("Legacy wizard trades lost uses or custom result data");
        }
        wizard.load(wizard.saveWithoutId(new CompoundTag()));
        helper.succeed();
    }

    @GameTest(template = "ebwizardry:empty")
    public static void allStructureTemplatesLoad(GameTestHelper helper) {
        var manager = helper.getLevel().getStructureManager();
        int checked = 0;
        for (var id : helper.getLevel().getServer().getResourceManager()
                .listResources("structure", path -> path.getNamespace().equals("ebwizardry") && path.getPath().endsWith(".nbt")).keySet()) {
            var templateId = id.withPath(id.getPath().substring("structure/".length(), id.getPath().length() - 4));
            if (manager.get(templateId).isEmpty()) helper.fail("Could not load structure " + templateId);
            checked++;
        }
        if (checked != 30) helper.fail("Expected 30 preserved structure templates, found " + checked);
        helper.succeed();
    }

    private static ItemStack roundTrip(GameTestHelper helper, ItemStack stack) {
        var registries = helper.getLevel().registryAccess();
        return ItemStack.parseOptional(registries, (CompoundTag) stack.save(registries));
    }

    @GameTest(template = "ebwizardry:empty")
    public static void castDataPersistsAndCopiesIndependently(GameTestHelper helper) {
        ItemStack wand = new ItemStack(EBItems.NOVICE_WAND.get());
        CastItemDataHelper.setSpells(wand, List.of(Spells.MAGIC_MISSILE, Spells.HEAL));
        CastItemDataHelper.setCurrentSpell(wand, 1);
        CastItemDataHelper.setProgression(wand, 17);
        CastItemDataHelper.setCurrentCooldown(wand, 40, 200);
        ItemStack restored = roundTrip(helper, wand);
        if (CastItemDataHelper.getCurrentSpell(restored) != Spells.HEAL
                || CastItemDataHelper.getProgression(restored) != 17
                || CastItemDataHelper.getCurrentCooldown(restored, 210) != 30) {
            helper.fail("Wand spell selection, progression, or cooldown did not survive saving");
        }
        ItemStack copy = restored.copy();
        CastItemDataHelper.setProgression(copy, 50);
        if (CastItemDataHelper.getProgression(restored) != 17) helper.fail("Copy mutation changed the original wand");
        helper.succeed();
    }

    @GameTest(template = "ebwizardry:empty")
    public static void conjureDataPersistsAndCopiesIndependently(GameTestHelper helper) {
        ItemStack sword = new ItemStack(EBItems.SPECTRAL_SWORD.get());
        var data = Services.OBJECT_DATA.getConjureData(sword);
        data.setSummoned(true);
        data.setDuration(600);
        data.setExpireTime(900);
        ItemStack restored = roundTrip(helper, sword);
        var restoredData = Services.OBJECT_DATA.getConjureData(restored);
        if (!restoredData.isSummoned() || restoredData.getDuration() != 600 || restoredData.getExpireTime() != 900) {
            helper.fail("Conjured lifetime did not survive saving");
        }
        restoredData.setExpireTime(1200);
        if (data.getExpireTime() != 900) helper.fail("Conjure component was shared mutably between stacks");
        helper.succeed();
    }

    @GameTest(template = "ebwizardry:empty")
    public static void imbuementRemainsAccessibleAfterEnchanting(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        var data = Services.OBJECT_DATA.getImbuementData(sword);
        data.addImbuement(Enchantments.FIRE_ASPECT, 900);
        sword.enchant(EnchantmentUtil.get(helper.getLevel(), Enchantments.FIRE_ASPECT), 1);
        var restoredData = Services.OBJECT_DATA.getImbuementData(roundTrip(helper, sword));
        if (restoredData == null || restoredData.getExpirationTime(Enchantments.FIRE_ASPECT) != 900) {
            helper.fail("Temporary enchantment tracking disappeared after enchanting or saving");
        }
        helper.succeed();
    }

    @GameTest(template = "ebwizardry:empty")
    public static void conditionalAttributePersists(GameTestHelper helper) {
        SpellCondition condition = new SpellCondition(List.of(new SpellCondition.SingleCondition(
                SpellCondition.Type.SPELL, Spells.HEAL.getLocation())));
        AttributeModifier modifier = WizardryAttributeModifier.create(UUID.randomUUID(), "healing", 0.5,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, condition);
        AttributeModifier restored = AttributeModifier.load(modifier.save());
        if (restored == null || !condition.equals(((ConditionalAttributeModifier) (Object) restored).ebwizardry$getCondition())) {
            helper.fail("Conditional spell modifier did not survive saving");
        }
        helper.succeed();
    }

    @GameTest(template = "ebwizardry:empty")
    public static void allRegisteredEntitiesSaveAndLoad(GameTestHelper helper) {
        int checked = 0;
        for (var type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!id.getNamespace().equals("ebwizardry")) continue;
            var entity = type.create(helper.getLevel());
            if (entity == null) helper.fail("Could not create registered entity " + id);
            CompoundTag tag = entity.saveWithoutId(new CompoundTag());
            entity.load(tag);
            checked++;
        }
        if (checked == 0) helper.fail("No Wizardry entities were checked");
        helper.succeed();
    }

    @GameTest(template = "ebwizardry:empty")
    public static void altarRecipesSynchronize(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(
                com.binaris.wizardry.setup.registries.EBRecipeTypes.IMBUEMENT_ALTAR);
        if (recipes.isEmpty()) helper.fail("No imbuement altar recipes loaded");
        for (var holder : recipes) {
            var recipe = holder.value();
            var json = com.binaris.wizardry.content.recipe.ImbuementAltarRecipe.Serializer.CODEC.codec()
                    .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, recipe).getOrThrow();
            com.binaris.wizardry.content.recipe.ImbuementAltarRecipe.Serializer.CODEC.codec()
                    .parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                var codec = com.binaris.wizardry.content.recipe.ImbuementAltarRecipe.Serializer.STREAM_CODEC;
                codec.encode(buffer, recipe);
                var decoded = codec.decode(buffer);
                if (buffer.isReadable() || decoded.hasPoll() != recipe.hasPoll()
                        || !ItemStack.matches(recipe.getResultItem(helper.getLevel().registryAccess()), decoded.getResultItem(helper.getLevel().registryAccess()))) {
                    helper.fail("Altar recipe changed during synchronization: " + holder.id());
                }
                if (recipe.hasPoll()) {
                    ItemStack crystal = new ItemStack(EBItems.SPECTRAL_DUST_EARTH.get());
                    ItemStack output = decoded.getRandomResult(helper.getLevel().random, new ItemStack[]{crystal, crystal, crystal, crystal});
                    if (output.isEmpty() || ItemData.read(output).getString("LootTable").isEmpty()) {
                        helper.fail("Weighted ruined-book repair lost its custom loot-table output data");
                    }
                }
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "ebwizardry:empty")
    public static void magicEffectPacketRoundTrip(GameTestHelper helper) {
        var effect = new com.binaris.wizardry.api.content.effect.MagicMobEffectInstance(
                com.binaris.wizardry.setup.registries.EBMobEffects.WARD.get(), 100, 1);
        var packet = new net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket(42, effect, true);
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var codec = net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket.STREAM_CODEC;
            codec.encode(buffer, packet);
            var decoded = codec.decode(buffer);
            if (buffer.isReadable() || !((com.binaris.wizardry.api.network.MagicEffectPacket) decoded).ebwizardry$isMagic()
                    || decoded.getEffectDurationTicks() != 100 || decoded.getEffectAmplifier() != 1) {
                helper.fail("Magic effect packet lost its marker, duration, or strength");
            }
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}

