package com.binaris.wizardry.content.data;

import com.binaris.wizardry.content.block.StatueBlock;
import com.binaris.wizardry.content.blockentity.StatueBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class StatueReleaseData extends SavedData {
    private static final String NAME = "ebwizardry_statue_release";
    private static final Factory<StatueReleaseData> FACTORY = new Factory<>(StatueReleaseData::new,
            (tag, registries) -> load(tag), DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);
    private final Map<UUID, Release> pending = new LinkedHashMap<>();

    public static void enqueue(ServerLevel level, UUID group, BlockPos base, int parts, CompoundTag mob) {
        var data = level.getDataStorage().computeIfAbsent(FACTORY, NAME);
        if (data.pending.putIfAbsent(group, new Release(base.immutable(), parts, mob.copy())) == null) data.setDirty();
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 5 != 0) return;
        var data = level.getDataStorage().get(FACTORY, NAME);
        if (data == null || data.pending.isEmpty()) return;
        var iterator = data.pending.entrySet().iterator();
        int attempts = 0;
        while (iterator.hasNext() && attempts < 8) {
            var entry = iterator.next();
            var release = entry.getValue();
            if (!level.hasChunkAt(release.base)) continue;
            attempts++;
            if (release.release(level, entry.getKey())) {
                iterator.remove();
                data.setDirty();
            }
        }
    }

    private record Release(BlockPos base, int parts, CompoundTag mob) {
        private boolean release(ServerLevel level, UUID group) {
            for (int part = parts - 1; part >= 0; part--) {
                var pos = base.above(part);
                if (level.getBlockState(pos).getBlock() instanceof StatueBlock
                        && level.getBlockEntity(pos) instanceof StatueBlockEntity statue && group.equals(statue.group())) {
                    statue.clearPayload();
                    level.removeBlock(pos, false);
                }
            }
            if (mob.hasUUID("UUID")) {
                UUID id = mob.getUUID("UUID");
                for (var dimension : level.getServer().getAllLevels()) {
                    if (dimension.getEntity(id) != null) return true;
                }
            }
            if (!(EntityType.create(mob.copy(), level).orElse(null) instanceof Mob restored)) return false;
            restored.setDeltaMovement(Vec3.ZERO);
            for (var candidate : new BlockPos[]{base, base.above(), base.north(), base.south(), base.east(), base.west()}) {
                if (!level.hasChunkAt(candidate) || !level.getWorldBorder().isWithinBounds(candidate)) continue;
                restored.setPos(Vec3.atBottomCenterOf(candidate));
                if (level.noCollision(restored)) return level.addFreshEntity(restored);
            }
            return false;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var entries = new ListTag();
        pending.forEach((id, release) -> {
            var record = new CompoundTag();
            record.putUUID("Group", id);
            record.putLong("Base", release.base.asLong());
            record.putInt("Parts", release.parts);
            record.put("Mob", release.mob);
            entries.add(record);
        });
        tag.put("Pending", entries);
        return tag;
    }

    private static StatueReleaseData load(CompoundTag tag) {
        var data = new StatueReleaseData();
        for (Tag value : tag.getList("Pending", Tag.TAG_COMPOUND)) {
            if (value instanceof CompoundTag entry && entry.hasUUID("Group") && entry.contains("Mob", Tag.TAG_COMPOUND)) {
                data.pending.put(entry.getUUID("Group"), new Release(BlockPos.of(entry.getLong("Base")),
                        Math.clamp(entry.getInt("Parts"), 1, 3), entry.getCompound("Mob")));
            }
        }
        return data;
    }
}
