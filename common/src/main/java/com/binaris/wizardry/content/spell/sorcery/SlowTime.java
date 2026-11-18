package com.binaris.wizardry.content.spell.sorcery;

import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.content.effect.SlowTimeMobEffect;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.BuffSpell;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public final class SlowTime extends BuffSpell {
    private static final SpellProperty<Integer> DURATION = SpellProperty.intProperty("slow_time_duration", 300);
    private static final SpellProperty<Integer> STRENGTH = SpellProperty.intProperty("slow_time_strength", 0);

    public SlowTime() {
        super(0.2f, 0.8f, 0.8f, EBMobEffects.SLOW_TIME);
        soundValues(0.6f, 1.5f, 0);
    }

    @Override
    public boolean requiresPacket() { return false; }

    @Override
    public boolean canCastByEntity() { return false; }

    @Override
    public boolean canCastByLocation() { return false; }

    @Override
    protected boolean applyEffects(CastContext context, LivingEntity target) {
        if (context.world().isClientSide) return true;
        int duration = (int) context.modifiers().get(SpellModifiers.DURATION, property(DURATION));
        int strength = Math.clamp(property(STRENGTH) + getStandardBonusAmplifier(context.modifiers().getFactor(SpellModifiers.POTENCY)), 0, 16);
        if (duration <= 0 || !target.addEffect(new MobEffectInstance(EBMobEffects.SLOW_TIME.get(), duration, strength, false, false))) return false;
        SlowTimeMobEffect.observe(target);
        return true;
    }

    @Override
    protected @NotNull SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.MASTER, Elements.SORCERY, SpellTypes.ALTERATION, SpellAction.POINT_UP, 100, 15, 200)
                .add(DURATION).add(STRENGTH).add(DefaultProperties.EFFECT_RADIUS, 8).build();
    }
}
