package com.binaris.wizardry.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Stack;

@Mixin(LivingEntity.class)
public abstract class WizardryDamageContainerMixin {
    @Shadow protected Stack<DamageContainer> damageContainers;

    @ModifyArg(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"), index = 1)
    private float ebwizardry$applyModifiedDamage(float damage) {
        // NeoForge reads its DamageContainer in actuallyHurt instead of this argument.
        damageContainers.peek().setNewDamage(damage);
        return damage;
    }
}
