package com.binaris.wizardry.client.particle;

import com.binaris.wizardry.api.client.particle.ParticleWizardry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

public final class ParticleBlockHighlight extends UnoccludedSpellParticle {
    private ParticleBlockHighlight(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites, false);
        quadSize = 0.505f;
        setLifetime(160);
    }

    @Override
    public void tick() {
        super.tick();
        var direction = pitch == 90 ? Direction.UP : pitch == -90 ? Direction.DOWN : Direction.fromYRot(yaw);
        var block = BlockPos.containing(x, y, z).relative(direction.getOpposite());
        if (level.isEmptyBlock(block)) {
            remove();
            return;
        }
        float fraction = Math.min(1, age * 2f / lifetime);
        setColor(0.9f - 0.2f * fraction, 0.5f * (1 - fraction), 0.8f + 0.2f * fraction);
        setAlpha(Math.min(1, 2f * (1 - (float) age / lifetime)));
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private static SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            Provider.sprites = sprites;
        }

        public static ParticleWizardry create(ClientLevel level, Vec3 pos) {
            return new ParticleBlockHighlight(level, pos.x, pos.y, pos.z, sprites);
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new ParticleBlockHighlight(level, x, y, z, sprites);
        }
    }
}
