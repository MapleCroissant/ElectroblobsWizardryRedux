package com.binaris.wizardry.content.entity.construct;

import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.core.AllyDesignation;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.binaris.wizardry.core.mixin.accessor.FallingBlockEntityAccessor;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.EBEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.UUID;

public final class LevitatingBlock extends FallingBlockEntity implements TraceableEntity {
    private static final EntityDataAccessor<Boolean> SUSPENDED = SynchedEntityData.defineId(
            LevitatingBlock.class, EntityDataSerializers.BOOLEAN);
    private @Nullable UUID owner;
    private WeakReference<LivingEntity> ownerReference = new WeakReference<>(null);
    private float impactDamage;
    private int suspension;

    public LevitatingBlock(EntityType<? extends LevitatingBlock> type, Level level) {
        super(type, level);
        dropItem = false;
    }

    public LevitatingBlock(Level level, BlockPos pos, BlockState state, LivingEntity caster, float damage) {
        this(EBEntities.LEVITATING_BLOCK.get(), level);
        ((FallingBlockEntityAccessor) (FallingBlockEntity) this).ebwizardry$setCarriedState(state);
        setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        setStartPos(pos);
        blocksBuilding = true;
        impactDamage = damage;
        suspend(caster);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SUSPENDED, false);
    }

    public void suspend(LivingEntity caster) {
        owner = caster.getUUID();
        if (ownerReference.get() != caster) ownerReference = new WeakReference<>(caster);
        suspension = 5;
        entityData.set(SUSPENDED, true);
    }

    @Override
    public @Nullable LivingEntity getOwner() {
        LivingEntity cached = ownerReference.get();
        if (cached != null && !cached.isRemoved() && cached.level() == level()) return cached;
        if (owner != null && level() instanceof ServerLevel server && server.getEntity(owner) instanceof LivingEntity living) {
            ownerReference = new WeakReference<>(living);
            return living;
        }
        return null;
    }

    @Override
    public void tick() {
        if (getBlockState().isAir()) {
            discard();
            return;
        }
        if (!level().isClientSide && suspension > 0 && --suspension == 0) entityData.set(SUSPENDED, false);
        time++;
        applyGravity();
        var motion = getDeltaMovement();
        move(MoverType.SELF, motion);
        if (!level().isClientSide) {
            damageOnImpact(motion);
            if (time > 600 || level().isOutsideBuildHeight(blockPosition())) {
                discard();
                return;
            }
            if (onGround() && !entityData.get(SUSPENDED)) {
                settle();
                return;
            }
        }
        setDeltaMovement(getDeltaMovement().scale(0.98));
    }

    private void damageOnImpact(net.minecraft.world.phys.Vec3 motion) {
        double speedSquared = motion.lengthSqr();
        if (speedSquared < 0.2 || impactDamage <= 0) return;
        var caster = getOwner();
        float damage = impactDamage * (float) Math.min(1, speedSquared / 0.4);
        for (var target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox(),
                entity -> (owner == null || !owner.equals(entity.getUUID())) && AllyDesignation.isValidTarget(caster, entity))) {
            if (target.hurt(MagicDamageSource.causeIndirectMagicDamage(this, caster, EBDamageSources.FORCE), damage)) {
                target.knockback(0.6, -motion.x, -motion.z);
            }
        }
    }

    private void settle() {
        Level level = level();
        BlockPos pos = blockPosition();
        BlockState displaced = level.getBlockState(pos);
        BlockState state = getBlockState();
        var caster = getOwner();
        discard();
        if (caster != null && EBServerConfig.PLAYER_BLOCK_DAMAGE.get() && BlockUtil.canPlaceBlock(caster, level, pos)
                && displaced.canBeReplaced() && state.canSurvive(level, pos)
                && level.isUnobstructed(state, pos, CollisionContext.empty()) && level.setBlock(pos, state, Block.UPDATE_ALL)) {
            if (state.getBlock() instanceof FallingBlock falling) falling.onLand(level, pos, state, displaced, this);
        } else {
            level.levelEvent(2001, pos, Block.getId(state));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Caster", owner);
        tag.putFloat("ImpactDamage", impactDamage);
        tag.putInt("Suspension", suspension);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Caster") ? tag.getUUID("Caster") : null;
        ownerReference.clear();
        impactDamage = tag.getFloat("ImpactDamage");
        suspension = tag.getInt("Suspension");
        entityData.set(SUSPENDED, suspension > 0);
        dropItem = false;
    }
}
