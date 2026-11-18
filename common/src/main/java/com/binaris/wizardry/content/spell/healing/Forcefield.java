package com.binaris.wizardry.content.spell.healing;

import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.entity.construct.ForcefieldConstruct;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.ConstructSpell;
import com.binaris.wizardry.setup.registries.EBEntities;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;

public final class Forcefield extends ConstructSpell<ForcefieldConstruct> {
    public Forcefield() { super(level -> new ForcefieldConstruct(EBEntities.FORCEFIELD.get(), level), false); }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.HEALING, SpellTypes.DEFENCE, SpellAction.THRUST, 45, 15, 200)
                .add(DefaultProperties.DURATION, 600).add(DefaultProperties.EFFECT_RADIUS, 3).build();
    }
}
