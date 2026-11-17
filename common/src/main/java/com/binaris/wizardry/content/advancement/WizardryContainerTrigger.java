package com.binaris.wizardry.content.advancement;

import com.binaris.wizardry.WizardryMainMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public class WizardryContainerTrigger extends SimpleCriterionTrigger<WizardryContainerTrigger.TriggerInstance> {
    private final ResourceLocation id;
    public WizardryContainerTrigger(String name) { id = WizardryMainMod.location(name); }
    public ResourceLocation getId() { return id; }
    @Override public Codec<TriggerInstance> codec() { return TriggerInstance.CODEC; }
    public void trigger(ServerPlayer player, ItemStack stack) {
        trigger(player, instance -> instance.item().map(p -> p.test(stack)).orElse(true));
    }
    public record TriggerInstance(Optional<ContextAwarePredicate> player,
                                  Optional<ItemPredicate> item) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item)
        ).apply(instance, TriggerInstance::new));
        public TriggerInstance(ResourceLocation id, ItemPredicate item) {
            this(Optional.empty(), Optional.of(item));
        }
    }
}