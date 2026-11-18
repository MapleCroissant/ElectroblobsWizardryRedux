package com.binaris.wizardry.client.renderer.entity;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.entity.construct.LightningHammerConstruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class LightningHammerRenderer extends EntityRenderer<LightningHammerConstruct> {
    private static final ResourceLocation TEXTURE = WizardryMainMod.location("textures/entity/lightning_hammer.png");
    private final ModelPart model;

    public LightningHammerRenderer(EntityRendererProvider.Context context) {
        super(context);
        var mesh = new MeshDefinition();
        var cubes = CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-10, 12, -6, 20, 12, 12)
                .texOffs(0, 24).addBox(-2, -2, -2, 4, 14, 4)
                .texOffs(0, 49).addBox(-2.5f, -7, -2.5f, 5, 5, 5)
                .texOffs(0, 42).addBox(-2.5f, 10, -2.5f, 5, 2, 5)
                .texOffs(20, 24).addBox(-8, 11, -7, 2, 14, 14)
                .texOffs(20, 24).addBox(6, 11, -7, 2, 14, 14);
        mesh.getRoot().addOrReplaceChild("hammer", cubes, PartPose.ZERO);
        model = LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    @Override
    public void render(LightningHammerConstruct hammer, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, 1.5, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        if (hammer.spinning()) pose.mulPose(Axis.ZP.rotationDegrees(180 + 15 * (hammer.tickCount + partialTick)));
        model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(hammer, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(LightningHammerConstruct hammer) { return TEXTURE; }
}
