package com.binaris.wizardry.client.renderer.entity;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.entity.projectile.LightningDiscEntity;
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

public final class LightningDiscRenderer extends EntityRenderer<LightningDiscEntity> {
    private static final ResourceLocation TEXTURE = WizardryMainMod.location("textures/entity/lightning_sigil.png");

    public LightningDiscRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LightningDiscEntity disc, float yaw, float partialTick, PoseStack poses, MultiBufferSource buffers, int light) {
        poses.pushPose();
        poses.translate(0, 0.01, 0);
        poses.mulPose(Axis.YP.rotationDegrees((disc.tickCount + partialTick) * 8));
        var vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE, false));
        vertex(vertices, poses.last(), -1, -1, 0, 1);
        vertex(vertices, poses.last(), 1, -1, 1, 1);
        vertex(vertices, poses.last(), 1, 1, 1, 0);
        vertex(vertices, poses.last(), -1, 1, 0, 0);
        poses.popPose();
        super.render(disc, yaw, partialTick, poses, buffers, light);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float z, float u, float v) {
        vertices.addVertex(pose, x, 0, z).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(LightningDiscEntity disc) { return TEXTURE; }
}
