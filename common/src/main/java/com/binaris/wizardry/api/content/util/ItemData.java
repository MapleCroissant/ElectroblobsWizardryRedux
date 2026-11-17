package com.binaris.wizardry.api.content.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * Access to mod-owned item data, retaining the original NBT keys inside vanilla's
 * custom-data component. Reads are snapshots; writes must replace the component
 * so copying, equality, inventory synchronization, and saving see the change.
 */
public final class ItemData {
    private ItemData() {
    }

    /** Custom block/entity fields are outside vanilla's inventory data-fixer schema. */
    public static ItemStack load(net.minecraft.core.HolderLookup.Provider registries, CompoundTag tag) {
        if (tag.contains("Count", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            tag = (CompoundTag) net.minecraft.util.datafix.DataFixers.getDataFixer().update(
                    net.minecraft.util.datafix.fixes.References.ITEM_STACK,
                    new com.mojang.serialization.Dynamic<>(net.minecraft.nbt.NbtOps.INSTANCE, tag.copy()),
                    3465, net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
        }
        return ItemStack.parseOptional(registries, tag);
    }

    public static CompoundTag read(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> update) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, update);
    }

    public static void set(ItemStack stack, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }
}
