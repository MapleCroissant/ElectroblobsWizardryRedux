package com.binaris.wizardry.content.block;

import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.blockentity.ThornsBlockEntity;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.AllyDesignation;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

public final class ThornsBlock extends Block implements EntityBlock {
    public ThornsBlock() {
        super(Properties.of().noCollission().noOcclusion().noLootTable().strength(4)
                .sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY).offsetType(OffsetType.XZ));
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                .setValue(BlockStateProperties.AGE_7, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.DOUBLE_BLOCK_HALF, BlockStateProperties.AGE_7);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isLower(state) ? new ThornsBlockEntity(pos, state) : null;
    }

    private static boolean isLower(BlockState state) {
        return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        boolean lower = isLower(state);
        if (direction == (lower ? Direction.UP : Direction.DOWN) && (!neighbor.is(this) || isLower(neighbor) == lower)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (lower && direction == Direction.DOWN && !neighbor.isFaceSturdy(level, neighborPos, Direction.UP)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isLower(state) || !(level.getBlockEntity(pos) instanceof ThornsBlockEntity thorns)) return;
        long age = thorns.age(level.getGameTime());
        if (age >= thorns.duration()) {
            level.removeBlock(pos, false);
            return;
        }
        int stage = (int) Math.min(7, age / 2);
        var upper = level.getBlockState(pos.above());
        if (!upper.is(this) || isLower(upper)) {
            level.removeBlock(pos, false);
            return;
        }
        level.setBlock(pos, state.setValue(BlockStateProperties.AGE_7, stage), Block.UPDATE_CLIENTS);
        level.setBlock(pos.above(), upper.setValue(BlockStateProperties.AGE_7, stage), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, this, (int) Math.min(thorns.duration() - age, stage == 7 ? Integer.MAX_VALUE : 2));
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity target) {
        if (canAffect(state, level, pos, target)) target.makeStuckInBlock(state, new Vec3(0.25, 0.05, 0.25));
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) canAffect(state, level, pos, player);
    }

    private boolean canAffect(BlockState state, Level level, BlockPos pos, Entity target) {
        if (!(level.getBlockEntity(isLower(state) ? pos : pos.below()) instanceof ThornsBlockEntity thorns)) return false;
        var owner = thorns.caster();
        if (target.getUUID().equals(owner)) return false;
        var caster = owner != null && EntityUtil.getEntityByUUID(level, owner) instanceof LivingEntity living ? living : null;
        if (!AllyDesignation.isValidTarget(caster, target)) return false;
        if (!level.isClientSide && target.tickCount % 20 == 0) {
            var source = caster == null ? level.damageSources().cactus()
                    : MagicDamageSource.causeDirectMagicDamage(level, caster, EBDamageSources.MAGIC);
            EntityUtil.attackEntityWithoutKnockback(target, source, Spells.FOREST_OF_THORNS.property(DefaultProperties.DAMAGE) * thorns.potency());
        }
        return true;
    }
}
