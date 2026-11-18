package com.binaris.wizardry.client.renderer.entity;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.entity.living.SpiritHorse;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class SpiritHorseRenderer extends MobRenderer<SpiritHorse, HorseModel<SpiritHorse>> {
    private static final ResourceLocation TEXTURE = WizardryMainMod.location("textures/entity/spirit_horse.png");

    public SpiritHorseRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiritModel(context.bakeLayer(ModelLayers.HORSE)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritHorse wolf) { return TEXTURE; }

    @Override
    protected RenderType getRenderType(SpiritHorse wolf, boolean visible, boolean translucent, boolean glowing) {
        return visible ? RenderType.entityTranslucent(TEXTURE) : super.getRenderType(wolf, false, translucent, glowing);
    }

    private static final class SpiritModel extends HorseModel<SpiritHorse> {
        private float opacity = 1;

        SpiritModel(ModelPart root) { super(root); }

        @Override
        public void setupAnim(SpiritHorse wolf, float limb, float speed, float age, float yaw, float pitch) {
            super.setupAnim(wolf, limb, speed, age, yaw, pitch);
            opacity = wolf.opacity();
        }

        @Override
        public void renderToBuffer(PoseStack poses, VertexConsumer vertices, int light, int overlay, int color) {
            int alpha = (int) ((color >>> 24) * opacity);
            super.renderToBuffer(poses, vertices, light, overlay, (color & 0xFFFFFF) | (alpha << 24));
        }
    }
}
