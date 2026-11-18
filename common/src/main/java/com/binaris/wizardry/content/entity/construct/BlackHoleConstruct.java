package com.binaris.wizardry.content.entity.construct;

import com.binaris.wizardry.api.content.entity.construct.ScaledConstructEntity;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.setup.registries.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class BlackHoleConstruct extends ScaledConstructEntity {
    private static final double SUCTION = 0.075;
    private static final int BLOCK_LIMIT = 3;
    private static final int BLOCK_SAMPLES_PER_TICK = 96;

    public BlackHoleConstruct(EntityType<?> type, Level level) {
        super(type, level);
        float base = Math.clamp(Spells.BLACK_HOLE.property(DefaultProperties.EFFECT_RADIUS), 0, 16);
        setBaseSize(base * 2, base);
    }

    public float visualRadius() { return Spells.BLACK_HOLE.property(DefaultProperties.EFFECT_RADIUS) * getSizeMultiplier(); }

    private double pullRadius() { return Math.clamp(2 * visualRadius() * getSizeMultiplier(), 0, 16); }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        if (level().isClientSide) {
            if (tickCount + 40 < getLifetime()) {
                for (int i = 0; i < 5; i++) {
                    level().addParticle(ParticleTypes.PORTAL, getX(), getY(), getZ(),
                            (random.nextDouble() - 0.5) * 4, (random.nextDouble() - 0.5) * 4 - 1,
                            (random.nextDouble() - 0.5) * 4);
                }
            }
            return;
        }
        int remaining = getLifetime() - tickCount;
        if (remaining == 75) playSound(EBSounds.ENTITY_BLACK_HOLE_VANISH.get(), 1.5f, 1);
        else if (tickCount % 80 == 1 && remaining > 80) playSound(EBSounds.ENTITY_BLACK_HOLE_AMBIENT.get(), 1.5f, 1);

        double radius = pullRadius();
        if (radius <= 0) return;
        var caster = getCaster();
        boolean terrain = caster instanceof Player player && EBServerConfig.PLAYER_BLOCK_DAMAGE.get()
                && player.mayBuild() && !player.isSpectator() && ArtifactChannel.isEquipped(player, EBItems.CHARM_BLACK_HOLE.get());
        if (terrain) uproot((Player) caster, radius);

        var center = position();
        for (var target : level().getEntities(this, new net.minecraft.world.phys.AABB(center, center).inflate(radius),
                entity -> entity.distanceToSqr(center) <= radius * radius)) {
            if (target instanceof LivingEntity living && living.isAlive() && isValidTarget(living)
                    && !living.getUUID().equals(getOwnerUUID())) {
                if (!(living instanceof Player) || caster instanceof Player) pull(living);
                if (distanceToSqr(living) <= 4) {
                    var source = MagicDamageSource.causeIndirectMagicDamage(this, caster, EBDamageSources.MAGIC);
                    living.hurt(source, 2 * damageMultiplier);
                }
            } else if (terrain && target instanceof FallingBlockEntity falling
                    && !(falling instanceof LevitatingBlock lifted && lifted.getOwner() != caster)
                    && falling.blockData == null) {
                if (falling instanceof LevitatingBlock lifted) lifted.suspend(caster);
                pull(falling);
                if (distanceToSqr(falling) <= 4) {
                    falling.playSound(EBSounds.ENTITY_BLACK_HOLE_BREAK_BLOCK.get(), 0.5f,
                            1 + (random.nextFloat() - random.nextFloat()) * 0.2f);
                    level().levelEvent(2001, falling.blockPosition(), Block.getId(falling.getBlockState()));
                    falling.discard();
                }
            }
        }
    }

    private void pull(Entity target) {
        Vec3 delta = position().subtract(target.position());
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(new Vec3(accelerate(motion.x, delta.x), accelerate(motion.y + target.getGravity(), delta.y),
                accelerate(motion.z, delta.z)));
        target.hasImpulse = true;
        target.hurtMarked = true;
        target.resetFallDistance();
    }

    private double accelerate(double velocity, double offset) {
        if (offset > 0 && velocity < 1) return Math.min(1, velocity + SUCTION);
        if (offset < 0 && velocity > -1) return Math.max(-1, velocity - SUCTION);
        return velocity;
    }

    private void uproot(Player owner, double radius) {
        int extent = (int) Math.ceil(radius);
        var world = level();
        int lifted = 0;
        for (int attempt = 0; attempt < BLOCK_SAMPLES_PER_TICK && lifted < BLOCK_LIMIT; attempt++) {
            var point = blockPosition().offset(random.nextIntBetweenInclusive(-extent, extent),
                    random.nextIntBetweenInclusive(-extent, extent), random.nextIntBetweenInclusive(-extent, extent));
            double distance = distanceToSqr(Vec3.atCenterOf(point));
            if (distance > radius * radius || random.nextInt(Math.max(1, (int) (3 * distance))) != 0
                    || !world.hasChunkAt(point) || !world.getWorldBorder().isWithinBounds(point)) continue;
            var state = world.getBlockState(point);
            if (state.isAir() || state.hasBlockEntity() || world.getBlockEntity(point) != null || !state.getFluidState().isEmpty()
                    || !state.isCollisionShapeFullBlock(world, point) || state.getDestroySpeed(world, point) < 0 || !world.mayInteract(owner, point)
                    || !BlockUtil.canBreak(owner, world, point, true)
                    || world.getBlockState(point) != state) continue;

            var falling = new LevitatingBlock(world, point, state, owner, 0);
            if (!world.setBlock(point, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)) continue;
            if (!world.addFreshEntity(falling)) {
                world.setBlock(point, state, Block.UPDATE_ALL);
                continue;
            }
            world.updateNeighborsAt(point, state.getBlock());
            lifted++;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BlackHoleAge", tickCount);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tickCount = tag.getInt("BlackHoleAge");
    }
}
