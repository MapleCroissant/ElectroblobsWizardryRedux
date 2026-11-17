package com.binaris.wizardry.mixin;

import com.binaris.wizardry.attachments.LegacyAttachmentData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class LegacyEntityDataMixin {
    @Inject(method = "load", at = @At("TAIL"))
    private void ebwizardry$legacyAttachments(CompoundTag tag, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        LegacyAttachmentData.read(entity, tag, entity.registryAccess());
    }
}
