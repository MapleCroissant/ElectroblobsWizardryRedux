package com.binaris.wizardry.core.mixin.client;

import com.binaris.wizardry.content.effect.SlowTimeMobEffect;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class SlowTimeClientLevelMixin {
    @Inject(method = "tickEntities", at = @At("HEAD"))
    private void ebwizardry$prepareSlowTime(CallbackInfo ci) {
        SlowTimeMobEffect.prepare((ClientLevel) (Object) this);
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$applySlowTime(Entity entity, CallbackInfo ci) {
        boolean blocked = entity instanceof LocalPlayer ? SlowTimeMobEffect.blocksLocalPlayer(entity.level())
                : SlowTimeMobEffect.blocks(entity);
        if (blocked) ci.cancel();
    }
}
