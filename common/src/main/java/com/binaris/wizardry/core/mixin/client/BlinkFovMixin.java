package com.binaris.wizardry.core.mixin.client;

import com.binaris.wizardry.client.effect.BlinkOverlay;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class BlinkFovMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void ebwizardry$blinkFov(Camera camera, float partialTick, boolean useConfiguredFov, CallbackInfoReturnable<Double> result) {
        if (!useConfiguredFov) return;
        float strength = Math.max(0, BlinkOverlay.strength(partialTick) - 0.25f);
        if (strength > 0) result.setReturnValue(result.getReturnValueD() * (1 + 0.7 * strength * strength));
    }
}
