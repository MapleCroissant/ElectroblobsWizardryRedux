package com.binaris.wizardry.content.spell.earth;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.binaris.wizardry.setup.registries.EBSounds;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

public final class Glide extends Spell {
    private static final SpellProperty<Float> SPEED = SpellProperty.floatProperty("speed", 0.4f);
    private static final SpellProperty<Float> FALL_SPEED = SpellProperty.floatProperty("fall_speed", 0.1f);
    private static final SpellProperty<Float> ACCELERATION = SpellProperty.floatProperty("acceleration", 0.1f);

    @Override
    public boolean isInstantCast() {
        return false;
    }

    @Override
    public boolean cast(PlayerCastContext context) {
        var player = context.caster();
        var velocity = player.getDeltaMovement();
        if (velocity.y < -0.1 && !player.isInWater()) {
            float cap = context.modifiers().get(SpellModifiers.POTENCY, property(SPEED));
            var motion = new Vec3(velocity.x, -property(FALL_SPEED), velocity.z);
            if (Math.abs(velocity.x) < cap && Math.abs(velocity.z) < cap) {
                float acceleration = context.modifiers().get(SpellModifiers.POTENCY, property(ACCELERATION));
                var look = player.getLookAngle();
                motion = motion.add(look.x * acceleration, 0, look.z * acceleration);
            }
            player.setDeltaMovement(motion);
            if (!EBServerConfig.REPLACE_VANILLA_FALL_DAMAGE.get()) player.fallDistance = 0;
        }

        var level = context.world();
        if (level.isClientSide) {
            ParticleBuilder.create(EBParticles.SPARKLE).pos(particlePos(context)).velocity(0, -0.1, 0)
                    .time(15).color(1, 1, 1).spawn(level);
            ParticleBuilder.create(EBParticles.LEAF).pos(particlePos(context)).time(20).spawn(level);
            playSoundLoop(level, player, context.castingTicks());
        }
        return true;
    }

    private static Vec3 particlePos(PlayerCastContext context) {
        var random = context.world().random;
        return context.caster().position().add(random.nextDouble() * 0.5 - 0.25, random.nextDouble(),
                random.nextDouble() * 0.5 - 0.25);
    }

    @Override
    protected SoundEvent[] getLoopSounds() {
        var wind = EBSounds.SPELL_GLIDE.get();
        return new SoundEvent[]{wind, wind, SoundEvents.UI_TOAST_OUT};
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.EARTH, SpellTypes.UTILITY, SpellAction.POINT_DOWN, 5, 0, 0)
                .add(SPEED).add(FALL_SPEED).add(ACCELERATION).build();
    }
}
