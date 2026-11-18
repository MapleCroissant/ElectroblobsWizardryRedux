package com.binaris.wizardry.client.renderer.entity;

import com.binaris.wizardry.api.content.entity.projectile.MagicProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class MagicProjectileRenderer<T extends MagicProjectileEntity> extends EntityRenderer<T> {
    private final ResourceLocation texture;
    private final RenderType renderType;
    private final float scale;

    public MagicProjectileRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        this(context, texture, false);
    }

    public MagicProjectileRenderer(EntityRendererProvider.Context context, ResourceLocation texture, boolean blend) {
        this(context, texture, 0.7f, blend);
    }

    public MagicProjectileRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale, boolean blend) {
        super(context);
        this.texture = texture;
        this.scale = scale;
        this.renderType = blend ? RenderType.entityTranslucentEmissive(texture, false)
                : RenderType.entityCutoutNoCull(texture, false);
    }

    @Override
    public void render(@NotNull T entity, float yaw, float partialTick, PoseStack poseStack,
                       @NotNull MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());

        VertexConsumer vertices = buffers.getBuffer(renderType);
        PoseStack.Pose pose = poseStack.last();
        vertex(vertices, pose, -0.5f, -0.25f, 0, 1);
        vertex(vertices, pose, 0.5f, -0.25f, 1, 1);
        vertex(vertices, pose, 0.5f, 0.75f, 1, 0);
        vertex(vertices, pose, -0.5f, 0.75f, 0, 0);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float u, float v) {
        vertices.addVertex(pose, x, y, 0)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0, 0, 1);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) {
        return texture;
    }
}
