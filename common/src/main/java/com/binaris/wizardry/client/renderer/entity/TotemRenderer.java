package com.binaris.wizardry.client.renderer.entity;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.entity.construct.TotemConstruct;
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

import java.util.stream.IntStream;

public final class TotemRenderer extends EntityRenderer<TotemConstruct> {
    private static final ResourceLocation FLARE = WizardryMainMod.location("textures/entity/totem/flare.png");
    private static final ResourceLocation[] FRAMES = IntStream.range(0, 14)
            .mapToObj(frame -> WizardryMainMod.location("textures/entity/totem/cube_" + frame + ".png"))
            .toArray(ResourceLocation[]::new);

    public TotemRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(TotemConstruct totem, float yaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight) {
        float age = totem.tickCount + partialTick;
        float scale = Math.clamp(Math.min(age, totem.getLifetime() - age) / 10, 0, 1);
        poses.pushPose();
        poses.translate(0, 0.5, 0);
        poses.scale(scale, scale, scale);

        poses.pushPose();
        poses.mulPose(entityRenderDispatcher.cameraOrientation());
        int red = totem.withering() ? Math.min(255, 125 + (int) (totem.healthDrained() * 3)) : 255;
        int green = totem.withering() ? 45 : 244;
        int blue = totem.withering() ? Math.max(45, 175 - (int) totem.healthDrained()) : 155;
        quad(buffers.getBuffer(RenderType.entityTranslucentEmissive(FLARE)), poses.last(), 0.75f, 0, red, green, blue);
        poses.popPose();

        poses.mulPose(Axis.YP.rotationDegrees(age));
        poses.mulPose(com.mojang.math.Axis.of(new org.joml.Vector3f(0.7071f, 0, 0.7071f)).rotationDegrees(60));
        poses.mulPose(Axis.YP.rotationDegrees(2 * age));
        var vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(totem)));
        for (int face = 0; face < 6; face++) {
            poses.pushPose();
            if (face < 4) poses.mulPose(Axis.YP.rotationDegrees(face * 90));
            else poses.mulPose(Axis.XP.rotationDegrees(face == 4 ? 90 : -90));
            quad(vertices, poses.last(), 0.25f, 0.25f, 255, 255, 255);
            poses.popPose();
        }
        poses.popPose();
        super.render(totem, yaw, partialTick, poses, buffers, packedLight);
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, float size, float depth, int red, int green, int blue) {
        vertex(out, pose, -size, -size, depth, 0, 1, red, green, blue);
        vertex(out, pose, size, -size, depth, 1, 1, red, green, blue);
        vertex(out, pose, size, size, depth, 1, 0, red, green, blue);
        vertex(out, pose, -size, size, depth, 0, 0, red, green, blue);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int red, int green, int blue) {
        out.addVertex(pose, x, y, z).setColor(red, green, blue, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
    }

    @Override
    public ResourceLocation getTextureLocation(TotemConstruct totem) { return FRAMES[totem.tickCount % FRAMES.length]; }
}
