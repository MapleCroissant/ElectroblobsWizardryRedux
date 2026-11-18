package com.binaris.wizardry.content.spell.fire;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.entity.projectile.EmberEntity;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.RaySpell;
import com.binaris.wizardry.setup.registries.*;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Disintegration extends RaySpell {
    public static final SpellProperty<Integer> BURN_DURATION = SpellProperty.intProperty("burn_duration", 10);
    public static final SpellProperty<Integer> EMBER_LIFETIME = SpellProperty.intProperty("ember_lifetime", 300);
    private static final SpellProperty<Integer> EMBER_COUNT = SpellProperty.intProperty("ember_count", 12);
    public static final byte ANIMATION_EVENT = 101;

    public interface Animation {
        int ebwizardry$disintegrationFrame();
        boolean ebwizardry$startDisintegration();
    }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) {
        if (context.world().isClientSide) return true;
        var target = hit.getEntity();
        var caster = context.caster();
        if (target.fireImmune()) {
            if (caster instanceof Player player) {
                player.displayClientMessage(Component.translatable("spell.resist", target.getName(), getDescriptionFormatted()), true);
            }
            return true;
        }
        target.igniteForSeconds(context.modifiers().get(SpellModifiers.DURATION, property(BURN_DURATION)));
        EntityUtil.attackEntityWithoutKnockback(target,
                MagicDamageSource.causeDirectMagicDamage(context.world(), caster, EBDamageSources.FIRE),
                context.modifiers().get(SpellModifiers.POTENCY, property(DefaultProperties.DAMAGE)));
        if (target instanceof LivingEntity living && living.isDeadOrDying()) spawnEmbers(living, caster);
        return true;
    }

    public static void spawnEmbers(LivingEntity victim, @Nullable LivingEntity caster) {
        var level = victim.level();
        if (level.isClientSide || !((Animation) victim).ebwizardry$startDisintegration()) return;
        victim.clearFire();
        level.broadcastEntityEvent(victim, ANIMATION_EVENT);
        var rng = level.random;
        for (int i = 0; i < Math.clamp(Spells.DISINTEGRATION.property(EMBER_COUNT), 0, 64); i++) {
            var ember = new EmberEntity(EBEntities.EMBER.get(), level);
            double x = (rng.nextDouble() - 0.5) * victim.getBbWidth();
            double y = rng.nextDouble() * victim.getBbHeight();
            double z = (rng.nextDouble() - 0.5) * victim.getBbWidth();
            ember.setPos(victim.position().add(x, y, z));
            ember.setOwner(caster);
            ember.tickCount = rng.nextInt(20);
            ember.setDeltaMovement(x * 0.2, y * 0.1, z * 0.2);
            level.addFreshEntity(ember);
        }
    }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) {
        var level = context.world();
        if (level.isClientSide) {
            var point = hit.getLocation();
            for (int i = 0; i < 8; i++) level.addParticle(ParticleTypes.LAVA, point.x, point.y, point.z, 0, 0, 0);
            if (level.getBlockState(hit.getBlockPos()).isSolid()) {
                ParticleBuilder.create(EBParticles.SCORCH).pos(point.relative(hit.getDirection(), 0.01))
                        .face(hit.getDirection()).color(1, 0.2f, 0).spawn(level);
            }
        }
        return false;
    }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) { return true; }

    @Override
    protected void spawnParticleRay(CastContext context, Vec3 origin, Vec3 direction, double distance) {
        ParticleBuilder.create(EBParticles.BEAM).pos(origin).target(origin.add(direction.scale(distance)))
                .color(1, 0.4f, 0).fade(1, 0.1f, 0).time(4).spawn(context.world());
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.FIRE, SpellTypes.ATTACK, SpellAction.POINT, 35, 0, 40)
                .add(DefaultProperties.RANGE, 10f).add(DefaultProperties.DAMAGE, 8f)
                .add(BURN_DURATION).add(EMBER_LIFETIME).add(EMBER_COUNT).build();
    }
}
