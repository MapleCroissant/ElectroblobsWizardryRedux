package com.binaris.wizardry.core;

import com.binaris.wizardry.api.content.event.SpellCastEvent;
import com.binaris.wizardry.api.content.spell.Spell;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/// Transient state for a player's currently accepted item cast.
public interface ActiveItemCast {
    @Nullable
    Attempt ebwizardry$getItemCast();

    void ebwizardry$setItemCast(@Nullable Attempt attempt);

    record Attempt(SpellCastEvent.Source source, Spell spell, ItemStack stack) {
    }
}
