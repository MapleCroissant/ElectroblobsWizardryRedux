package com.binaris.wizardry.content.spell.earth;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.data.SpellVar;
import com.binaris.wizardry.api.content.data.VarPersistence;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.*;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.RayTracer;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.AllyDesignation;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.*;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Grapple extends Spell {
    private static final SpellProperty<Float> EXTENSION = SpellProperty.floatProperty("extension_speed", 3.5f);
    private static final SpellProperty<Float> REEL = SpellProperty.floatProperty("reel_speed", 1);
    private static final double MINIMUM_DISTANCE = 3;
    private static final double STRETCH = 1.5;
    private static final SpellVar<Hook> HOOK = new SpellVar<Hook>(VarPersistence.NEVER)
            .withTicker((player, hook) -> EntityUtil.isCasting(player, Spells.GRAPPLE) ? hook : null);

    @Override
    public boolean canCastByEntity() { return true; }

    @Override
    public boolean canCastByLocation() { return true; }

    @Override
    public boolean isInstantCast() { return false; }

    @Override
    public boolean cast(PlayerCastContext context) {
        var caster = context.caster();
        var data = Services.OBJECT_DATA.getSpellManagerData(caster);
        var hook = data.getVariable(HOOK);
        Vec3 origin = caster.getEyePosition();
        if (hook == null || context.castingTicks() < hook.lastTick) {
            var hit = trace(context, origin, caster.getLookAngle());
            hook = new Hook(hit);
            data.setVariable(HOOK, hook);
            caster.swing(context.hand());
            sound(context, origin, EBSounds.SPELL_GRAPPLE_SHOOT.get());
        }
        hook.lastTick = context.castingTicks();
        HitResult hit = hook.hit;
        if (hit instanceof EntityHitResult entity && !isMovable(context, entity.getEntity())
                || hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                        && (!context.world().hasChunkAt(block.getBlockPos()) || !context.world().getBlockState(block.getBlockPos()).blocksMotion())) {
            return false;
        }

        var endpoint = endpoint(hit);
        double distance = origin.distanceTo(endpoint);
        if (distance > range(context) * STRETCH) {
            release(context, origin, endpoint);
            data.setVariable(HOOK, null);
            return false;
        }

        double reach = extension(context) * context.castingTicks();
        Vec3 direction = endpoint.subtract(origin).normalize();
        if (reach < distance) {
            vine(context, origin, origin.add(direction.scale(reach)), direction.scale(extension(context)), false, null);
            return true;
        }

        if (hit.getType() == HitResult.Type.MISS) {
            release(context, origin, endpoint);
            data.setVariable(HOOK, null);
            return false;
        }
        boolean lowering = caster.isShiftKeyDown() && ArtifactChannel.isEquipped(caster, EBItems.CHARM_ABSEILING.get());
        var desired = direction.scale(context.modifiers().get(SpellModifiers.POTENCY, property(REEL)));
        if (lowering) {
            float payout = (float) Math.max(-0.25, distance - range(context) * STRETCH + 1);
            desired = new Vec3(desired.x, payout, desired.z);
        }
        if (hit instanceof EntityHitResult entity) {
            if (distance > MINIMUM_DISTANCE) pull(context, entity.getEntity(), desired.reverse());
            vine(context, origin, endpoint, Vec3.ZERO, true, entity.getEntity());
        } else {
            pull(context, caster, desired);
            vine(context, origin, endpoint, Vec3.ZERO, true, null);
        }

        if (!hook.attached) {
            hook.attached = true;
            sound(context, endpoint, EBSounds.SPELL_GRAPPLE_ATTACH.get());
            sound(context, origin, EBSounds.SPELL_GRAPPLE_PULL.get());
        }
        return true;
    }

    @Override
    public boolean cast(EntityCastContext context) {
        var target = context.target();
        if (target == null) return false;
        Vec3 origin = context.caster().getEyePosition();
        var direction = target.getBoundingBox().getCenter().subtract(origin).normalize();
        return trace(context, origin, direction) instanceof EntityHitResult hit && hit.getEntity() == target
                && fixedPull(context, origin, hit.getEntity());
    }

    @Override
    public boolean cast(LocationCastContext context) {
        return trace(context, context.vec3(), Vec3.atLowerCornerOf(context.direction().getNormal())) instanceof EntityHitResult hit
                && fixedPull(context, context.vec3(), hit.getEntity());
    }

    private boolean fixedPull(CastContext context, Vec3 origin, Entity target) {
        if (!isMovable(context, target)) return false;
        Vec3 end = target.getBoundingBox().getCenter();
        Vec3 delta = end.subtract(origin);
        double distance = delta.length();
        Vec3 direction = delta.normalize();
        double speed = extension(context);
        double reach = speed * context.castingTicks();
        boolean attached = reach >= distance;
        if (context.castingTicks() == 0) sound(context, origin, EBSounds.SPELL_GRAPPLE_SHOOT.get());
        if (attached && reach - speed < distance) {
            sound(context, end, EBSounds.SPELL_GRAPPLE_ATTACH.get());
            sound(context, origin, EBSounds.SPELL_GRAPPLE_PULL.get());
        }
        if (attached && distance > MINIMUM_DISTANCE) {
            pull(context, target, direction.scale(-context.modifiers().get(SpellModifiers.POTENCY, property(REEL))));
        }
        vine(context, origin, attached ? end : origin.add(direction.scale(reach)), direction.scale(speed), attached,
                attached ? target : null);
        return true;
    }

    private boolean isMovable(CastContext context, Entity target) {
        return target.isAlive() && target.isPushable() && AllyDesignation.isValidTarget(context.caster(), target)
                && (!(target instanceof Player) || context.caster() instanceof Player);
    }

    private void pull(CastContext context, Entity target, Vec3 desired) {
        if (context.world().isClientSide) return;
        target.setDeltaMovement(target.getDeltaMovement().lerp(desired, 0.3));
        target.hasImpulse = true;
        target.hurtMarked = true;
        if (target == context.caster() && target.getDeltaMovement().y > 0) target.resetFallDistance();
    }

    private HitResult trace(CastContext context, Vec3 origin, Vec3 direction) {
        Vec3 end = origin.add(direction.scale(range(context)));
        var hit = RayTracer.rayTrace(context.world(), context.caster(), origin, end, 0, false, Entity.class,
                RayTracer.ignoreEntityFilter(context.caster()));
        if (hit instanceof EntityHitResult entity && !isMovable(context, entity.getEntity())
                || hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                        && !context.world().getBlockState(block.getBlockPos()).blocksMotion()) {
            return BlockHitResult.miss(hit.getLocation(), net.minecraft.core.Direction.DOWN, net.minecraft.core.BlockPos.containing(hit.getLocation()));
        }
        return hit;
    }

    private Vec3 endpoint(HitResult hit) {
        return hit instanceof EntityHitResult entity ? entity.getEntity().getBoundingBox().getCenter() : hit.getLocation();
    }

    private double extension(CastContext context) {
        return Math.clamp(context.modifiers().get(SpellModifiers.POTENCY, property(EXTENSION)), 0.05, 32);
    }

    private double range(CastContext context) {
        return Math.clamp(context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE)), 0, 64);
    }

    private void vine(CastContext context, Vec3 origin, Vec3 endpoint, Vec3 velocity, boolean attached, Entity target) {
        if (context.world().isClientSide) {
            var particle = ParticleBuilder.create(EBParticles.VINE).pos(origin.subtract(0, 0.3, 0)).target(endpoint)
                    .seed(context.world().getGameTime() - context.castingTicks());
            if (!attached) particle.tvel(velocity);
            if (target != null) particle.target(target);
            if (context.caster() != null) particle.entity(context.caster()).pos(origin.subtract(context.caster().position()).subtract(0, 0.3, 0));
            particle.spawn(context.world());
        }
    }

    @Override
    public void endCast(CastContext context) {
        if (context.caster() instanceof Player player) {
            var data = Services.OBJECT_DATA.getSpellManagerData(player);
            var hook = data.getVariable(HOOK);
            if (hook != null) {
                release(context, player.getEyePosition(), endpoint(hook.hit));
                data.setVariable(HOOK, null);
            }
        } else if (context.caster() != null) {
            var caster = context.caster();
            var target = context instanceof EntityCastContext entity ? entity.target() : null;
            release(context, caster.getEyePosition(), target == null ? caster.getEyePosition() : target.getBoundingBox().getCenter());
        } else if (context instanceof LocationCastContext location) {
            release(context, location.vec3(), endpoint(trace(context, location.vec3(), Vec3.atLowerCornerOf(location.direction().getNormal()))));
        }
    }

    private void release(CastContext context, Vec3 origin, Vec3 point) {
        sound(context, origin, EBSounds.SPELL_GRAPPLE_RELEASE.get());
        if (!context.world().isClientSide) return;
        Vec3 direction = point.subtract(origin).normalize();
        double reach = Math.min(origin.distanceTo(point), context.castingTicks() * extension(context));
        for (double distance = 1.5; distance <= reach; distance += 1.5) {
            ParticleBuilder.create(EBParticles.LEAF).pos(origin.add(direction.scale(distance))).time(25 + context.world().random.nextInt(5))
                    .spawn(context.world());
        }
    }

    private void sound(CastContext context, Vec3 at, SoundEvent sound) {
        if (!context.world().isClientSide) {
            context.world().playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, 1, 1);
        }
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.APPRENTICE, Elements.EARTH, SpellTypes.UTILITY, SpellAction.GRAPPLE, 5, 0, 0)
                .add(DefaultProperties.RANGE, 20f).add(EXTENSION).add(REEL).build();
    }

    private static final class Hook {
        private final HitResult hit;
        private int lastTick;
        private boolean attached;

        private Hook(HitResult hit) { this.hit = hit; }
    }
}
