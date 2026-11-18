package com.binaris.wizardry.content.entity.construct;

import com.binaris.wizardry.api.content.entity.construct.MagicConstructEntity;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.spell.earth.Earthquake;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.binaris.wizardry.setup.registries.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;

public final class EarthquakeConstruct extends MagicConstructEntity {
    public EarthquakeConstruct(EntityType<?> type, Level level) { super(type, level); }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved() || level().isClientSide) return;
        var caster = getCaster();
        double radius = Math.min(64, tickCount * Earthquake.speed() + 1.5);
        if (caster instanceof Player player && EBServerConfig.PLAYER_BLOCK_DAMAGE.get() && player.mayBuild()) {
            bounceTerrain(player, radius);
        }
        for (LivingEntity target : EntityUtil.getLivingWithinRadius(radius, getX(), getY(), getZ(), level())) {
            if (!target.isAlive() || target.getUUID().equals(getOwnerUUID()) || target.isSpectator()
                    || Math.abs(target.getY() - getY()) >= 1 || distanceTo(target) <= radius - 1) continue;
            var motion = target.getDeltaMovement();
            if (isValidTarget(target)) {
                var source = MagicDamageSource.causeIndirectMagicDamage(this, caster, EBDamageSources.BLAST);
                EntityUtil.attackEntityWithoutKnockback(target, source, 10 * damageMultiplier);
                if (target.isAlive()) target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 1));
            }
            target.setDeltaMovement(motion.x, 0.8, motion.z);
            target.hasImpulse = true;
            target.hurtMarked = true;
        }
    }

    private void bounceTerrain(Player caster, double radius) {
        int samples = Math.clamp(getLifetime() * 3, 4, 192);
        var ring = new LinkedHashSet<BlockPos>();
        for (int i = 0; i < samples; i++) {
            double angle = 2 * Math.PI * i / samples;
            ring.add(BlockPos.containing(getX() + Math.sin(angle) * radius, getY() - 0.5,
                    getZ() + Math.cos(angle) * radius));
        }
        var level = level();
        for (var point : ring) {
            if (!level.hasChunkAt(point) || !level.getWorldBorder().isWithinBounds(point) || !level.mayInteract(caster, point)) continue;
            var state = level.getBlockState(point);
            var upper = level.getBlockState(point.above());
            if (state.isAir() || state.hasBlockEntity() || level.getBlockEntity(point) != null || !state.getFluidState().isEmpty()
                    || state.getDestroySpeed(level, point) < 0 || !state.isCollisionShapeFullBlock(level, point)
                    || !upper.getCollisionShape(level, point.above()).isEmpty() || !BlockUtil.canBreak(caster, level, point, false)
                    || level.getBlockState(point) != state) continue;
            var debris = new LevitatingBlock(level, point, state, caster, 0);
            debris.enableDebrisRecovery();
            debris.setDeltaMovement(new Vec3(0, 0.3, 0));
            if (!level.addFreshEntity(debris)) continue;
            if (level.getBlockState(point) != state || !level.setBlock(point, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) {
                debris.discard();
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("QuakeAge", tickCount);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tickCount = tag.getInt("QuakeAge");
    }
}
