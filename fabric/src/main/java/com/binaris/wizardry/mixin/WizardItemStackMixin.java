package com.binaris.wizardry.mixin;

import com.binaris.wizardry.api.content.item.ICustomDamageItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class WizardItemStackMixin {
    @Unique
    ItemStack stack = (ItemStack) (Object) this;

    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    public void EBWIZARDRY$itemGetMaxDamage(CallbackInfoReturnable<Integer> cir) {
        if (!(stack.getItem() instanceof ICustomDamageItem wizardryItem)) return;
        int maxDamage = wizardryItem.getCustomMaxDamage(stack);
        cir.setReturnValue(maxDamage);
    }

    @Inject(method = "setDamageValue", at = @At("TAIL"), cancellable = true)
    public void EBWIZARDRY$itemGetDamage(int damage, CallbackInfo ci) {
        if (!(stack.getItem() instanceof ICustomDamageItem wizardryItem)) return;
        wizardryItem.setCustomDamage(stack, damage);
        ci.cancel();
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V", at = @At("HEAD"), cancellable = true)
    public void EBWIZARDRY$itemHurtBreak(int amount, net.minecraft.server.level.ServerLevel level,
                                       ServerPlayer user, java.util.function.Consumer<net.minecraft.world.item.Item> onBreak, CallbackInfo ci) {
        if (!(stack.getItem() instanceof ICustomDamageItem customDamageItem)) return;
        if (customDamageItem.canBreak(stack)) return;
        if (!stack.isDamageableItem() || (user != null && user.hasInfiniteMaterials())) {
            ci.cancel();
            return;
        }
        var conjure = com.binaris.wizardry.core.platform.Services.OBJECT_DATA.getConjureData(stack);
        if (conjure == null || !conjure.isSummoned()) {
            int damage = amount > 0 ? net.minecraft.world.item.enchantment.EnchantmentHelper.processDurabilityChange(level, stack, amount) : amount;
            if (user != null && damage != 0) {
                net.minecraft.advancements.CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(user, stack, stack.getDamageValue() + damage);
            }
            stack.setDamageValue(stack.getDamageValue() + damage);
        }
        ci.cancel();
    }
}
