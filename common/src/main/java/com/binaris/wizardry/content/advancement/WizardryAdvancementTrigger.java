package com.binaris.wizardry.content.advancement;

import com.binaris.wizardry.WizardryMainMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.Optional;

public class WizardryAdvancementTrigger extends SimpleCriterionTrigger<WizardryAdvancementTrigger.TriggerInstance> {
    private final ResourceLocation id;
    public WizardryAdvancementTrigger(String name) { id = WizardryMainMod.location(name); }
    public ResourceLocation getId() { return id; }
    @Override public Codec<TriggerInstance> codec() { return TriggerInstance.CODEC; }
    public void triggerFor(Player player) {
        if (player instanceof ServerPlayer serverPlayer) trigger(serverPlayer, instance -> true);
    }
    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
        ).apply(instance, TriggerInstance::new));
        public TriggerInstance(ResourceLocation id) { this(Optional.empty()); }
    }
}