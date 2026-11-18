package com.binaris.wizardry.content.blockentity;

import com.binaris.wizardry.setup.registries.EBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class MagicLightBlockEntity extends BlockEntityTimer {
    public MagicLightBlockEntity(BlockPos pos, BlockState state) {
        super(EBBlockEntities.MAGIC_LIGHT.get(), pos, state, 600);
    }

    @Override
    public void setLifetime(int lifetime) {
        super.setLifetime(lifetime);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void tick(Level level) {
        if (timer < Integer.MAX_VALUE) timer++;
        if (!level.isClientSide && maxTimer > 0) {
            if (timer >= maxTimer) level.removeBlock(worldPosition, false);
            else if (timer % 20 == 0) setChanged();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
