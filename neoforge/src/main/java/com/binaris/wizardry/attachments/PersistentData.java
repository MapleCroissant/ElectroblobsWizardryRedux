package com.binaris.wizardry.attachments;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Native attachment serialization contract; no dependency on Fabric or CCA. */
public interface PersistentData {
    void readFromNbt(CompoundTag tag, HolderLookup.Provider registries);
    void writeToNbt(CompoundTag tag, HolderLookup.Provider registries);
}
