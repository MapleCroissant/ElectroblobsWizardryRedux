package com.binaris.wizardry.content.entity.living;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.content.entity.projectile.LightningDiscEntity;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.core.mixin.accessor.MobGoalsAccessor;
import com.binaris.wizardry.setup.registries.EBEntities;
import com.binaris.wizardry.setup.registries.EBSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.NotNull;


public class StormElemental extends Blaze implements RangedAttackMob {

    public StormElemental(EntityType<? extends Blaze> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 10;
        ((MobGoalsAccessor) this).getPathfindingMalus().remove(PathType.WATER);
        ((MobGoalsAccessor) this).getPathfindingMalus().remove(PathType.LAVA);
        ((MobGoalsAccessor) this).getPathfindingMalus().remove(PathType.DANGER_FIRE);
        ((MobGoalsAccessor) this).getPathfindingMalus().remove(PathType.DAMAGE_FIRE);
    }

    public StormElemental(Level level) {
        this(EBEntities.STORM_ELEMENTAL.get(), level);
    }

    @Override
    public void spawnAnim() {
        super.spawnAnim();
        spawnParticles();
    }

    @Override
    protected void tickDeath() {
        super.tickDeath();
        if (this.deathTime == 1) this.spawnParticles();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount % 120 == 1) {
            this.playSound(EBSounds.ENTITY_STORM_ELEMENTAL_WIND.get(), 1.0f, 1.0f);
        }

        // Ambient burn/crackle sound
        if (this.random.nextInt(24) == 0) {
            this.playSound(EBSounds.ENTITY_STORM_ELEMENTAL_BURN.get(), 1.0F + this.random.nextFloat(),
                    this.random.nextFloat() * 0.7F + 0.3F);
        }

        // Slow fall effect
        if (!this.onGround() && this.getDeltaMovement().y < 0.0D) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6, 1.0));
        }

        // Particle effects
        if (level().isClientSide) {
            // Large smoke particles
            for (int i = 0; i < 2; ++i) {
                level().addParticle(ParticleTypes.LARGE_SMOKE,
                        this.getX() + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth(),
                        this.getY() + this.random.nextDouble() * (double) this.getBbHeight(),
                        this.getZ() + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth(),
                        0.0D, 0.0D, 0.0D);

                // Spark particles
                ParticleBuilder.create(EBParticles.SPARK, this).spawn(level());
            }

            // Spinning cyan sparkles around the body
            for (int i = 0; i < 10; i++) {
                float brightness = random.nextFloat() * 0.2f;
                double dy = this.random.nextDouble() * (double) this.getBbHeight();

                ParticleBuilder.create(EBParticles.SPARKLE)
                        .pos(this.getX(), this.getY() + dy, this.getZ())
                        .time(20 + random.nextInt(10))
                        .color(0, brightness, brightness)
                        .spin(0.2 + 0.5 * dy, 0.1 + 0.05 * random.nextDouble())
                        .spawn(level());
            }
        }
    }

    private void spawnParticles() {
        if (!level().isClientSide) return;

        for (int i = 0; i < 15; i++) {
            float brightness = random.nextFloat() * 0.3f;
            ParticleBuilder.create(EBParticles.SPARKLE, this)
                    .velocity(0, 0.05, 0)
                    .time(20 + random.nextInt(10))
                    .color(brightness, brightness + 0.2f, 1.0f)
                    .spawn(level());
        }
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    public void thunderHit(@NotNull ServerLevel level, @NotNull LightningBolt lightning) {
        // Immune to lightning strikes - do nothing
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        var disc = new LightningDiscEntity(EBEntities.LIGHTNING_DISC.get(), level());
        disc.setOwner(this);
        var start = getEyePosition().add(0, -0.25, 0);
        var direction = target.getBoundingBox().getCenter().subtract(start);
        disc.setPos(start.add(direction.normalize()));
        disc.shoot(direction.x, direction.y, direction.z, 1, EntityUtil.getDefaultAimingError(level().getDifficulty()));
        if (level().addFreshEntity(disc)) {
            playSound(EBSounds.SPELL_LIGHTNING_DISC.get(), 1, 0.95f / (random.nextFloat() * 0.3f + 0.85f));
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(EBDamageSources.SHOCK) || super.isInvulnerableTo(source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return EBSounds.ENTITY_STORM_ELEMENTAL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return EBSounds.ENTITY_STORM_ELEMENTAL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return EBSounds.ENTITY_STORM_ELEMENTAL_DEATH.get();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals((goal) -> true);
        this.targetSelector.removeAllGoals((goal) -> true);

        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1, false) {
            @Override
            public boolean canUse() {
                return getTarget() != null && distanceToSqr(getTarget()) < 4 && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return getTarget() != null && distanceToSqr(getTarget()) < 4 && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(4, new RangedAttackGoal(this, 1, 90, 15));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0F));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0F, 0.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
}