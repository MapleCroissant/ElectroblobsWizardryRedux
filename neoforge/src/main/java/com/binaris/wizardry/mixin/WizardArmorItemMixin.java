package com.binaris.wizardry.mixin;

import com.binaris.wizardry.content.item.armor.WizardArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Empty wizard armor must stop providing protection as soon as its mana runs out. */
@Mixin(ItemStack.class)
public abstract class WizardArmorItemMixin {
    @Inject(method = {
            "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
            "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V"
    }, at = @At("HEAD"), cancellable = true)
    private void ebwizardry$manaAttributes(CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof WizardArmorItem armor && armor.getMana(stack) == 0) ci.cancel();
    }
}