package com.binaris.wizardry.content.effect;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.effect.MagicMobEffect;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.MobEffectContext;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.binaris.wizardry.setup.registries.Spells;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import com.google.common.util.concurrent.AtomicDouble;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.concurrent.atomic.AtomicBoolean;

public final class IceShroudMobEffect extends MagicMobEffect {
    public IceShroudMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0);
        hideVanillaParticles();
    }

    @Override
    public void onUserHurt(LivingEntity user, DamageSource source, AtomicDouble amount, AtomicBoolean canceled, MobEffectContext context) {
        if (!user.level().isClientSide && !canceled.get() && !source.is(DamageTypeTags.IS_PROJECTILE)
                && source.getEntity() instanceof LivingEntity attacker && attacker != user && attacker.distanceToSqr(user) < 100) {
            attacker.addEffect(new MobEffectInstance(EBMobEffects.FROST.get(),
                    Spells.ICE_SHROUD.property(DefaultProperties.EFFECT_DURATION),
                    Spells.ICE_SHROUD.property(DefaultProperties.EFFECT_STRENGTH)));
        }
    }

    @Override
    public void spawnCustomParticle(Level level, double x, double y, double z) {
        float brightness = 0.5f + level.random.nextFloat() / 2;
        ParticleBuilder.create(EBParticles.SPARKLE).pos(x, y, z)
                .color(brightness, Math.min(1, brightness + 0.1f), 1).gravity(true).spawn(level);
        ParticleBuilder.create(EBParticles.SNOW).pos(x, y, z).spawn(level);
    }
}
