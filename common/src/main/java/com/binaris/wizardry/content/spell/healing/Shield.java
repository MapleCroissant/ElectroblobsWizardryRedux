package com.binaris.wizardry.content.spell.healing;

import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.entity.ShieldEntity;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class Shield extends Spell {
    @Override
    public boolean isInstantCast() { return false; }

    @Override
    public boolean cast(PlayerCastContext context) {
        var caster = context.caster();
        if (!ShieldEntity.refresh(caster)) return false;
        if (!context.world().isClientSide) {
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10,
                    property(DefaultProperties.EFFECT_STRENGTH), false, false));
        }
        playSoundLoop(context.world(), caster, context.castingTicks());
        return true;
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.APPRENTICE, Elements.HEALING, SpellTypes.DEFENCE, SpellAction.BLOCK, 5, 0, 0)
                .add(DefaultProperties.EFFECT_STRENGTH, 0).build();
    }
}
