package com.binaris.wizardry.content.block;

import com.binaris.wizardry.content.blockentity.LecternBlockEntity;
import com.binaris.wizardry.core.networking.s2c.OpenLecternS2C;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.EBBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

public final class IndexedLecternBlock extends MagicLecternBlock implements EntityBlock {
    public static final MapCodec<IndexedLecternBlock> CODEC = MapCodec.unit(IndexedLecternBlock::new);

    @Override
    protected MapCodec<? extends MagicLecternBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) {
            Services.NETWORK_HELPER.sendTo(serverPlayer, new OpenLecternS2C(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LecternBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide || type != EBBlockEntities.LECTERN.get()) return null;
        return (world, pos, block, entity) -> ((LecternBlockEntity) entity).animate(world);
    }
}
