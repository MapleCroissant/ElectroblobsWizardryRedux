package com.binaris.wizardry.mixin;

import com.binaris.wizardry.attachments.LegacyAttachmentData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class LegacyBlockDataMixin {
    @Inject(method = "loadWithComponents", at = @At("TAIL"))
    private void ebwizardry$legacyAttachments(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        LegacyAttachmentData.read((BlockEntity) (Object) this, tag, registries);
    }
}
