package com.binaris.wizardry.content.entity.construct;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.entity.construct.MagicConstructEntity;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.item.LightningHammerItem;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.lightning.LightningBoltSpell;
import com.binaris.wizardry.content.spell.lightning.LightningHammer;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.core.networking.s2c.ScreenShakeS2C;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.*;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Comparator;

public final class LightningHammerConstruct extends MagicConstructEntity {
    private static final EntityDataAccessor<Boolean> SPIN = SynchedEntityData.defineId(LightningHammerConstruct.class, EntityDataSerializers.BOOLEAN);
    private double descent;

    public LightningHammerConstruct(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = false;
    }

    public LightningHammerConstruct(Level level) {
        this(EBEntities.LIGHTNING_HAMMER.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPIN, false);
    }

    public void startSpin() { entityData.set(SPIN, true); }
    public boolean spinning() { return entityData.get(SPIN); }

    @Override
    public boolean isPickable() { return !isRemoved(); }

    public int remainingLifetime() { return Math.max(0, getLifetime() - tickCount); }

    @Override
    public void tick() {
        if (remainingLifetime() == 0) {
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.8, getZ(), 1, 0, 0, 0, 0);
                playSound(EBSounds.ENTITY_HAMMER_EXPLODE.get(), 1, 1);
                discard();
            }
            return;
        }
        super.tick();
        if (level().isClientSide) {
            if (tickCount % 3 == 0) ParticleBuilder.create(EBParticles.SPARK, this).spawn(level());
            return;
        }

        boolean wasGrounded = onGround();
        double previousY = getY();
        var velocity = getDeltaMovement().add(0, -0.04, 0);
        move(MoverType.SELF, velocity);
        descent += Math.max(0, previousY - getY());
        setDeltaMovement(velocity.multiply(horizontalCollision ? 0 : onGround() ? 0.7 : 0.98,
                verticalCollision ? 0 : 0.98, horizontalCollision ? 0 : onGround() ? 0.7 : 0.98));
        if (onGround()) {
            if (!wasGrounded) land((ServerLevel) level());
            descent = 0;
            entityData.set(SPIN, false);
            attackNearby();
        } else {
            for (var target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox(),
                    target -> target.isAlive() && isValidTarget(target))) {
                target.hurt(MagicDamageSource.causeIndirectMagicDamage(this, getCaster(), EBDamageSources.SHOCK),
                        Spells.LIGHTNING_HAMMER.property(LightningHammer.DIRECT_DAMAGE) * damageMultiplier);
            }
        }
    }

    private void land(ServerLevel level) {
        var floor = level.getBlockState(getOnPos());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), getX(), getY(), getZ(), 40, 0.6, 0, 0.6, 0.15);
        if (descent > 10) {
            LightningBoltSpell.summon(level, position(), getCaster(), 5);
            for (var player : level.players()) {
                if (distanceToSqr(player) <= 100) Services.NETWORK_HELPER.sendTo(player, new ScreenShakeS2C(6, 10));
            }
        }
        playSound(EBSounds.ENTITY_HAMMER_LAND.get(), 1, 0.6f);
    }

    private void attackNearby() {
        var spell = Spells.LIGHTNING_HAMMER;
        int interval = Math.max(1, spell.property(LightningHammer.ATTACK_INTERVAL));
        var targets = EntityUtil.getLivingWithinRadius(spell.property(DefaultProperties.EFFECT_RADIUS), getX(), getY() + 1, getZ(), level());
        targets.stream().filter(target -> target.isAlive() && isValidTarget(target))
                .sorted(Comparator.comparingDouble(this::distanceToSqr))
                .limit(Math.max(0, spell.property(LightningHammer.TARGET_LIMIT)))
                .filter(target -> target.tickCount % interval == 0)
                .forEach(target -> {
                    ParticleBuilder.create(EBParticles.LIGHTNING).allowServer(true).pos(getX(), getY() + getBbHeight() - 0.1, getZ())
                            .target(target).spawn(level());
                    ParticleBuilder.spawnShockParticles(level(), target.getX(), target.getY() + target.getBbHeight(), target.getZ());
                    target.playSound(EBSounds.ENTITY_HAMMER_ATTACK.get(), 1, 1.5f + random.nextFloat() * 0.4f);
                    var source = MagicDamageSource.causeIndirectMagicDamage(this, getCaster(), EBDamageSources.SHOCK);
                    if (EntityUtil.attackEntityWithoutKnockback(target, source, spell.property(LightningHammer.SPLASH_DAMAGE) * damageMultiplier)
                            && getCaster() != null) {
                        target.knockback(0.4, getX() - target.getX(), getZ() - target.getZ());
                    }
                });
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return player.getMainHandItem().isEmpty() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        if (isRemoved() || !player.getUUID().equals(getOwnerUUID()) || tickCount <= 10 || remainingLifetime() == 0
                || !player.getMainHandItem().isEmpty() || !ArtifactChannel.isEquipped(player, EBItems.RING_HAMMER.get())) {
            return InteractionResult.PASS;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, LightningHammerItem.create(player, remainingLifetime(), damageMultiplier));
        discard();
        return InteractionResult.CONSUME;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HammerAge", tickCount);
        tag.putDouble("HammerDescent", descent);
        tag.putBoolean("HammerSpin", spinning());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tickCount = tag.getInt("HammerAge");
        descent = tag.getDouble("HammerDescent");
        entityData.set(SPIN, tag.getBoolean("HammerSpin"));
    }
}
