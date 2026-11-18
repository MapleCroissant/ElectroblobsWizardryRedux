package com.binaris.wizardry.content.block;

import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.content.blockentity.StatueBlockEntity;
import com.binaris.wizardry.core.config.EBServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.UUID;

public final class StatueBlock extends Block implements EntityBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    private final boolean ice;

    public StatueBlock(boolean ice) {
        super(Properties.of().noOcclusion().dynamicShape().noLootTable().strength(ice ? 0.5f : 3)
                .sound(ice ? SoundType.GLASS : SoundType.STONE).pushReaction(PushReaction.BLOCK));
        this.ice = ice;
        registerDefaultState(stateDefinition.any().setValue(PART, 0));
    }

    public boolean icy() { return ice; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART); }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return ice ? RenderShape.MODEL : RenderShape.INVISIBLE; }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return ice && adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (ice || !(level.getBlockEntity(pos) instanceof StatueBlockEntity statue)) {
            return net.minecraft.world.phys.shapes.Shapes.block();
        }
        float half = Math.clamp(statue.width() * 8, 1, 8);
        float height = Math.clamp((statue.height() - state.getValue(PART)) * 16, 1, 16);
        return box(8 - half, 0, 8 - half, 8 + half, height, 8 + half);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StatueBlockEntity(pos, state); }

    public boolean convert(Mob target, LivingEntity caster, int duration) {
        if (!(target.level() instanceof ServerLevel level) || duration <= 0 || !target.isAlive()
                || target.isVehicle() || target.isPassenger() || caster == null
                || caster instanceof Player && !EBServerConfig.PLAYER_BLOCK_DAMAGE.get()) return false;
        var base = target.blockPosition();
        float height = target.getBbHeight();
        float width = target.getBbWidth();
        int count = height < 1.2 || target.isBaby() ? 1 : height < 2.5 ? 2 : 3;
        if (height > 3 || width > 3) return false;
        var old = new ArrayList<BlockState>(count);
        for (int i = 0; i < count; i++) {
            var position = base.above(i);
            if (level.isOutsideBuildHeight(position) || !level.hasChunkAt(position) || !level.getWorldBorder().isWithinBounds(position)
                    || !BlockUtil.canBlockBeReplaced(level, position, true) || level.getBlockEntity(position) != null
                    || !BlockUtil.canPlaceBlock(caster, level, position)) return false;
            old.add(level.getBlockState(position));
        }
        CompoundTag payload = new CompoundTag();
        if (!target.save(payload)) return false;
        payload.putShort("Fire", (short) 0);

        var statues = new ArrayList<StatueBlockEntity>(count);
        for (int i = 0; i < count; i++) {
            var position = base.above(i);
            if (!level.setBlock(position, defaultBlockState().setValue(PART, i), UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE)
                    || !(level.getBlockEntity(position) instanceof StatueBlockEntity statue)) {
                for (int rollback = 0; rollback <= i; rollback++) {
                    var point = base.above(rollback);
                    if (level.getBlockState(point).is(this)) level.setBlock(point, old.get(rollback), UPDATE_ALL);
                }
                return false;
            }
            statues.add(statue);
        }

        UUID id = UUID.randomUUID();
        for (var statue : statues) {
            statue.configure(id, base, count, duration, width, height, statue.getBlockPos().equals(base) ? payload : null);
        }
        target.discard();
        for (var statue : statues) level.updateNeighborsAt(statue.getBlockPos(), this);
        level.scheduleTick(base, this, ice ? duration : 200);
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) != 0 || !(level.getBlockEntity(pos) instanceof StatueBlockEntity statue)) return;
        boolean expired = statue.age() >= statue.duration();
        if (expired && (ice || level.getMaxLocalRawBrightness(pos) < random.nextInt(12) - 3)) statue.requestRelease(level);
        else level.scheduleTick(pos, this, ice ? Math.max(1, statue.duration() - statue.age()) : 200);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (level instanceof ServerLevel server && !replacement.is(this) && level.getBlockEntity(pos) instanceof StatueBlockEntity statue) {
            statue.requestRelease(server);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
