package com.binaris.wizardry.content.block;

import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.blockentity.SnareBlockEntity;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.AllyDesignation;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SnareBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);

    public SnareBlock() {
        super(Properties.of().noCollission().noOcclusion().noLootTable().instabreak()
                .pushReaction(PushReaction.DESTROY).sound(SoundType.GRASS));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return side == Direction.DOWN && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof LivingEntity target) || !target.isAlive()
                || !(level.getBlockEntity(pos) instanceof SnareBlockEntity snare)) return;
        var uuid = snare.caster();
        var caster = uuid != null && server.getEntity(uuid) instanceof LivingEntity living ? living : null;
        if (target.getUUID().equals(uuid) || !AllyDesignation.isValidTarget(caster, target)) return;
        if (!level.removeBlock(pos, false)) return;

        var source = caster == null ? level.damageSources().cactus()
                : MagicDamageSource.causeDirectMagicDamage(level, caster, EBDamageSources.MAGIC);
        target.hurt(source, Spells.SNARE.property(DefaultProperties.DAMAGE));
        int duration = Spells.SNARE.property(DefaultProperties.EFFECT_DURATION);
        if (duration > 0) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration,
                Spells.SNARE.property(DefaultProperties.EFFECT_STRENGTH)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SnareBlockEntity(pos, state); }
}
