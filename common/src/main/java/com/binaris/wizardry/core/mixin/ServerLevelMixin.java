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
    @Inject(method = "tick", at = @At("TAIL"))
    private void ebwizardry$tickDispenserSpells(java.util.function.BooleanSupplier hasTime,
                                                org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.binaris.wizardry.content.item.ScrollDispenseBehavior.tick((ServerLevel) (Object) this);
    }

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    public void EBWIZARDRY$addEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        boolean result = WizardryEventBus.fireEvent(new EBEntityJoinLevelEvent(entity, (ServerLevel) (Object) this));
        if (result) cir.setReturnValue(false);
    }
}
