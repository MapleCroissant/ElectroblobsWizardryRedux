package com.binaris.wizardry.integration.curios;

import com.binaris.wizardry.api.content.item.ArtifactItem;
import com.binaris.wizardry.core.IArtifactEffect;
import com.binaris.wizardry.core.integrations.ArtifactIntegration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CuriosArtifactImpl implements ArtifactIntegration {
    public static final CuriosArtifactImpl INSTANCE = new CuriosArtifactImpl();

    // we don't use this in the impl :p
    @Override
    public boolean isLoaded() {
        return false;
    }

    public void load(Item item) {
        if (item instanceof ICurioItem) {
            CuriosApi.registerCurio(item, (ICurioItem) item);
        }
    }

    @Override
    public Item createArtifact(Rarity rarity, IArtifactEffect effect) {
        return new CuriosArtifactItem(rarity, effect);
    }

    @Override
    public List<ItemStack> getEquippedArtifacts(Player player) {
        ArrayList<ItemStack> list = new ArrayList<>();

        CuriosApi.getCuriosInventory(player).ifPresent(itemHandler -> {
            IItemHandlerModifiable handler = itemHandler.getEquippedCurios();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (stack.getItem() instanceof ArtifactItem) list.add(stack);
            }
        });

        return list;
    }

    @Override
    public boolean isEquipped(Player player, Item item) {
        // Curios' isEquipped cache can retain an item until the next inventory tick.
        // Match the live inventory used by getEquippedArtifacts on equip/unequip.
        return getEquippedArtifacts(player).stream().anyMatch(stack -> stack.is(item));
    }
}

