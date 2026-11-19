package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.api.content.event.EBClientTickEvent;
import com.binaris.wizardry.core.event.WizardryEventBus;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$possessedBlockAttack(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !com.binaris.wizardry.content.spell.necromancy.Possession.active(minecraft.player)
                || !(minecraft.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)
                || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        com.binaris.wizardry.core.platform.Services.NETWORK_HELPER.sendToServer(
                new com.binaris.wizardry.core.networking.c2s.ControlInputPacketC2S(
                        com.binaris.wizardry.core.networking.c2s.ControlInputPacketC2S.ControlType.POSSESSION_TAKE_BLOCK));
        ci.setReturnValue(false);
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$possessedUse(CallbackInfo ci) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null || !com.binaris.wizardry.content.spell.necromancy.Possession.active(player)) return;
        com.binaris.wizardry.core.platform.Services.NETWORK_HELPER.sendToServer(
                new com.binaris.wizardry.core.networking.c2s.ControlInputPacketC2S(
                        com.binaris.wizardry.core.networking.c2s.ControlInputPacketC2S.ControlType.POSSESSION_ABILITY));
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void EBWIZARDRY$clientTick(CallbackInfo ci) {
        Minecraft minecraft = ((Minecraft) (Object) this);
        WizardryEventBus.fireEvent(new EBClientTickEvent(minecraft));
    }
}
