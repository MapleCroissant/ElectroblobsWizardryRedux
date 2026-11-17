package com.binaris.wizardry.content.advancement;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.event.DiscoverSpellEvent;
import com.binaris.wizardry.api.content.spell.Spell;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;
import java.util.Locale;

public class SpellDiscoveryTrigger extends SimpleCriterionTrigger<SpellDiscoveryTrigger.TriggerInstance> {
    public ResourceLocation getId() { return WizardryMainMod.location("discover_spell"); }
    @Override public Codec<TriggerInstance> codec() { return TriggerInstance.CODEC; }
    public void trigger(ServerPlayer player, Spell spell, DiscoverSpellEvent.Source source) {
        trigger(player, instance -> instance.spell().test(spell) && instance.source() == source);
    }
    private static final Codec<DiscoverSpellEvent.Source> SOURCE_CODEC = Codec.STRING.comapFlatMap(name -> {
        DiscoverSpellEvent.Source source = DiscoverSpellEvent.Sources.byName(name);
        return source == null ? DataResult.error(() -> "Unknown spell discovery source: " + name) : DataResult.success(source);
    }, source -> source.getName().toLowerCase(Locale.ROOT));
    public record TriggerInstance(Optional<ContextAwarePredicate> player, SpellPredicate spell,
                                  DiscoverSpellEvent.Source source) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                SpellPredicate.CODEC.optionalFieldOf("spell", SpellPredicate.any()).forGetter(TriggerInstance::spell),
                SOURCE_CODEC.fieldOf("source").forGetter(TriggerInstance::source)
        ).apply(instance, TriggerInstance::new));
        public static TriggerInstance discoverSpell(DiscoverSpellEvent.Source source) {
            return new TriggerInstance(Optional.empty(), SpellPredicate.any(), source);
        }
    }
}