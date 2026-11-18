package com.binaris.wizardry.content.entity.living;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.setup.registries.EBEntities;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;

public final class SpectralGolem extends IronGolem {
    public SpectralGolem(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        setPlayerCreated(true);
        xpReward = 0;
    }

    public SpectralGolem(Level level) {
        this(EBEntities.SPECTRAL_GOLEM.get(), level);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            ParticleBuilder.create(EBParticles.DUST).pos(getRandomX(1), getRandomY() + 0.2, getRandomZ(1))
                    .color(0.7f, 0.9f, 1).spawn(level());
            if (tickCount == 1 || deathTime == 1) {
                for (int i = 0; i < 15; i++) {
                    level().addParticle(ParticleTypes.LARGE_SMOKE, getRandomX(1), getRandomY(), getRandomZ(1), 0, 0, 0);
                }
            }
        }
    }
}
