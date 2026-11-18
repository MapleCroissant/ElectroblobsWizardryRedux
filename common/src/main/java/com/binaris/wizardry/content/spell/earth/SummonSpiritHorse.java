package com.binaris.wizardry.content.spell.earth;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.data.StoredSpellVar;
import com.binaris.wizardry.api.content.data.VarPersistence;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.entity.living.SpiritHorse;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.setup.registries.EBEntities;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class SummonSpiritHorse extends SpiritCompanionSpell<SpiritHorse> {
    public static final StoredSpellVar<UUID> HORSE_UUID = StoredSpellVar.ofUUID("spiritHorseUUID", VarPersistence.ALWAYS);

    public SummonSpiritHorse() {
        super(HORSE_UUID, EBEntities.SPIRIT_HORSE::get);
    }

    @Override
    protected void configure(SpiritHorse horse, Player caster, float potency) {
        horse.setTamed(true);
        horse.setOwnerUUID(caster.getUUID());
        var key = WizardryMainMod.location("spirit_horse_potency");
        horse.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(
                new AttributeModifier(key, potency - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        horse.getAttribute(Attributes.JUMP_STRENGTH).addPermanentModifier(
                new AttributeModifier(key, (potency - 1) * 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.EARTH, SpellTypes.MINION, SpellAction.SUMMON, 50, 10, 150)
                .add(DefaultProperties.SUMMON_RADIUS, 2).build();
    }
}
