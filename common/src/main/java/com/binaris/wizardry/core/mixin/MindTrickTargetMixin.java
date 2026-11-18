package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.setup.registries.EBMobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MindTrickTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$preventTargetAcquisition(LivingEntity target, CallbackInfo callback) {
        if (target == null) return;
        Mob mob = (Mob) (Object) this;
        if (mob.hasEffect(EBMobEffects.MIND_TRICK.get()) || mob.hasEffect(EBMobEffects.FEAR.get())) {
            callback.cancel();
        } else if (target.hasEffect(EBMobEffects.MIRAGE.get())) {
            mob.setTarget(null);
            callback.cancel();
        } else if (target.hasEffect(EBMobEffects.MUFFLE.get())) {
            var direction = target.getEyePosition().subtract(mob.getEyePosition()).normalize();
            if (mob.getLookAngle().dot(direction) < Math.cos(Math.PI * 0.4)) callback.cancel();
        }
    }
}
