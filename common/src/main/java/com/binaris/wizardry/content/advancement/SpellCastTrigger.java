package com.binaris.wizardry.content.advancement;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.spell.Spell;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public class SpellCastTrigger extends SimpleCriterionTrigger<SpellCastTrigger.TriggerInstance> {
    public ResourceLocation getId() { return WizardryMainMod.location("cast_spell"); }
    @Override public Codec<TriggerInstance> codec() { return TriggerInstance.CODEC; }
    public void trigger(ServerPlayer player, Spell spell, ItemStack stack) {
        trigger(player, instance -> instance.spell().test(spell) && instance.item().map(p -> p.test(stack)).orElse(true));
    }
    public record TriggerInstance(Optional<ContextAwarePredicate> player, SpellPredicate spell,
                                  Optional<ItemPredicate> item) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                SpellPredicate.CODEC.optionalFieldOf("spell", SpellPredicate.any()).forGetter(TriggerInstance::spell),
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item)
        ).apply(instance, TriggerInstance::new));
    }
}