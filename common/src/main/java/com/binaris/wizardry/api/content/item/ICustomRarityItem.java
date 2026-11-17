package com.binaris.wizardry.api.content.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** Stack-sensitive rarity, bridged to ItemStack by the common item mixin. */
public interface ICustomRarityItem {
    Rarity getRarity(ItemStack stack);
}
