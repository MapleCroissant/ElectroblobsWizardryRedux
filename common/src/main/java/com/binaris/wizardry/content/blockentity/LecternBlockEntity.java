package com.binaris.wizardry.content.blockentity;

import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.util.RegistryUtils;
import com.binaris.wizardry.content.item.SpellBookItem;
import com.binaris.wizardry.setup.registries.EBBlockEntities;
import com.binaris.wizardry.setup.registries.Spells;
import com.binaris.wizardry.core.config.EBServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class LecternBlockEntity extends BlockEntity {

    private @Nullable ResourceLocation selected;
    public float spread;
    public float previousSpread;
    public float flip;
    public float previousFlip;
    private float flipTarget;

    public LecternBlockEntity(BlockPos pos, BlockState state) {
        super(EBBlockEntities.LECTERN.get(), pos, state);
    }

    /// Index actual spell books rather than registry namespaces so addon spell books work automatically.
    public static Map<Spell, Set<BlockPos>> index(Level level, BlockPos center) {
        var entries = new LinkedHashMap<Spell, Set<BlockPos>>();
        int radius = EBServerConfig.LECTERN_SEARCH_RADIUS.get();
        var extraShelves = EBServerConfig.LECTERN_EXTRA_BOOKSHELVES.get();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof Container shelf)) continue;
            if (!(shelf instanceof BookshelfBlockEntity)
                    && !extraShelves.contains(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()))) continue;
            for (int slot = 0; slot < shelf.getContainerSize(); slot++) {
                var stack = shelf.getItem(slot);
                if (!(stack.getItem() instanceof SpellBookItem)) continue;
                var spell = RegistryUtils.getSpell(stack);
                if (spell != Spells.NONE) entries.computeIfAbsent(spell, ignored -> new LinkedHashSet<>()).add(pos.immutable());
            }
        }
        return entries;
    }

    public @Nullable ResourceLocation selected() {
        return selected;
    }

    public boolean select(ResourceLocation id) {
        if (level == null || level.isClientSide) return false;
        if (!id.equals(Spells.NONE.getLocation()) && index(level, worldPosition).keySet().stream()
                .noneMatch(spell -> id.equals(spell.getLocation()))) return false;
        selected = id;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    public void animate(Level level) {
        previousSpread = spread;
        previousFlip = flip;
        Vec3 center = Vec3.atCenterOf(worldPosition);
        boolean open = level.getNearestPlayer(center.x, center.y, center.z, 5, false) != null;
        if (open && (spread < 0.5f || level.random.nextInt(40) == 0)) {
            flipTarget += (level.random.nextBoolean() ? 1 : -1) * (1 + level.random.nextInt(3));
        }
        spread = Math.clamp(spread + (open ? 0.1f : -0.1f), 0, 1);
        flip += Math.clamp((flipTarget - flip) * 0.4f, -0.2f, 0.2f);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        selected = ResourceLocation.tryParse(tag.getString("SelectedSpell"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (selected != null) tag.putString("SelectedSpell", selected.toString());
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
