package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.api.content.item.IManaItem;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.util.RegistryUtils;
import com.binaris.wizardry.client.NotImplementedItems;
import com.binaris.wizardry.content.item.ScrollItem;
import com.binaris.wizardry.content.item.SpellBookItem;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.setup.registries.Spells;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getRarity", at = @At("RETURN"), cancellable = true)
    private void ebwizardry$customRarity(CallbackInfoReturnable<net.minecraft.world.item.Rarity> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.getItem() instanceof com.binaris.wizardry.api.content.item.ICustomRarityItem item) {
            cir.setReturnValue(item.getRarity(self));
        }
    }
    @Unique
    ItemStack stack = (ItemStack) (Object) this;

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "getTooltipLines", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"))
    private net.minecraft.network.chat.MutableComponent ebwizardry$manaTooltip(net.minecraft.network.chat.MutableComponent original) {
        if (stack.getItem() instanceof IManaItem
                && original.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                && contents.getKey().equals("item.durability")) {
            return Component.translatable("item.ebwizardry.wand.damage_desc",
                    stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()).withStyle(ChatFormatting.BLUE);
        }
        return original;
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    public void EBWIZARDRY$getTooltipLinesEvent(net.minecraft.world.item.Item.TooltipContext context, Player player, TooltipFlag isAdvanced, CallbackInfoReturnable<List<Component>> cir){
        if (NotImplementedItems.notImplemented(stack.getItem())) {
            cir.getReturnValue().add(Component.literal("This item is not working. This has nothing to do with you, your modpack or your modloader. I need to develop the functionality of this item. Please don't ask if this is your fault. It is not").withStyle(ChatFormatting.RED));
        }

        if (stack.getItem() instanceof SpellBookItem || stack.getItem() instanceof ScrollItem) {
            Spell spell = RegistryUtils.getSpell(stack);
            if (spell != Spells.NONE && spell.property(DefaultProperties.SENSIBLE)) {
                cir.getReturnValue().add(Component.literal("Sensible Spell (Only for testing)").withStyle(ChatFormatting.RED));
            }
        }
    }
}
