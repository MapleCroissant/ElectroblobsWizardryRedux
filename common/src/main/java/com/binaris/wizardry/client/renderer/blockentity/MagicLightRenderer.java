package com.binaris.wizardry.client.renderer.blockentity;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.client.util.ClientUtils;
import com.binaris.wizardry.content.blockentity.MagicLightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class MagicLightRenderer implements BlockEntityRenderer<MagicLightBlockEntity> {
    private static final RenderType FLARE = RenderType.entityTranslucentEmissive(WizardryMainMod.location("textures/entity/light/flare.png"), false);
    private static final RenderType RAY = RenderType.entityTranslucentEmissive(WizardryMainMod.location("textures/entity/light/ray.png"), false);
    private final RandomSource random = RandomSource.create();

    public MagicLightRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MagicLightBlockEntity light, float partialTick, PoseStack poses, MultiBufferSource buffers, int packedLight, int overlay) {
        float size = ClientUtils.smoothScaleFactor(light.getLifetime(), light.timer, partialTick, 10, 10);
        float age = (light.timer % 360) + partialTick;
        poses.pushPose();
        poses.translate(0.5, 0.5, 0.5);
        poses.scale(size, size, size);
        poses.pushPose();
        poses.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        var flare = buffers.getBuffer(FLARE);
        quadVertex(flare, poses.last(), -0.6f, 0.6f, 0, 0, 0, 255);
        quadVertex(flare, poses.last(), 0.6f, 0.6f, 0, 1, 0, 255);
        quadVertex(flare, poses.last(), 0.6f, -0.6f, 0, 1, 1, 255);
        quadVertex(flare, poses.last(), -0.6f, -0.6f, 0, 0, 1, 255);
        poses.popPose();

        random.setSeed(light.getBlockPos().asLong());
        var rays = buffers.getBuffer(RAY);
        for (int i = 0; i < 30; i++) {
            poses.pushPose();
            int tilt = random.nextInt(31);
            poses.mulPose(Axis.XP.rotationDegrees(31 * tilt));
            poses.mulPose(Axis.ZP.rotationDegrees(31 * random.nextInt(31)));
            float first = (age + 40 * i) * Mth.DEG_TO_RAD;
            float second = first - (20 + tilt) * Mth.DEG_TO_RAD;
            quadVertex(rays, poses.last(), 0, 0, 0, 0, 0, 235);
            quadVertex(rays, poses.last(), 0.5f * Mth.sin(first), 0, 0.5f * Mth.cos(first), 1, 0, 130);
            quadVertex(rays, poses.last(), 0.5f * Mth.sin(second), 0, 0.5f * Mth.cos(second), 1, 1, 130);
            quadVertex(rays, poses.last(), 0, 0, 0, 0, 1, 235);
            poses.popPose();
        }
        poses.popPose();
    }

    private static void quadVertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z, float u, float v, int alpha) {
        vertices.addVertex(pose, x, y, z).setColor(255, 255, 255, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }
}
