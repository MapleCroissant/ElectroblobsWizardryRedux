package com.binaris.wizardry.content.entity.projectile;

import com.binaris.wizardry.api.content.entity.projectile.MagicProjectileEntity;
import com.binaris.wizardry.content.spell.fire.Disintegration;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public final class EmberEntity extends MagicProjectileEntity {
    private int extraLifetime;

    public EmberEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
        extraLifetime = random.nextInt(30);
    }

    @Override
    public int getLifeTime() { return Spells.DISINTEGRATION.property(Disintegration.EMBER_LIFETIME) + extraLifetime; }

    @Override
    public float getSeekingStrength() { return 0; }

    @Override
    public float getDamage(EntityHitResult hit) { return 0; }

    @Override
    public ResourceKey<DamageType> getDamageType(EntityHitResult hit) { return EBDamageSources.FIRE; }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (!level().isClientSide && hit.getEntity() instanceof LivingEntity living) ignite(living);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        var face = hit.getDirection();
        switch (face.getAxis()) {
            case X -> setDeltaMovement(getDeltaMovement().multiply(0, 1, 1));
            case Y -> {
                setDeltaMovement(getDeltaMovement().multiply(0.5, 0, 0.5));
                if (face == net.minecraft.core.Direction.UP) setNoGravity(true);
            }
            case Z -> setDeltaMovement(getDeltaMovement().multiply(1, 1, 0));
        }
    }

    @Override
    public void tick() {
        if (isNoGravity() && level().getBlockState(blockPosition().below()).isAir()) setNoGravity(false);
        super.tick();
        if (isRemoved()) return;
        if (level().isClientSide) {
            if (random.nextFloat() > (float) tickCount / Math.max(1, getLifeTime())) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0, 0);
            }
        } else {
            for (var target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox(), LivingEntity::isAlive)) ignite(target);
        }
    }

    private void ignite(LivingEntity target) {
        if (isValidTarget(target) && !target.fireImmune()) {
            target.igniteForSeconds(Spells.DISINTEGRATION.property(Disintegration.BURN_DURATION));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("EmberAge", tickCount);
        tag.putInt("ExtraLifetime", extraLifetime);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tickCount = tag.getInt("EmberAge");
        extraLifetime = tag.getInt("ExtraLifetime");
    }
}
