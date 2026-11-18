package com.binaris.wizardry.core.mixin.client;

import com.binaris.wizardry.setup.registries.EBMobEffects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class ParalysisLookMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$preventParalysedLook(double yaw, double pitch, CallbackInfo callback) {
        if ((Object) this instanceof LocalPlayer player && player.hasEffect(EBMobEffects.PARALYSIS.get())) {
            callback.cancel();
        }
    }
}
