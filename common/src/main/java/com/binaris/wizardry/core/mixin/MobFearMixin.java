package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.content.effect.FearMobEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Mob.class)
public abstract class MobFearMixin implements FearMobEffect.SourceAccess {
    @Unique private UUID ebwizardry$fearSource;

    @Override
    public UUID ebwizardry$getFearSource() { return ebwizardry$fearSource; }

    @Override
    public void ebwizardry$setFearSource(UUID source) { ebwizardry$fearSource = source; }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void ebwizardry$writeFearSource(CompoundTag tag, CallbackInfo callback) {
        if (ebwizardry$fearSource != null) tag.putUUID("WizardryFearSource", ebwizardry$fearSource);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void ebwizardry$readFearSource(CompoundTag tag, CallbackInfo callback) {
        ebwizardry$fearSource = tag.hasUUID("WizardryFearSource") ? tag.getUUID("WizardryFearSource") : null;
    }
}
