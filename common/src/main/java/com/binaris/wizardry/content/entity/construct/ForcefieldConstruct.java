package com.binaris.wizardry.content.entity.construct;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.entity.construct.ScaledConstructEntity;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.ClientSpellSoundManager;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.setup.registries.*;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;

public final class ForcefieldConstruct extends ScaledConstructEntity {
    public ForcefieldConstruct(EntityType<?> type, Level level) {
        super(type, level);
        float diameter = 2f * Spells.FORCEFIELD.property(DefaultProperties.EFFECT_RADIUS);
        setBaseSize(diameter, diameter);
    }

    public double radius() { return Math.clamp(baseWidth * sizeMultiplier / 2, 0.1, 16); }

    @Override
    protected AABB makeBoundingBox() { return new AABB(position(), position()).inflate(radius()); }

    @Override
    public boolean isPickable() { return !isRemoved(); }

    public boolean contains(Vec3 point) { return position().distanceToSqr(point) < radius() * radius(); }

    public boolean contains(AABB box) {
        double x = Math.max(Math.abs(box.minX - getX()), Math.abs(box.maxX - getX()));
        double y = Math.max(Math.abs(box.minY - getY()), Math.abs(box.maxY - getY()));
        double z = Math.max(Math.abs(box.minZ - getZ()), Math.abs(box.maxZ - getZ()));
        return x * x + y * y + z * z < radius() * radius();
    }

    public Optional<Vec3> intersect(Vec3 start, Vec3 end, double inflation) {
        var move = end.subtract(start);
        double a = move.lengthSqr();
        if (a < 1.0e-8) return Optional.empty();
        var offset = start.subtract(position());
        double b = offset.dot(move);
        double r = radius() + inflation;
        double determinant = b * b - a * (offset.lengthSqr() - r * r);
        if (determinant < 0) return Optional.empty();
        double root = Math.sqrt(determinant);
        double t = (-b - root) / a;
        if (t <= 1.0e-7) t = (-b + root) / a;
        return t > 1.0e-7 && t <= 1 ? Optional.of(start.add(move.scale(t))) : Optional.empty();
    }

    @Nullable
    public static ForcefieldConstruct surrounding(Level level, Vec3 point) {
        return level.getEntitiesOfClass(ForcefieldConstruct.class, new AABB(point, point).inflate(16),
                        field -> field.contains(point)).stream()
                .min(Comparator.comparingDouble(field -> field.distanceToSqr(point))).orElse(null);
    }

    @Nullable
    private static ForcefieldConstruct surrounding(Entity entity) {
        return entity.level().getEntitiesOfClass(ForcefieldConstruct.class, entity.getBoundingBox().inflate(16),
                        field -> field.contains(entity.getBoundingBox())).stream()
                .min(Comparator.comparingDouble(field -> field.distanceToSqr(entity))).orElse(null);
    }

    public static boolean crosses(Entity entity, Vec3 target) {
        return surrounding(entity.level(), entity.position()) != surrounding(entity.level(), target);
    }

    public static boolean blocksDamage(LivingEntity victim, net.minecraft.world.damagesource.DamageSource source) {
        var attacker = source.getEntity() == null ? source.getDirectEntity() : source.getEntity();
        if (attacker == null || attacker instanceof ForcefieldConstruct || source.getDirectEntity() instanceof ForcefieldConstruct
                || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)) return false;
        if (attacker instanceof Player player && source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
                && ArtifactChannel.isEquipped(player, EBItems.RING_DEFENDER.get())) return false;
        return surrounding(victim) != surrounding(attacker);
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        if (level().isClientSide) {
            if (tickCount == 1) ClientSpellSoundManager.playMovingSound(this, EBSounds.ENTITY_FORCEFIELD_AMBIENT.get(),
                    SoundSource.PLAYERS, 0.5f, 1, true);
            return;
        }
        var caster = getCaster();
        boolean defender = caster instanceof Player player && ArtifactChannel.isEquipped(player, EBItems.RING_DEFENDER.get());
        boolean interdiction = caster instanceof Player player && ArtifactChannel.isEquipped(player, EBItems.RING_INTERDICTION.get());
        for (var target : level().getEntities(this, getBoundingBox().inflate(4), entity -> entity.isAlive() && !entity.isSpectator()
                && !(entity instanceof ExperienceOrb || entity instanceof ForcefieldConstruct) && isValidTarget(entity))) {
            if (defender && target instanceof Projectile projectile && projectile.getOwner() != null && !isValidTarget(projectile.getOwner())) continue;
            var center = target.getBoundingBox().getCenter();
            var velocity = target.getDeltaMovement();
            var next = center.add(velocity);
            if (target instanceof LivingEntity && contains(next)) {
                repel(target, center, true, interdiction);
            } else if (!(target instanceof LivingEntity)) {
                intersect(center, next, 0).ifPresent(hit -> repel(target, hit, false, false));
            }
        }
    }

    private void repel(Entity target, Vec3 point, boolean living, boolean interdiction) {
        Vec3 normal = point.subtract(position()).normalize();
        if (normal.lengthSqr() < 1.0e-8) normal = new Vec3(0, 1, 0);
        boolean inside = contains(target.getBoundingBox().getCenter());
        Vec3 velocity = target.getDeltaMovement().scale(-0.2).add(normal.scale(living ? 0.25 : inside ? -0.1 : 0.1));
        if (target.onGround() && velocity.y < 0) velocity = new Vec3(velocity.x, 0.1, velocity.z);
        Vec3 desiredCenter = position().add(normal.scale(radius() + (inside && !living ? -0.1 : 0.1)));
        Vec3 shift = desiredCenter.subtract(target.getBoundingBox().getCenter());
        if (shift.lengthSqr() <= 16 && level().noCollision(target, target.getBoundingBox().move(shift))) {
            target.setPos(target.position().add(shift));
        }
        target.setDeltaMovement(velocity);
        target.hurtMarked = true;
        target.hasImpulse = true;
        if (interdiction) target.hurt(MagicDamageSource.causeIndirectMagicDamage(this, getCaster(), EBDamageSources.MAGIC), 1);
        target.playSound(EBSounds.ENTITY_FORCEFIELD_DEFLECT.get(), 0.3f, 1.3f);
        var surface = position().add(normal.scale(radius()));
        float yaw = (float) Math.toDegrees(Math.atan2(normal.x, -normal.z));
        float pitch = (float) Math.toDegrees(Math.asin(normal.y));
        ParticleBuilder.create(EBParticles.FLASH).pos(surface).color(0.9f, 0.95f, 1).time(6)
                .face(yaw, pitch).allowServer(true).spawn(level());
        if (tickCount % 5 == 0) setLifetime((int) (getLifetime() * 0.99));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ForcefieldAge", tickCount);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tickCount = tag.getInt("ForcefieldAge");
    }
}
