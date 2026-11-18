package com.binaris.wizardry.client.renderer.entity;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.entity.living.SpectralGolem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;

public final class SpectralGolemRenderer extends MobRenderer<SpectralGolem, IronGolemModel<SpectralGolem>> {
    private static final ResourceLocation TEXTURE = WizardryMainMod.location("textures/entity/spectral_golem.png");

    public SpectralGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new IronGolemModel<>(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SpectralGolem golem) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(SpectralGolem golem, boolean visible, boolean translucent, boolean glowing) {
        return visible ? RenderType.entityTranslucent(TEXTURE) : super.getRenderType(golem, false, translucent, glowing);
    }

    @Override
    protected void setupRotations(SpectralGolem golem, PoseStack poses, float age, float yaw, float partialTick, float scale) {
        super.setupRotations(golem, poses, age, yaw, partialTick, scale);
        if (golem.walkAnimation.speed() >= 0.01f) {
            poses.mulPose(Axis.ZP.rotationDegrees(6.5f * Mth.triangleWave(golem.walkAnimation.position(partialTick) + 6, 13)));
        }
    }
}
