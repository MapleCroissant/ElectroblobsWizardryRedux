package com.binaris.wizardry.attachments.stack;

import com.binaris.wizardry.api.content.util.ItemData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/** Stack-bound adapter; each write installs a new immutable component value. */
abstract class StackDataHolder {
    private final ItemStack stack;
    private final DataComponentType<CustomData> type;
    private final String legacyId;

    StackDataHolder(ItemStack stack, DataComponentType<CustomData> type, String legacyId) {
        this.stack = stack;
        this.type = type;
        this.legacyId = legacyId;
    }

    protected CompoundTag read() {
        CustomData value = stack.get(type);
        if (value != null) return value.copyTag();
        // Vanilla's old-item conversion retains mod NBT inside CUSTOM_DATA.
        return ItemData.read(stack).getCompound("cardinal_components").getCompound(legacyId).copy();
    }

    protected void update(Consumer<CompoundTag> change) {
        CompoundTag data = read();
        change.accept(data);
        stack.set(type, CustomData.of(data));
    }
}
