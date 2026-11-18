package com.binaris.wizardry.content.effect;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.effect.MagicMobEffect;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.core.networking.s2c.SlowTimeS2C;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.binaris.wizardry.setup.registries.Spells;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class SlowTimeMobEffect extends MagicMobEffect {
    private static final Map<LivingEntity, Boolean> HOSTS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Level, Set<Integer>> BLOCKED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Level, Boolean> LOCAL_BLOCKED = Collections.synchronizedMap(new WeakHashMap<>());

    public SlowTimeMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x5be3bb);
        hideVanillaParticles();
    }

    @Override
    public void spawnCustomParticle(Level level, double x, double y, double z) {}

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        observe(entity);
        return true;
    }

    public static void observe(LivingEntity entity) { HOSTS.put(entity, Boolean.TRUE); }

    public static void prepare(Level level) {
        LivingEntity[] sources;
        synchronized (HOSTS) {
            sources = HOSTS.keySet().toArray(LivingEntity[]::new);
        }
        var schedules = new HashMap<Integer, Schedule>();
        double radius = Math.clamp(Spells.SLOW_TIME.property(DefaultProperties.EFFECT_RADIUS), 0, 32);
        for (var host : sources) {
            if (host == null || host.level() != level) continue;
            var effect = host.getEffect(EBMobEffects.SLOW_TIME.get());
            if (!host.isAlive() || effect == null) {
                HOSTS.remove(host);
                continue;
            }
            boolean stopped = host instanceof Player player && ArtifactChannel.isEquipped(player, EBItems.CHARM_STOP_TIME.get());
            int interval = 6 + 4 * Math.clamp(effect.getAmplifier(), 0, 16);
            boolean sleep = stopped || (host.tickCount + 1) % interval != 0;
            for (var target : level.getEntities(host, host.getBoundingBox().inflate(radius), entity -> eligible(host, entity, radius))) {
                var schedule = new Schedule(stopped, interval, sleep, host.getId());
                schedules.merge(target.getId(), schedule, Schedule::stronger);
                if (level.isClientSide && host.tickCount % 2 == 0) dust(level, target);
            }
        }

        var blocked = new HashSet<Integer>();
        schedules.forEach((id, schedule) -> { if (schedule.paused) blocked.add(id); });
        var previous = BLOCKED.put(level, Set.copyOf(blocked));
        if (level instanceof ServerLevel server) {
            for (var player : server.players()) {
                boolean current = blocked.contains(player.getId());
                if (current != (previous != null && previous.contains(player.getId()))) {
                    Services.NETWORK_HELPER.sendTo(player, new SlowTimeS2C(current));
                }
            }
        }
    }

    private static boolean eligible(LivingEntity host, Entity entity, double radius) {
        return entity.isAlive() && !entity.isSpectator() && entity.distanceToSqr(host) < radius * radius
                && !(entity instanceof LivingEntity living && living.hasEffect(EBMobEffects.SLOW_TIME.get()))
                && !entity.hasPassenger(passenger -> passenger instanceof LivingEntity living && living.hasEffect(EBMobEffects.SLOW_TIME.get()))
                && !(entity instanceof AbstractArrow && entity.isInWall())
                && (!(entity instanceof Player) || !entity.level().isClientSide && EBServerConfig.SLOW_TIME_AFFECTS_PLAYERS.get())
                && (EBServerConfig.SLOW_TIME_AFFECTS_PLAYERS.get() || !entity.hasPassenger(passenger -> passenger instanceof Player));
    }

    private record Schedule(boolean stopped, int interval, boolean paused, int host) {
        private Schedule stronger(Schedule other) {
            if (stopped != other.stopped) return stopped ? this : other;
            if (interval != other.interval) return interval > other.interval ? this : other;
            return host < other.host ? this : other;
        }
    }

    private static void dust(Level level, Entity entity) {
        var rng = level.random;
        double scale = entity.getBbWidth();
        var displacement = new Vec3((rng.nextDouble() * 2 - 1) * scale, (rng.nextDouble() * 2 - 1) * scale,
                (rng.nextDouble() * 2 - 1) * scale);
        ParticleBuilder.create(EBParticles.DUST).pos(entity.getBoundingBox().getCenter().add(displacement))
                .velocity(displacement.scale(-1.0 / 15)).color(0x5be3bb).time(15).spawn(level);
    }

    public static boolean blocks(Entity entity) {
        var set = BLOCKED.get(entity.level());
        return set != null && set.contains(entity.getId());
    }

    public static boolean blocksLocalPlayer(Level level) { return Boolean.TRUE.equals(LOCAL_BLOCKED.get(level)); }

    public static void setLocalBlocked(Level level, boolean blocked) { LOCAL_BLOCKED.put(level, blocked); }
}
