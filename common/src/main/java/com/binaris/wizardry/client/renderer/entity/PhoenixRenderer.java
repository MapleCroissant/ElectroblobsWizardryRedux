package com.binaris.wizardry.client.renderer.entity;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.entity.living.Phoenix;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class PhoenixRenderer extends MobRenderer<Phoenix, PhoenixRenderer.Model> {
    private static final ResourceLocation TEXTURE = WizardryMainMod.location("textures/entity/phoenix.png");

    public PhoenixRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(), 1);
    }

    @Override
    public ResourceLocation getTextureLocation(Phoenix phoenix) { return TEXTURE; }

    @Override
    public void render(Phoenix phoenix, float yaw, float delta, PoseStack poses, MultiBufferSource buffers, int light) {
        poses.pushPose();
        poses.translate(0, -0.1, 0);
        super.render(phoenix, yaw, delta, poses, buffers, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
        poses.popPose();
    }

    public static final class Model extends HierarchicalModel<Phoenix> {
        private final ModelPart root;

        private Model() {
            super(RenderType::entityTranslucentEmissive);
            var mesh = new MeshDefinition();
            var body = mesh.getRoot().addOrReplaceChild("body",
                    CubeListBuilder.create().texOffs(0, 34).mirror().addBox(0, 0, -3, 6, 15, 6), PartPose.offset(-3, 0, -5));
            body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 0).mirror()
                    .addBox(-27, -27, 0, 27, 34, 0), PartPose.offsetAndRotation(0, 5, 0, 0.1745329f, 0, 0));
            body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 0).addBox(0, -27, 0, 27, 34, 0),
                    PartPose.offsetAndRotation(6, 5, 0, 0.1745329f, 0, 0));
            var tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 55).mirror().addBox(-1, 0, -1, 2, 7, 2),
                    PartPose.offset(3, 15, 2));
            tail.addOrReplaceChild("feathers", CubeListBuilder.create().texOffs(0, 57).mirror().addBox(-5, 0, 0, 10, 7, 0),
                    PartPose.offset(0, 7, 1));
            var neck = mesh.getRoot().addOrReplaceChild("neck", CubeListBuilder.create().texOffs(24, 44).mirror().addBox(-1, -4, -1, 2, 4, 2),
                    PartPose.offset(0, 0, -4));
            var head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 34).mirror().addBox(-2, -4, -5, 4, 4, 6),
                    PartPose.offset(0, -4, 0));
            head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(32, 44).mirror().addBox(-0.5f, 4, -1, 1, 2, 3),
                    PartPose.offsetAndRotation(0, -5, -8, 0.2792527f, 0, 0));
            head.addOrReplaceChild("plume", CubeListBuilder.create().texOffs(28, 50).mirror().addBox(-0.03333334f, 1, 0, 0, 5, 5),
                    PartPose.offset(0, -7, 0));
            root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        }

        @Override
        public ModelPart root() { return root; }

        @Override
        public void setupAnim(Phoenix phoenix, float walk, float amount, float age, float headYaw, float headPitch) {
            var neck = root.getChild("neck");
            neck.xRot = headPitch * Mth.DEG_TO_RAD;
            neck.yRot = headYaw * Mth.DEG_TO_RAD;
            var body = root.getChild("body");
            body.xRot = 0.3f + Mth.cos(age * 0.1f) * 0.15f;
            var tail = body.getChild("tail");
            tail.xRot = body.xRot * 1.1f;
            tail.getChild("feathers").xRot = body.xRot * 1.2f;
            var right = body.getChild("right_wing");
            right.yRot = Mth.cos(age * 0.3f) * Mth.PI * 0.15f;
            body.getChild("left_wing").yRot = -right.yRot;
        }
    }
}
