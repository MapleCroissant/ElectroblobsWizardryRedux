package com.binaris.wizardry.content.spell.earth;

import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.world.effect.MobEffectInstance;

public final class SixthSense extends Spell {
    public SixthSense() { soundValues(1, 1.1f, 0.2f); }

    @Override
    public boolean requiresPacket() { return false; }

    @Override
    public boolean cast(PlayerCastContext context) {
        if (!context.world().isClientSide) {
            var caster = context.caster();
            float step = EBServerConfig.RANGE_INCREASE_PER_LEVEL.get();
            int amplifier = step > 0 ? Math.clamp(Math.round((context.modifiers().getFactor(SpellModifiers.RANGE) - 1) / step), 0, 255) : 0;
            int duration = (int) context.modifiers().get(SpellModifiers.DURATION, property(DefaultProperties.EFFECT_DURATION));
            if (duration <= 0) return false;
            caster.addEffect(new MobEffectInstance(EBMobEffects.SIXTH_SENSE.get(), duration, amplifier, false, false));
            playSound(context.world(), caster, context.castingTicks(), -1);
        }
        return true;
    }

    public static float radius(int amplifier) {
        return Spells.SIXTH_SENSE.property(DefaultProperties.EFFECT_RADIUS)
                * (1 + amplifier * EBServerConfig.RANGE_INCREASE_PER_LEVEL.get());
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.APPRENTICE, Elements.EARTH, SpellTypes.BUFF, SpellAction.POINT_UP, 20, 0, 100)
                .add(DefaultProperties.EFFECT_DURATION, 400).add(DefaultProperties.EFFECT_RADIUS, 20).build();
    }
}
