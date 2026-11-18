package com.binaris.wizardry.client.renderer.entity;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.entity.construct.BlackHoleConstruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class BlackHoleRenderer extends EntityRenderer<BlackHoleConstruct> {
    private static final ResourceLocation RAY = WizardryMainMod.location("textures/entity/black_hole/ray.png");
    private static final ResourceLocation CENTER = WizardryMainMod.location("textures/entity/black_hole/centre.png");

    public BlackHoleRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(BlackHoleConstruct hole, float yaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight) {
        float age = hole.tickCount + partialTick;
        float scale = Math.clamp(Math.min(age, hole.getLifetime() - age) / 10, 0, 1);
        poses.pushPose();
        poses.scale(scale, scale, scale);
        var pose = poses.last();
        var vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(RAY));
        var rng = RandomSource.create(hole.getUUID().getLeastSignificantBits());
        for (int i = 0; i < 30; i++) {
            int tilt = rng.nextInt(10);
            int bank = rng.nextInt(10);
            double angle = age + 40 * i;
            var edge = rayEdge(angle, tilt, bank, hole.visualRadius());
            var other = rayEdge(angle - 20 - tilt, tilt, bank, hole.visualRadius());
            vertex(vertices, pose, Vec3.ZERO, 0, 0);
            vertex(vertices, pose, edge, 1, 0);
            vertex(vertices, pose, other, 1, 1);
            vertex(vertices, pose, Vec3.ZERO, 0, 1);
        }

        poses.mulPose(entityRenderDispatcher.cameraOrientation());
        vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(CENTER));
        vertex(vertices, poses.last(), new Vec3(-0.4, 0.4, 0), 0, 0);
        vertex(vertices, poses.last(), new Vec3(0.4, 0.4, 0), 1, 0);
        vertex(vertices, poses.last(), new Vec3(0.4, -0.4, 0), 1, 1);
        vertex(vertices, poses.last(), new Vec3(-0.4, -0.4, 0), 0, 1);
        poses.popPose();
        super.render(hole, yaw, partialTick, poses, buffers, packedLight);
    }

    private static Vec3 rayEdge(double angle, int tilt, int bank, double radius) {
        double x = radius * Math.sin(Math.toRadians(angle));
        double z = radius * Math.cos(Math.toRadians(angle));
        return new Vec3(x * Math.cos(31 * bank),
                z * Math.sin(31 * tilt) + x * Math.cos(31 * tilt) * Math.sin(31 * bank), z * Math.cos(31 * tilt));
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 point, float u, float v) {
        vertices.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleConstruct hole) { return RAY; }
}
