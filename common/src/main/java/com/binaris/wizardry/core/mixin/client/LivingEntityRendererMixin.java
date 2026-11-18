package com.binaris.wizardry.core.mixin.client;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.binaris.wizardry.client.renderer.entity.layers.LayerSummonAnimation;
import com.binaris.wizardry.content.spell.fire.Disintegration;
import net.minecraft.resources.ResourceLocation;
import java.util.stream.IntStream;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/// Adds the summon appear/disappear overlay layer to every living entity renderer, and hides the normal model while a
/// minion is appearing or disappearing (only the overlay is shown during those ticks).
///
/// This is a common mixin loaded by both Fabric and Forge, so no per-loader registration is needed: minions are
/// arbitrary mobs, so the layer has to be present on every `LivingEntityRenderer` anyway.
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @WrapMethod(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void ebwizardry$translateMirage(LivingEntity entity, float yaw, float delta, PoseStack poses,
                                           MultiBufferSource buffers, int light, Operation<Void> original) {
        if (!entity.hasEffect(EBMobEffects.MIRAGE.get())) {
            original.call(entity, yaw, delta, poses, buffers, light);
            return;
        }
        var rng = new java.util.Random(entity.tickCount / 47 + entity.tickCount / 71);
        poses.pushPose();
        try {
            poses.translate(rng.nextDouble() * 6 - 3, rng.nextDouble() * 6 - 3, rng.nextDouble() * 6 - 3);
            original.call(entity, yaw, delta, poses, buffers, light);
        } finally {
            poses.popPose();
        }
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            index = 4)
    private int ebwizardry$tintMirage(int color, @Local(argsOnly = true) LivingEntity entity) {
        return entity.hasEffect(EBMobEffects.MIRAGE.get()) ? (color & 0x00ffffff) | 0x80000000 : color;
    }

    @org.spongepowered.asm.mixin.Unique
    private static final ResourceLocation[] EBWIZARDRY_DISINTEGRATION_FRAMES = IntStream.range(0, 19)
            .mapToObj(frame -> WizardryMainMod.location("textures/entity/disintegrate_overlay/disintegrate_overlay_" + frame + ".png"))
            .toArray(ResourceLocation[]::new);

    @Inject(method = "getFlipDegrees", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$disableDeathTilt(LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        if (((Disintegration.Animation) entity).ebwizardry$disintegrationFrame() >= 0) cir.setReturnValue(0f);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL"))
    private void ebwizardry$renderSixthSense(LivingEntity entity, float yaw, float delta,
            com.mojang.blaze3d.vertex.PoseStack poses, net.minecraft.client.renderer.MultiBufferSource buffers,
            int light, CallbackInfo ci) {
        com.binaris.wizardry.client.effect.SixthSenseMarkers.render(entity, poses, buffers);
    }

    @Shadow
    protected List<RenderLayer<T, M>> layers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void EBWIZARDRY$addSummonAnimationLayer(CallbackInfo ci) {
        this.layers.add(new com.binaris.wizardry.client.renderer.entity.layers.LayerMineralFlesh<T, M>(
                (RenderLayerParent<T, M>) (Object) this));
        this.layers.add(new com.binaris.wizardry.client.renderer.entity.layers.LayerMindControl<T, M>(
                (RenderLayerParent<T, M>) (Object) this));
        this.layers.add(new LayerSummonAnimation<T, M>((RenderLayerParent<T, M>) (Object) this));
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void EBWIZARDRY$hideModelDuringSummonAnimation(LivingEntity entity, boolean bodyVisible, boolean invisibleToPlayer, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        int frame = ((Disintegration.Animation) entity).ebwizardry$disintegrationFrame();
        if (frame >= 0) {
            cir.setReturnValue(RenderType.entityTranslucentEmissive(EBWIZARDRY_DISINTEGRATION_FRAMES[Math.min(frame, 18)]));
            return;
        }
        if (LayerSummonAnimation.shouldHideModel(entity)) {
            cir.setReturnValue(null);
        } else if (bodyVisible && entity.hasEffect(EBMobEffects.MIRAGE.get())) {
            @SuppressWarnings("unchecked")
            var renderer = (LivingEntityRenderer<T, M>) (Object) this;
            @SuppressWarnings("unchecked")
            var subject = (T) entity;
            cir.setReturnValue(RenderType.entityTranslucentEmissive(renderer.getTextureLocation(subject)));
        }
    }
}