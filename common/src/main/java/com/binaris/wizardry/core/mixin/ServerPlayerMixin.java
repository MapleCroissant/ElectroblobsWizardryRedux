package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.api.content.event.EBItemTossEvent;
import com.binaris.wizardry.content.spell.healing.Resurrection;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.binaris.wizardry.core.event.WizardryEventBus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void ebwizardry$tickPossession(CallbackInfo ci) {
        com.binaris.wizardry.content.spell.necromancy.Possession.tick((ServerPlayer) (Object) this);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void ebwizardry$endPossessionOnDeath(DamageSource source, CallbackInfo ci) {
        com.binaris.wizardry.content.spell.necromancy.Possession.end((ServerPlayer) (Object) this);
    }

    @Inject(method = "setGameMode", at = @At("HEAD"))
    private void ebwizardry$endPossessionOnModeChange(net.minecraft.world.level.GameType type,
            CallbackInfoReturnable<Boolean> ci) {
        com.binaris.wizardry.content.spell.necromancy.Possession.end((ServerPlayer) (Object) this);
    }

    @Inject(method = "changeDimension", at = @At("HEAD"))
    private void ebwizardry$endPossessionOnTravel(DimensionTransition transition,
            CallbackInfoReturnable<net.minecraft.world.entity.Entity> ci) {
        com.binaris.wizardry.content.spell.necromancy.Possession.end((ServerPlayer) (Object) this);
    }

    @Unique private int ebwizardry$reservedSlot = -1;
    @Unique private boolean ebwizardry$amuletAtDeath;

    @WrapOperation(method = "die", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void ebwizardry$retainResurrectionItem(ServerPlayer player, ServerLevel level, DamageSource source,
                                                   Operation<Void> original) {
        if (!Resurrection.amuletActive(player) || level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
            original.call(player, level, source);
            return;
        }
        ebwizardry$amuletAtDeath = true;
        int slot = Resurrection.findCastingSlot(player);
        if (slot < 0) {
            original.call(player, level, source);
            return;
        }
        ItemStack reserved = player.getInventory().removeItemNoUpdate(slot);
        try {
            original.call(player, level, source);
        } finally {
            if (player.getInventory().getItem(slot).isEmpty()) {
                player.getInventory().setItem(slot, reserved);
                ebwizardry$reservedSlot = slot;
            } else if (!player.getInventory().add(reserved)) {
                player.spawnAtLocation(reserved);
            }
        }
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void ebwizardry$recordDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        Resurrection.markDeath(player, ebwizardry$amuletAtDeath || Resurrection.amuletActive(player), ebwizardry$reservedSlot);
        ebwizardry$reservedSlot = -1;
        ebwizardry$amuletAtDeath = false;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void ebwizardry$saveDeath(CompoundTag tag, CallbackInfo ci) {
        Resurrection.saveDeath((ServerPlayer) (Object) this, tag);
        com.binaris.wizardry.content.spell.necromancy.Possession.saveAbilities((ServerPlayer) (Object) this, tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void ebwizardry$loadDeath(CompoundTag tag, CallbackInfo ci) {
        Resurrection.loadDeath((ServerPlayer) (Object) this, tag);
    }

    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$respawnAtDeath(boolean keepEverything, DimensionTransition.PostDimensionTransition post,
                                          CallbackInfoReturnable<DimensionTransition> cir) {
        var transition = Resurrection.transition((ServerPlayer) (Object) this, post);
        if (transition != null) cir.setReturnValue(transition);
    }


    @Inject(method = "doTick", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$pausePlayerSimulation(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (com.binaris.wizardry.content.effect.SlowTimeMobEffect.blocks((ServerPlayer) (Object) this)) ci.cancel();
    }

    @Inject(method = "drop*", at = @At("HEAD"), cancellable = true)
    public void EBWIZARDRY$drop(ItemStack itemStack, boolean bl, boolean bl2, CallbackInfoReturnable<ItemEntity> cir) {
        boolean result = WizardryEventBus.fireEvent(new EBItemTossEvent((Player) (Object) this, itemStack));
        if (result) cir.cancel();
    }
}
