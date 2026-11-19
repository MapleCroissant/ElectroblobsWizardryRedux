package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.content.spell.healing.Resurrection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void ebwizardry$endPossessionOnLogout(ServerPlayer player, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.binaris.wizardry.content.spell.necromancy.Possession.end(player);
    }

    @Inject(method = "respawn", at = @At("HEAD"))
    private void ebwizardry$releaseResurrectionItem(ServerPlayer player, boolean keepEverything,
            Entity.RemovalReason reason, CallbackInfoReturnable<ServerPlayer> cir) {
        if (!keepEverything) Resurrection.ordinaryRespawn(player);
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    private void ebwizardry$preserveUnreleasedItem(ServerPlayer player, boolean keepEverything,
            Entity.RemovalReason reason, CallbackInfoReturnable<ServerPlayer> cir) {
        Resurrection.finishOrdinaryRespawn(player, cir.getReturnValue());
    }
}
