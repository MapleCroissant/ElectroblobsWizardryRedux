package com.binaris.wizardry.attachments.stack;

import com.binaris.wizardry.api.content.data.ImbuementEnchantData;
import com.binaris.wizardry.setup.registries.EBDataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.Map;

public class ImbuementEnchantDataHolder extends StackDataHolder implements ImbuementEnchantData {
    public ImbuementEnchantDataHolder(ItemStack stack) {
        super(stack, EBDataComponents.IMBUEMENT_ENCHANTS, "ebwizardry:imbuement_enchants");
    }

    @Override
    public void addImbuement(ResourceKey<Enchantment> enchant, long expireTime) {
        update(data -> data.putLong(enchant.location().toString(), expireTime));
    }

    @Override
    public Map<ResourceLocation, Long> getImbuements() {
        Map<ResourceLocation, Long> result = new HashMap<>();

        // If there is no root tag, this means there are no temporary enchantments, so we can return an empty map
        var data = read();
        data.getAllKeys().forEach(key -> {
            try {
                ResourceLocation enchantId = ResourceLocation.tryParse(key);
                long expireTime = data.getLong(key);
                if (enchantId != null) result.put(enchantId, expireTime);
            } catch (Exception e) {
                // Ignore invalid keys
            }
        });

        return result;
    }

    @Override
    public void removeImbuement(ResourceKey<Enchantment> enchant) {
        update(data -> data.remove(enchant.location().toString()));
    }

    @Override
    public boolean isImbuement(ResourceKey<Enchantment> enchant) {
        return read().contains(enchant.location().toString());
    }

    @Override
    public long getExpirationTime(ResourceKey<Enchantment> enchantment) {
        var data = read();
        String id = enchantment.location().toString();
        if (data.contains(id)) return data.getLong(id);
        return -1;
    }
}
