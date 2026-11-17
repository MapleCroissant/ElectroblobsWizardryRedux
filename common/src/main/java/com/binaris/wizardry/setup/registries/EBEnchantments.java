package com.binaris.wizardry.setup.registries;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.DeferredObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class EBEnchantments {
    static Map<String, DeferredObject<Enchantment>> ENCHANTMENTS = new HashMap<>();

    private EBEnchantments() {
    }

    // ======= Registry =======
    public static void bootstrap(net.minecraft.data.worldgen.BootstrapContext<Enchantment> context) {
        ENCHANTMENTS.forEach(((id, enchantment) ->
                context.register(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, WizardryMainMod.location(id)), enchantment.get())));
    }

    // ======= Helpers =======
    static DeferredObject<Enchantment> enchantment(String name, Enchantment enchantment) {
        DeferredObject<Enchantment> deferredEnchant = new DeferredObject<>(() -> enchantment);
        ENCHANTMENTS.put(name, deferredEnchant);
        return deferredEnchant;
    }
}
