package com.binaris.wizardry.content.blockentity;

import com.binaris.wizardry.content.data.StatueReleaseData;
import com.binaris.wizardry.setup.registries.EBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class StatueBlockEntity extends BlockEntity {
    private UUID group;
    private BlockPos base;
    private int size;
    private int duration;
    private long created;
    private float width = 1;
    private float height = 1;
    private CompoundTag mob;
    private CompoundTag display;

    public StatueBlockEntity(BlockPos pos, BlockState state) { super(EBBlockEntities.STATUE.get(), pos, state); }

    public void configure(UUID id, BlockPos base, int parts, int duration, float width, float height, @Nullable CompoundTag mob) {
        group = id;
        this.base = base;
        size = parts;
        this.duration = duration;
        created = getLevel().getGameTime();
        this.width = width;
        this.height = height;
        this.mob = mob == null ? null : mob.copy();
        display = mob == null ? null : displayCopy(mob);
        setChanged();
        getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
    }

    private static CompoundTag displayCopy(CompoundTag source) {
        var safe = new CompoundTag();
        for (var key : java.util.List.of("id", "CustomName", "Rotation", "Age", "Size", "Variant", "variant", "Color",
                "CatType", "RabbitType", "VillagerData", "ArmorItems", "HandItems", "Saddle", "Sheared")) {
            var item = source.get(key);
            if (item != null) safe.put(key, item.copy());
        }
        return safe;
    }

    public @Nullable UUID group() { return group; }
    public @Nullable CompoundTag display() { return display; }
    public float width() { return width; }
    public float height() { return height; }

    public void requestRelease(ServerLevel level) {
        if (group == null || base == null) return;
        if (getBlockPos().equals(base)) {
            if (mob != null) StatueReleaseData.enqueue(level, group, base, size, mob);
        } else if (level.getBlockEntity(base) instanceof StatueBlockEntity anchor && group.equals(anchor.group)) {
            anchor.requestRelease(level);
        }
    }

    public void clearPayload() {
        group = null;
        mob = null;
        setChanged();
    }

    public int age() { return (int) Math.min(Integer.MAX_VALUE, getLevel().getGameTime() - created); }
    public int duration() { return duration; }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.putFloat("Width", width);
        tag.putFloat("Height", height);
        if (display != null) tag.put("Display", display);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.merge(getUpdateTag(registries));
        if (group != null) tag.putUUID("Group", group);
        if (base != null) tag.putLong("Base", base.asLong());
        tag.putInt("Size", size);
        tag.putInt("Duration", duration);
        tag.putLong("Created", created);
        if (mob != null) tag.put("Mob", mob);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        width = tag.contains("Width") ? tag.getFloat("Width") : 1;
        height = tag.contains("Height") ? tag.getFloat("Height") : 1;
        display = tag.contains("Display") ? tag.getCompound("Display") : null;
        group = tag.hasUUID("Group") ? tag.getUUID("Group") : null;
        base = tag.contains("Base") ? BlockPos.of(tag.getLong("Base")) : null;
        size = tag.getInt("Size");
        duration = tag.getInt("Duration");
        created = tag.getLong("Created");
        mob = tag.contains("Mob") ? tag.getCompound("Mob") : null;
    }
}
