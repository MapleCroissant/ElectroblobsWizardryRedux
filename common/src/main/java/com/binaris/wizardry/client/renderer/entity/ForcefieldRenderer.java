package com.binaris.wizardry.client.renderer.entity;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.entity.construct.ForcefieldConstruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class ForcefieldRenderer extends EntityRenderer<ForcefieldConstruct> {
    public ForcefieldRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(ForcefieldConstruct field, float yaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light) {
        float age = field.tickCount + partialTick;
        double radius = field.radius() * Math.min(1, age / 3);
        int green = (int) (140 + 13 * Math.sin(age / 10));
        var pose = poses.last();
        var vertices = buffers.getBuffer(RenderType.debugQuads());
        sphere(vertices, pose, Math.max(0, radius - 0.12), 160, 210, 255, 70);
        sphere(vertices, pose, radius, 89, green, 255, 86);
        super.render(field, yaw, partialTick, poses, buffers, light);
    }

    private static void sphere(VertexConsumer vertices, PoseStack.Pose pose, double radius, int red, int green, int blue, int alpha) {
        for (int row = 0; row < 20; row++) {
            double low = (row - 10) * Math.PI / 20;
            double high = low + Math.PI / 20;
            for (int column = 0; column < 40; column++) {
                double left = column * Math.PI / 20;
                double right = left + Math.PI / 20;
                vertex(vertices, pose, radius, low, left, red, green, blue, alpha);
                vertex(vertices, pose, radius, low, right, red, green, blue, alpha);
                vertex(vertices, pose, radius, high, right, red, green, blue, alpha);
                vertex(vertices, pose, radius, high, left, red, green, blue, alpha);
            }
        }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, double radius, double lat, double lon,
                               int red, int green, int blue, int alpha) {
        double horizontal = radius * Math.cos(lat);
        out.addVertex(pose, (float) (horizontal * Math.sin(lon)), (float) (radius * Math.sin(lat)),
                        (float) (horizontal * Math.cos(lon))).setColor(red, green, blue, alpha);
    }

    @Override
    public ResourceLocation getTextureLocation(ForcefieldConstruct entity) { return WizardryMainMod.location("textures/spells/forcefield.png"); }
}
