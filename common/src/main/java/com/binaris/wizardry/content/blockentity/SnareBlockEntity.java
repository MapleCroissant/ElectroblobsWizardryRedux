package com.binaris.wizardry.content.blockentity;

import com.binaris.wizardry.setup.registries.EBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public final class SnareBlockEntity extends BlockEntity {
    private UUID caster;

    public SnareBlockEntity(BlockPos pos, BlockState state) {
        super(EBBlockEntities.SNARE.get(), pos, state);
    }

    public void setCaster(LivingEntity caster) {
        this.caster = caster == null ? null : caster.getUUID();
        setChanged();
    }

    public UUID caster() { return caster; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (caster != null) tag.putUUID("Caster", caster);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        caster = tag.hasUUID("Caster") ? tag.getUUID("Caster") : null;
    }
}
