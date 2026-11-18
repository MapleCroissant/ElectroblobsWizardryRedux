package com.binaris.wizardry.content.spell.lightning;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.AllyDesignation;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LightningPulse extends Spell {
    private static final double ANTI_Z_FIGHTING_OFFSET = 0.005;
    private static final SpellProperty<Float> REPULSION_VELOCITY = SpellProperty.floatProperty("repulsion_velocity", 0.8f);

    public LightningPulse() {
        soundValues(2, 1, 0);
    }

    @Override
    public boolean cast(PlayerCastContext ctx) {
        if (!ctx.caster().onGround()) return false;
        float radius = ctx.modifiers().get(SpellModifiers.BLAST, property(DefaultProperties.EFFECT_RADIUS));

        List<LivingEntity> targets = EntityUtil.getLivingWithinRadius(radius, ctx.caster().getX(), ctx.caster().getY(), ctx.caster().getZ(), ctx.world());
        targets.removeIf(t -> !AllyDesignation.isValidTarget(ctx.caster(), t));
        for (LivingEntity target : targets) {
            if (ctx.world().isClientSide()) continue;
            target.hurt(
                    MagicDamageSource.causeDirectMagicDamage(ctx.caster(), EBDamageSources.SHOCK),
                    ctx.modifiers().get(SpellModifiers.POTENCY, property(DefaultProperties.DAMAGE))
            );

            var outward = target.position().subtract(ctx.caster().position()).multiply(1, 0, 1)
                    .normalize().scale(property(REPULSION_VELOCITY));
            target.setDeltaMovement(outward);
            target.hurtMarked = true;
            if (target instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
            }
        }

        if (ctx.world().isClientSide()) {
            ParticleBuilder.create(EBParticles.LIGHTNING_PULSE)
                    .pos(ctx.caster().getX(), ctx.caster().getY() + ANTI_Z_FIGHTING_OFFSET, ctx.caster().getZ())
                    .scale(ctx.modifiers().getFactor(SpellModifiers.BLAST))
                    .spawn(ctx.world());
        }

        if (!ctx.world().isClientSide()) {
            ctx.world().playSound(null, ctx.caster(), com.binaris.wizardry.setup.registries.EBSounds.SPELL_LIGHTNING_PULSE_SPARK.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 2, 1);
            ctx.world().playSound(null, ctx.caster(), com.binaris.wizardry.setup.registries.EBSounds.SPELL_LIGHTNING_PULSE_EXPLOSION.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 2, 1);
        }
        return true;
    }

    @Override
    protected @NotNull SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.LIGHTNING, SpellTypes.ATTACK, SpellAction.POINT_DOWN, 25, 0, 75)
                .add(DefaultProperties.DAMAGE, 8.0F)
                .add(DefaultProperties.EFFECT_RADIUS, 3)
                .add(REPULSION_VELOCITY)
                .build();
    }
}
