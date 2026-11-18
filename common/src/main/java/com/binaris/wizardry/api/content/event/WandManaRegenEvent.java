package com.binaris.wizardry.api.content.event;

import com.binaris.wizardry.api.content.event.abstr.WizardryEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/// Changes passive wand recharge on the server's condenser interval. The base amount is zero
/// for an unupgraded wand. Listeners can set an amount for the current interval.
public final class WandManaRegenEvent extends WizardryEvent {
    private final Player player;
    private final ItemStack wand;
    private int amount;

    public WandManaRegenEvent(Player player, ItemStack wand, int amount) {
        this.player = player;
        this.wand = wand;
        this.amount = amount;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getWand() {
        return wand;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
    }
}
