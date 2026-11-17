package com.binaris.wizardry.gametest;

import com.binaris.wizardry.core.integrations.ArtifactChannel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Optional API references are invoked only after checking the corresponding mod. */
final class OptionalEquipmentChecks {
    static void curios(Player player, Item ring) {
        var inventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow();
        var slots = inventory.getCurios().get("ring");
        if (slots == null || slots.getStacks().getSlots() == 0) throw new IllegalStateException("Curios ring slots were not assigned to the player");
        inventory.setEquippedCurio("ring", 0, new ItemStack(ring));
        verify(player, ring, true);
        inventory.setEquippedCurio("ring", 0, ItemStack.EMPTY);
        verify(player, ring, false);
    }
    static void accessories(Player player, Item ring) {
        var capability = io.wispforest.accessories.api.AccessoriesCapability.get(player);
        if (capability == null || !capability.getContainers().containsKey("ring")) throw new IllegalStateException("Accessories ring slots were not assigned to the player");
        var slots = capability.getContainers().get("ring").getAccessories();
        slots.setItem(0, new ItemStack(ring));
        verify(player, ring, true);
        slots.setItem(0, ItemStack.EMPTY);
        verify(player, ring, false);
    }
    private static void verify(Player player, Item item, boolean expected) {
        boolean equipped = ArtifactChannel.isEquipped(player, item);
        boolean listed = ArtifactChannel.getEquippedArtifacts(player).stream().anyMatch(stack -> stack.is(item));
        if (equipped != expected || listed != expected) throw new IllegalStateException("Artifact equip state: expected=" + expected + ", equipped=" + equipped + ", listed=" + listed + ", bridge=" + ArtifactChannel.getIntegration().getClass().getSimpleName());
    }
}
