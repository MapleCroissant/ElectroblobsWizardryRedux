package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.api.content.event.EBItemTossEvent;
import com.binaris.wizardry.core.ActiveItemCast;
import com.binaris.wizardry.content.item.LightningHammerItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import com.binaris.wizardry.core.event.WizardryEventBus;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.entity.player.Player.class)
public abstract class PlayerMixin implements ActiveItemCast {
    @Unique
    private Attempt ebwizardry$itemCast;

    @Override
    public @Nullable Attempt ebwizardry$getItemCast() {
        return ebwizardry$itemCast;
    }

    @Override
    public void ebwizardry$setItemCast(@Nullable Attempt attempt) {
        ebwizardry$itemCast = attempt;
    }

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$transienceInteract(Entity target, net.minecraft.world.InteractionHand hand,
            CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        var player = (Player) (Object) this;
        if (player.hasEffect(com.binaris.wizardry.setup.registries.EBMobEffects.TRANSIENCE.get())
                || com.binaris.wizardry.content.entity.construct.ForcefieldConstruct.crosses(player, target.getBoundingBox().getCenter())) {
            cir.setReturnValue(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$hammerAttack(Entity target, CallbackInfo ci) {
        var player = (Player) (Object) this;
        if (player.hasEffect(com.binaris.wizardry.setup.registries.EBMobEffects.TRANSIENCE.get())) {
            ci.cancel();
            return;
        }
        var stack = player.getMainHandItem();
        if (stack.getItem() instanceof LightningHammerItem && target instanceof LivingEntity living && target.isAttackable()) {
            LightningHammerItem.onAttack(stack, player, living);
        }
    }

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("RETURN"), cancellable = true)
    public void EBWIZARDRY$drop(ItemStack itemStack, boolean includeThrowerName, CallbackInfoReturnable<ItemEntity> cir) {
        boolean result = WizardryEventBus.fireEvent(new EBItemTossEvent((Player) (Object) this, itemStack));
        if (result) cir.cancel();
    }
}
