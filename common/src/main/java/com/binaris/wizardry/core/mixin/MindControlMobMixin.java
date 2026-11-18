package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.content.effect.MindControlMobEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Mob.class)
public abstract class MindControlMobMixin implements MindControlMobEffect.ControllerAccess {
    @Unique private UUID ebwizardry$controller;

    @Override
    public @Nullable UUID ebwizardry$getController() { return ebwizardry$controller; }

    @Override
    public void ebwizardry$setController(@Nullable UUID controller) { ebwizardry$controller = controller; }

    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity ebwizardry$adjustControlTarget(LivingEntity target) {
        return MindControlMobEffect.filterTarget((Mob) (Object) this, target);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void ebwizardry$saveController(CompoundTag tag, CallbackInfo ci) {
        if (ebwizardry$controller != null) tag.putUUID("WizardryController", ebwizardry$controller);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void ebwizardry$loadController(CompoundTag tag, CallbackInfo ci) {
        ebwizardry$controller = tag.hasUUID("WizardryController") ? tag.getUUID("WizardryController") : null;
    }
}
