package com.binaris.wizardry.content.blockentity;

import com.binaris.wizardry.setup.registries.EBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public final class ThornsBlockEntity extends BlockEntity {
    private UUID caster;
    private long placed;
    private int duration;
    private float potency = 1;

    public ThornsBlockEntity(BlockPos pos, BlockState state) { super(EBBlockEntities.THORNS.get(), pos, state); }

    public void configure(LivingEntity caster, int duration, float potency) {
        this.caster = caster == null ? null : caster.getUUID();
        this.placed = getLevel().getGameTime();
        this.duration = duration;
        this.potency = potency;
        setChanged();
        getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
    }

    public UUID caster() { return caster; }
    public long age(long now) { return Math.max(0, now - placed); }
    public int duration() { return duration; }
    public float potency() { return potency; }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (caster != null) tag.putUUID("Caster", caster);
        tag.putLong("Placed", placed);
        tag.putInt("Duration", duration);
        tag.putFloat("Potency", potency);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        caster = tag.hasUUID("Caster") ? tag.getUUID("Caster") : null;
        placed = tag.getLong("Placed");
        duration = tag.getInt("Duration");
        potency = tag.getFloat("Potency");
    }
}
