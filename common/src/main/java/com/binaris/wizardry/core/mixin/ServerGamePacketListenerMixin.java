package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {

    @org.spongepowered.asm.mixin.Shadow public ServerPlayer player;

    @org.spongepowered.asm.mixin.injection.Inject(
            method = {"handleMovePlayer", "handleMoveVehicle", "handlePlayerAction", "handleUseItemOn", "handleUseItem", "handleInteract"},
            at = @At("HEAD"), cancellable = true)
    private void ebwizardry$pausePlayerInput(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        var server = player.getServer();
        if (server != null && server.isSameThread() && com.binaris.wizardry.content.effect.SlowTimeMobEffect.blocks(player)) ci.cancel();
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = {"handleUseItem", "handleUseItemOn", "handlePlayerAction",
            "handleContainerClick", "handleSetCreativeModeSlot", "handlePickItem"}, at = @At("HEAD"), cancellable = true)
    private void ebwizardry$preventPossessedInventoryUse(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (player.server.isSameThread() && com.binaris.wizardry.content.spell.necromancy.Possession.active(player)) ci.cancel();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSleeping()Z"))
    private boolean EBWIZARDRY$whitelistFlightFromFloatingKick(ServerPlayer player) {
        if (EntityUtil.isCasting(player, Spells.FLIGHT) || com.binaris.wizardry.content.effect.SlowTimeMobEffect.blocks(player)) return true;
        return player.isSleeping();
    }
}
