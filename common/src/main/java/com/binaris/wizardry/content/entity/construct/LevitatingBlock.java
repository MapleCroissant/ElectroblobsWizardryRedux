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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
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
    private boolean recoverDebris;

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
                recoverOrDiscard();
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
        if (caster != null && EBServerConfig.PLAYER_BLOCK_DAMAGE.get() && BlockUtil.canPlaceBlock(caster, level, pos)
                && displaced.canBeReplaced() && state.canSurvive(level, pos)
                && level.isUnobstructed(this, state.getCollisionShape(level, pos, CollisionContext.empty()).move(pos.getX(), pos.getY(), pos.getZ()))
                && level.setBlock(pos, state, Block.UPDATE_ALL)) {
            if (blockData != null) {
                var contents = level.getBlockEntity(pos);
                if (!matchesPortableBlock(state, contents)) {
                    level.setBlock(pos, displaced, Block.UPDATE_ALL);
                    recoverOrDiscard();
                    return;
                }
                contents.loadWithComponents(blockData, level.registryAccess());
                contents.setChanged();
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
            discard();
            if (state.getBlock() instanceof FallingBlock falling) falling.onLand(level, pos, state, displaced, this);
        } else {
            recoverOrDiscard();
        }
    }

    private boolean matchesPortableBlock(BlockState state, @Nullable BlockEntity contents) {
        return state.is(Blocks.BARREL) && contents instanceof BarrelBlockEntity
                || state.getBlock() instanceof ShulkerBoxBlock && contents instanceof ShulkerBoxBlockEntity;
    }

    public void enableDebrisRecovery() { recoverDebris = true; }

    private void recoverOrDiscard() {
        if (blockData == null && recoverDebris) {
            var item = new net.minecraft.world.item.ItemStack(getBlockState().getBlock());
            if (item.isEmpty() || spawnAtLocation(item) != null) discard();
            return;
        }
        if (blockData == null) {
            level().levelEvent(2001, blockPosition(), Block.getId(getBlockState()));
            discard();
            return;
        }

        var position = blockPosition().atY(net.minecraft.util.Mth.clamp(blockPosition().getY(),
                level().getMinBuildHeight() + 1, level().getMaxBuildHeight() - 1));
        var state = getBlockState();
        var contents = BlockEntity.loadStatic(position, state, blockData, level().registryAccess());
        if (!matchesPortableBlock(state, contents)) return;
        var stack = new net.minecraft.world.item.ItemStack(state.getBlock());
        contents.saveToItem(stack, level().registryAccess());
        var item = new net.minecraft.world.entity.item.ItemEntity(level(),
                position.getX() + 0.5, position.getY(), position.getZ() + 0.5, stack);
        item.setDefaultPickUpDelay();
        setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if (level().addFreshEntity(item)) discard();
    }
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Caster", owner);
        tag.putFloat("ImpactDamage", impactDamage);
        tag.putInt("Suspension", suspension);
        tag.putBoolean("RecoverDebris", recoverDebris);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Caster") ? tag.getUUID("Caster") : null;
        ownerReference.clear();
        impactDamage = tag.getFloat("ImpactDamage");
        suspension = tag.getInt("Suspension");
        recoverDebris = tag.getBoolean("RecoverDebris");
        entityData.set(SUSPENDED, suspension > 0);
        dropItem = false;
    }
}
