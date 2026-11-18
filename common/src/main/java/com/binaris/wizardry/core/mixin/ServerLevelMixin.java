package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.api.content.event.EBEntityJoinLevelEvent;
import com.binaris.wizardry.core.event.WizardryEventBus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void ebwizardry$prepareSlowTime(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.binaris.wizardry.content.effect.SlowTimeMobEffect.prepare((ServerLevel) (Object) this);
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$pauseEntity(Entity entity, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer)
                && com.binaris.wizardry.content.effect.SlowTimeMobEffect.blocks(entity)) ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ebwizardry$tickDispenserSpells(java.util.function.BooleanSupplier hasTime,
                                                org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        var level = (ServerLevel) (Object) this;
        com.binaris.wizardry.content.item.ScrollDispenseBehavior.tick(level);
        com.binaris.wizardry.content.data.StatueReleaseData.tick(level);
    }

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    public void EBWIZARDRY$addEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        boolean result = WizardryEventBus.fireEvent(new EBEntityJoinLevelEvent(entity, (ServerLevel) (Object) this));
        if (result) cir.setReturnValue(false);
    }
}
