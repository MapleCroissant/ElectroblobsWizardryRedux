package com.binaris.wizardry.client.renderer.entity.layers;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class LayerMindControl<T extends LivingEntity, M extends EntityModel<T>> extends LayerTiledOverlay<T, M> {
    private static final ResourceLocation TEXTURE = WizardryMainMod.location("textures/entity/mind_control_overlay.png");

    public LayerMindControl(RenderLayerParent<T, M> parent) { super(parent); }

    @Override
    public boolean shouldRender(T entity, float partialTick) {
        return !entity.isInvisible() && entity.hasEffect(EBMobEffects.MIND_CONTROL.get());
    }

    @Override
    public ResourceLocation getTexture(T entity, float partialTick) { return TEXTURE; }

    @Override
    protected float alpha(T entity, float partialTick) {
        var effect = entity.getEffect(EBMobEffects.MIND_CONTROL.get());
        return effect == null ? 0 : Math.clamp((effect.getDuration() - partialTick) / 20, 0, 1);
    }

    @Override
    protected float textureOffset(T entity, float partialTick) { return (entity.tickCount + partialTick) * 0.003f; }
}
