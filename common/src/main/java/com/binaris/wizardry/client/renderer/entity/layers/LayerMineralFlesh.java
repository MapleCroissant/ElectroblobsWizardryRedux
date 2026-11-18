package com.binaris.wizardry.client.renderer.entity.layers;

import com.binaris.wizardry.setup.registries.EBMobEffects;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class LayerMineralFlesh<T extends LivingEntity, M extends EntityModel<T>> extends LayerTiledOverlay<T, M> {
    private static final ResourceLocation IRON = ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");
    private static final ResourceLocation DIAMOND = ResourceLocation.withDefaultNamespace("textures/block/diamond_block.png");

    public LayerMineralFlesh(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public boolean shouldRender(T entity, float partialTick) {
        return !entity.isInvisible() && !LayerSummonAnimation.shouldHideModel(entity)
                && (entity.hasEffect(EBMobEffects.IRONFLESH.get()) || entity.hasEffect(EBMobEffects.DIAMONDFLESH.get()));
    }

    @Override
    public ResourceLocation getTexture(T entity, float partialTick) {
        return entity.hasEffect(EBMobEffects.DIAMONDFLESH.get()) ? DIAMOND : IRON;
    }
}
