package com.binaris.wizardry.client.renderer.entity;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.entity.ShieldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class ShieldRenderer extends EntityRenderer<ShieldEntity> {
    private static final ResourceLocation TEXTURE = WizardryMainMod.location("textures/entity/shield.png");
    private static final float[][] OUTLINE = {
            {-0.6f, 0.4f}, {-0.3f, 0.7f}, {0.3f, 0.7f}, {0.6f, 0.4f},
            {0.6f, -0.4f}, {0.3f, -0.7f}, {-0.3f, -0.7f}, {-0.6f, -0.4f}
    };

    public ShieldRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(ShieldEntity shield, float yaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight) {
        poses.pushPose();
        poses.translate(0, 1.1, 0);
        poses.mulPose(Axis.YP.rotationDegrees(-yaw));
        poses.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, shield.xRotO, shield.getXRot())));
        poses.translate(0, 0, 0.7);
        var pose = poses.last();
        var vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        float pulse = 1 - ((shield.tickCount + partialTick) % 5) / 5 * 0.52f;
        for (int i = 0; i < OUTLINE.length; i++) {
            var left = OUTLINE[i];
            var right = OUTLINE[(i + 1) % OUTLINE.length];
            vertex(vertices, pose, left[0], left[1], -0.2f, 85, pulse);
            vertex(vertices, pose, right[0], right[1], -0.2f, 85, pulse);
            vertex(vertices, pose, Math.clamp(right[0], -0.3f, 0.3f), Math.clamp(right[1], -0.4f, 0.4f), 0, 200, pulse);
            vertex(vertices, pose, Math.clamp(left[0], -0.3f, 0.3f), Math.clamp(left[1], -0.4f, 0.4f), 0, 200, pulse);
        }
        vertex(vertices, pose, -0.3f, 0.4f, 0, 200, pulse);
        vertex(vertices, pose, 0.3f, 0.4f, 0, 200, pulse);
        vertex(vertices, pose, 0.3f, -0.4f, 0, 200, pulse);
        vertex(vertices, pose, -0.3f, -0.4f, 0, 200, pulse);
        poses.popPose();
        super.render(shield, yaw, partialTick, poses, buffers, packedLight);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z, int alpha, float pulse) {
        vertices.addVertex(pose, x, y, z).setColor(200, 200, 255, alpha)
                .setUv(0.5f + x / 1.2f * pulse * pulse, 0.5f - y / 1.4f * pulse * pulse)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
    }

    @Override
    public ResourceLocation getTextureLocation(ShieldEntity shield) { return TEXTURE; }
}
