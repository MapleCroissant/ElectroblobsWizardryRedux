package com.binaris.wizardry.api.content.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/** Resolve enchantments through the current world's reloadable registry. */
public final class EnchantmentUtil {
    private EnchantmentUtil() {
    }

    public static Holder<Enchantment> get(Level level, ResourceKey<Enchantment> key) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }

    public static int level(ResourceKey<Enchantment> key, LivingEntity entity) {
        return EnchantmentHelper.getEnchantmentLevel(get(entity.level(), key), entity);
    }

    public static int itemLevel(Level level, ResourceKey<Enchantment> key, ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(get(level, key), stack);
    }
}
