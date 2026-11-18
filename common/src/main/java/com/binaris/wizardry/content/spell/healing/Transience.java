package com.binaris.wizardry.content.spell.healing;

import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class Transience extends Spell {
    @Override
    public boolean requiresPacket() { return false; }

    @Override
    public boolean cast(PlayerCastContext context) {
        var caster = context.caster();
        if (caster.hasEffect(EBMobEffects.TRANSIENCE.get())) return false;
        int duration = (int) context.modifiers().get(SpellModifiers.DURATION, property(DefaultProperties.EFFECT_DURATION));
        if (duration <= 0) return false;
        if (!context.world().isClientSide) {
            apply(caster, duration);
            playSound(context.world(), caster, context.castingTicks(), duration);
        }
        return true;
    }

    public static void apply(LivingEntity user, int duration) {
        user.addEffect(new MobEffectInstance(EBMobEffects.TRANSIENCE.get(), duration));
        user.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false));
    }

    public static boolean blocksDamage(LivingEntity target, DamageSource source) {
        if (target.hasEffect(EBMobEffects.TRANSIENCE.get()) && !source.is(DamageTypes.FELL_OUT_OF_WORLD)) return true;
        var attacker = source.getEntity() == null ? source.getDirectEntity() : source.getEntity();
        return attacker instanceof LivingEntity living && living.hasEffect(EBMobEffects.TRANSIENCE.get());
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.HEALING, SpellTypes.BUFF, SpellAction.POINT_UP, 50, 15, 100)
                .add(DefaultProperties.EFFECT_DURATION, 400).build();
    }
}
