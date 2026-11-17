package com.binaris.wizardry.content.effect;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.effect.MagicMobEffect;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class FrostMobEffect extends MagicMobEffect {
    public FrostMobEffect() {
        super(MobEffectCategory.HARMFUL, 0);
        hideVanillaParticles();
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, com.binaris.wizardry.WizardryMainMod.location("modifier/35dded48-2f19-4541-8510-b29e2dc2cd51"), -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int i) {
        if (livingEntity.isOnFire()) {
            if (livingEntity.hasEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this))) {
                livingEntity.removeEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this));
                livingEntity.igniteForSeconds(0);
            }
        }

        livingEntity.setIsInPowderSnow(true);

        // need to check the -1, being the infinite duration case
        if (livingEntity.hasEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this)) && livingEntity.getEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this)).getDuration() <= 1 && livingEntity.getEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this)).getDuration() != -1) {
            livingEntity.setIsInPowderSnow(false);
        }
        super.applyEffectTick(livingEntity, i);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int i, int j) {
        return true;
    }

    @Override
    public void spawnCustomParticle(Level world, double x, double y, double z) {
        ParticleBuilder.create(EBParticles.SNOW).pos(x, y, z).time(15 + world.random.nextInt(5)).spawn(world);
    }
}
