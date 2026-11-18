package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.setup.registries.EBMobEffects;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobParalysisMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$suspendParalysedAI(CallbackInfo callback) {
        Mob mob = (Mob) (Object) this;
        if (mob.hasEffect(EBMobEffects.PARALYSIS.get())) {
            mob.getNavigation().stop();
            mob.setXxa(0);
            mob.setYya(0);
            mob.setSpeed(0);
            mob.setJumping(false);
            callback.cancel();
        }
    }
}
