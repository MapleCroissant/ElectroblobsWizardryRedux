package com.binaris.wizardry.client;

import com.binaris.wizardry.setup.registries.EBItems;
import net.minecraft.world.item.Item;

import java.util.List;

public final class NotImplementedItems {
    static List<Item> ITEMS = List.of(
            EBItems.AMULET_AUTO_SHIELD.get(),
            EBItems.AMULET_ANCHORING.get(),
            EBItems.WIZARD_HANDBOOK.get(),

            EBItems.AMULET_FIRE_CLOAKING.get(),
            EBItems.AMULET_FROST_WARDING.get(),
            EBItems.AMULET_BANISHING.get(),
            EBItems.RING_SOULBINDING.get(),
            EBItems.CHARM_HUNGER_CASTING.get()
    );

    public static void init() {
    }

    public static boolean notImplemented(Item item) {
        return ITEMS.contains(item);
    }

    private NotImplementedItems() {
    }
}
