package com.binaris.wizardry.content.effect;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.effect.MagicMobEffect;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.level.Level;

public final class TransienceMobEffect extends MagicMobEffect {
    public TransienceMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0);
        hideVanillaParticles();
    }

    @Override
    public void spawnCustomParticle(Level level, double x, double y, double z) {
        ParticleBuilder.create(EBParticles.DUST).pos(x, y, z).color(0.8f, 0.8f, 1).shaded(true).spawn(level);
    }
}
