package com.binaris.wizardry.core.mixin.client;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.client.effect.BlinkOverlay;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class SpellVisionRendererMixin {
    @Unique
    private static final ResourceLocation EBWIZARDRY_TRANSIENCE = WizardryMainMod.location("shaders/post/transience.json");

    @Unique
    private static final ResourceLocation EBWIZARDRY_SIXTH_SENSE = WizardryMainMod.location("shaders/post/sixth_sense.json");

    @Unique
    private static final ResourceLocation EBWIZARDRY_SLOW_TIME = WizardryMainMod.location("shaders/post/slow_time.json");

    @Shadow PostChain postEffect;
    @Shadow public abstract void loadEffect(ResourceLocation id);
    @Shadow public abstract void shutdownEffect();

    @Unique private ResourceLocation ebwizardry$lastVision;

    @Inject(method = "tick", at = @At("TAIL"))
    private void ebwizardry$updateSpellVision(CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        ResourceLocation desired = null;
        if (player != null) {
            if (player.hasEffect(EBMobEffects.TRANSIENCE.get())) desired = EBWIZARDRY_TRANSIENCE;
            else if (player.hasEffect(EBMobEffects.SIXTH_SENSE.get())) desired = EBWIZARDRY_SIXTH_SENSE;
            else if (player.hasEffect(EBMobEffects.SLOW_TIME.get())) desired = EBWIZARDRY_SLOW_TIME;
        }
        if (desired == ebwizardry$lastVision) return;
        if (postEffect != null && ebwizardry$lastVision != null
                && ebwizardry$lastVision.toString().equals(postEffect.getName())) shutdownEffect();
        if (desired != null) {
            BlinkOverlay.start(player);
            if (postEffect == null) loadEffect(desired);
        }
        ebwizardry$lastVision = desired;
    }
}