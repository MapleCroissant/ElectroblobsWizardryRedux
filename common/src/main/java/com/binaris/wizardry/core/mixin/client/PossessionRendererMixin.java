package com.binaris.wizardry.core.mixin.client;

import com.binaris.wizardry.content.spell.necromancy.Possession;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class PossessionRendererMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void ebwizardry$renderHostInstead(LivingEntity entity, float yaw, float partialTick, PoseStack pose,
                                              MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (entity instanceof Player player && Possession.active(player)) ci.cancel();
    }
}
