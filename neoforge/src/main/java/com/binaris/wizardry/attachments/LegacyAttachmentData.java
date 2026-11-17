package com.binaris.wizardry.attachments;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/** Imports retained Fabric/Forge fields once; modern attachments take precedence. */
public final class LegacyAttachmentData {
    public static void read(IAttachmentHolder holder, CompoundTag root, HolderLookup.Provider registries) {
        var cardinal = root.getCompound("cardinal_components");
        var forge = root.getCompound("ForgeCaps");
        var modern = root.getCompound("neoforge:attachments");
        for (var key : EBComponents.ALL) {
            if (holder instanceof Player && (key == EBComponents.WIZARD_DATA || key == EBComponents.CAST_COMMAND_DATA || key == EBComponents.SPELL_MANAGER_DATA)
                    || holder instanceof Mob && key == EBComponents.MINION_DATA
                    || holder instanceof LivingEntity && key == EBComponents.CONTAINMENT_DATA
                    || holder instanceof BaseContainerBlockEntity && key == EBComponents.ARCANE_LOCK_DATA) {
                String id = "ebwizardry:" + key.id();
                if (modern.contains(id)) continue;
                String forgeId = key == EBComponents.ARCANE_LOCK_DATA ? "ebwizardry:arcane_lock_data" : id;
                if (cardinal.contains(id)) key.get(holder).readFromNbt(cardinal.getCompound(id), registries);
                else if (forge.contains(forgeId)) key.get(holder).readFromNbt(forge.getCompound(forgeId), registries);
            }
        }
    }
}
