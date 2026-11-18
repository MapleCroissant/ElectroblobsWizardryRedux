package com.binaris.wizardry.content.spell.earth;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.data.StoredSpellVar;
import com.binaris.wizardry.api.content.data.VarPersistence;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.entity.living.SpiritWolf;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.setup.registries.EBEntities;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public final class SummonSpiritWolf extends SpiritCompanionSpell<SpiritWolf> {
    public static final StoredSpellVar<UUID> WOLF_UUID = StoredSpellVar.ofUUID("spiritWolfUUID", VarPersistence.ALWAYS);

    public SummonSpiritWolf() {
        super(WOLF_UUID, EBEntities.SPIRIT_WOLF::get);
    }

    @Override
    protected void configure(SpiritWolf wolf, net.minecraft.world.entity.player.Player caster, float potency) {
        wolf.setTame(true, true);
        wolf.setOwnerUUID(caster.getUUID());
        var key = WizardryMainMod.location("spirit_wolf_potency");
        wolf.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(
                new AttributeModifier(key, potency - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        wolf.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(
                new AttributeModifier(key, (potency - 1) * 1.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.APPRENTICE, Elements.EARTH, SpellTypes.MINION, SpellAction.SUMMON, 25, 0, 100)
                .add(DefaultProperties.SUMMON_RADIUS, 2).build();
    }
}
