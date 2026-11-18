package com.binaris.wizardry.content.spell.necromancy;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.effect.FearMobEffect;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.AreaEffectSpell;
import com.binaris.wizardry.content.spell.abstr.BuffSpell;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public final class Intimidate extends AreaEffectSpell {
    public Intimidate() {
        alwaysSucceed(true);
    }

    @Override
    public boolean canCastByLocation() { return false; }

    @Override
    protected boolean affectEntity(CastContext context, Vec3 origin, LivingEntity target, int index) {
        if (!(target instanceof PathfinderMob mob) || context.caster() == null) return false;
        if (!context.world().isClientSide) {
            int duration = (int) (property(DefaultProperties.EFFECT_DURATION) * context.modifiers().getFactor(SpellModifiers.DURATION));
            if (duration <= 0) return false;
            int amplifier = Math.max(0, property(DefaultProperties.EFFECT_STRENGTH)
                    + BuffSpell.getStandardBonusAmplifier(context.modifiers().getFactor(SpellModifiers.POTENCY)));
            if (mob.addEffect(new MobEffectInstance(EBMobEffects.FEAR.get(), duration, amplifier))) {
                ((FearMobEffect.SourceAccess) mob).ebwizardry$setFearSource(context.caster().getUUID());
                mob.setTarget(null);
            }
        }
        return true;
    }

    @Override
    protected void spawnParticleEffect(CastContext context, Vec3 origin, double radius) {
        var world = context.world();
        var center = context.caster() == null ? origin : context.caster().getEyePosition();
        for (int i = 0; i < 30; i++) {
            ParticleBuilder.create(EBParticles.DARK_MAGIC)
                    .pos(center.x - 1 + world.random.nextDouble() * 2, center.y - 0.25 + world.random.nextDouble() * 0.5,
                            center.z - 1 + world.random.nextDouble() * 2)
                    .color(0.9f, 0.1f, 0).spawn(world);
        }
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.APPRENTICE, Elements.NECROMANCY, SpellTypes.ALTERATION, SpellAction.SUMMON, 20, 0, 100)
                .add(DefaultProperties.EFFECT_RADIUS, 8).add(DefaultProperties.EFFECT_DURATION, 600)
                .add(DefaultProperties.EFFECT_STRENGTH, 0).build();
    }
}
