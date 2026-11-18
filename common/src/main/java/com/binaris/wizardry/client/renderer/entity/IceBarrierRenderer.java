package com.binaris.wizardry.client.renderer.entity;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.entity.construct.IceBarrierConstruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class IceBarrierRenderer extends EntityRenderer<IceBarrierConstruct> {
    private static final ResourceLocation TEXTURE = WizardryMainMod.location("textures/entity/ice_barrier.png");
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(WizardryMainMod.location("ice_barrier"), "main");
    private final ModelPart model;

    public IceBarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = context.bakeLayer(LAYER);
    }

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("barrier", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-14.5f, -14.5f, -1.5f, 29, 29, 3)
                .texOffs(0, 32).addBox(-9, -9, -4, 18, 18, 8),
                PartPose.offsetAndRotation(0, 24, 0, 0, 0, 0.7854f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(IceBarrierConstruct barrier, float yaw, float partialTick, PoseStack poses, MultiBufferSource buffers, int light) {
        poses.pushPose();
        double midpoint = barrier.getBbHeight() / 2;
        poses.translate(0, midpoint, 0);
        poses.mulPose(Axis.ZP.rotationDegrees(180));
        poses.mulPose(Axis.YP.rotationDegrees(yaw));
        poses.translate(0, -midpoint - 0.3, 0);
        float size = barrier.getSizeMultiplier();
        poses.scale(size, size, size);
        model.render(poses, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        poses.popPose();
        super.render(barrier, yaw, partialTick, poses, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(IceBarrierConstruct barrier) { return TEXTURE; }
}
