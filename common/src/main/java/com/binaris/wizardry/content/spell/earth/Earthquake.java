package com.binaris.wizardry.content.spell.earth;

import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.content.entity.construct.EarthquakeConstruct;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.ConstructSpell;
import com.binaris.wizardry.core.networking.s2c.ScreenShakeS2C;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class Earthquake extends ConstructSpell<EarthquakeConstruct> {
    private static final SpellProperty<Float> SPREAD_SPEED = SpellProperty.floatProperty("spread_speed", 0.4f);

    public Earthquake() {
        super(level -> new EarthquakeConstruct(EBEntities.EARTHQUAKE.get(), level), true);
        floor(true);
        overlap(true);
        soundValues(2, 1, 0);
    }

    public static float speed() { return Math.clamp(Spells.EARTHQUAKE.property(SPREAD_SPEED), 0.05f, 4f); }

    @Override
    protected void addConstructExtras(CastContext context, EarthquakeConstruct quake, Direction side) {
        float range = context.modifiers().get(SpellModifiers.BLAST, property(DefaultProperties.EFFECT_RADIUS));
        quake.setLifetime(Math.clamp((int) (range / speed()), 1, 160));
    }

    @Override
    protected boolean spawnConstruct(CastContext context, Vec3 point, Direction side) {
        var world = context.world();
        var base = BlockPos.containing(point);
        if (!world.hasChunkAt(base) || !world.getWorldBorder().isWithinBounds(base)
                || !super.spawnConstruct(context, point, side)) return false;
        if (world instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, point.x, point.y + 0.1, point.z, 1, 0, 0, 0, 0);
            var state = level.getBlockState(base.below());
            if (!state.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    point.x, point.y + 0.1, point.z, 40, 0.6, 0.05, 0.6, 0.25);
            for (var player : level.players()) {
                if (player.distanceToSqr(point) <= 225) Services.NETWORK_HELPER.sendTo(player, new ScreenShakeS2C(12, 15));
            }
        }
        return true;
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.MASTER, Elements.EARTH, SpellTypes.ATTACK, SpellAction.POINT_DOWN, 75, 25, 250)
                .add(DefaultProperties.EFFECT_RADIUS, 8).add(SPREAD_SPEED).build();
    }
}
