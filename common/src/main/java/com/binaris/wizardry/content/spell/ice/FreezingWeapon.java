package com.binaris.wizardry.content.spell.ice;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.sorcery.ImbueWeapon;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public final class FreezingWeapon extends ImbueWeapon {
    public static final ResourceKey<Enchantment> FREEZING = ResourceKey.create(Registries.ENCHANTMENT,
            WizardryMainMod.location("freezing_weapon"));

    @Override
    protected ResourceKey<Enchantment> enchantmentFor(ItemStack stack) {
        return isSword(stack) || isBow(stack) ? FREEZING : null;
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.ICE, SpellTypes.UTILITY, SpellAction.IMBUE, 35, 0, 70)
                .add(DefaultProperties.EFFECT_DURATION, 900).build();
    }
}
