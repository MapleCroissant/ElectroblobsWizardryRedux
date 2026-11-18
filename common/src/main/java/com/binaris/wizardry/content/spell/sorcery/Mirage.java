package com.binaris.wizardry.content.spell.sorcery;

import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.content.spell.abstr.BuffSpell;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public final class Mirage extends BuffSpell {
    private static final SpellProperty<Integer> DURATION = SpellProperty.intProperty("mirage_duration", 400);
    private static final SpellProperty<Integer> STRENGTH = SpellProperty.intProperty("mirage_strength", 0);

    public Mirage() { super(0.64f, 0.47f, 0.9f, EBMobEffects.MIRAGE); }

    @Override
    protected boolean applyEffects(CastContext context, LivingEntity target) {
        if (context.world().isClientSide) return true;
        int duration = (int) context.modifiers().get(SpellModifiers.DURATION, property(DURATION));
        int strength = Math.clamp(property(STRENGTH) + getStandardBonusAmplifier(context.modifiers().getFactor(SpellModifiers.POTENCY)), 0, 255);
        return duration > 0 && target.addEffect(new MobEffectInstance(EBMobEffects.MIRAGE.get(), duration, strength, false, false));
    }

    @Override
    protected @NotNull SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.SORCERY, SpellTypes.ALTERATION, SpellAction.POINT_UP, 40, 10, 200)
                .add(DURATION).add(STRENGTH).build();
    }
}
