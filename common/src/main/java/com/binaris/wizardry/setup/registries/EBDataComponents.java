package com.binaris.wizardry.setup.registries;

import com.binaris.wizardry.WizardryMainMod;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.component.CustomData;

/** Persistent and networked replacements for the former CCA item components. */
public final class EBDataComponents {
    public static final DataComponentType<CustomData> CONJURE = register("conjure");
    public static final DataComponentType<CustomData> IMBUEMENT_ENCHANTS = register("imbuement_enchants");

    private EBDataComponents() {
    }

    private static DataComponentType<CustomData> register(String id) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, WizardryMainMod.location(id),
                DataComponentType.<CustomData>builder().persistent(CustomData.CODEC)
                        .networkSynchronized(CustomData.STREAM_CODEC).build());
    }

    public static void init() {
        // Trigger registration during the mod initializer, before registry freeze.
    }
}
