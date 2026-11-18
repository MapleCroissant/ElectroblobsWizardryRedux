package com.binaris.wizardry.content.spell.healing;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.entity.living.ISpellCaster;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.AreaEffectSpell;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class EmpoweringPresence extends AreaEffectSpell {
    public static final SpellProperty<Float> POTENCY_PER_LEVEL = SpellProperty.floatProperty("potency_per_level", 0.15f);

    public EmpoweringPresence() {
        targetAllies(true);
        alwaysSucceed(true);
    }

    @Override
    protected boolean affectEntity(CastContext context, Vec3 origin, LivingEntity target, int targetCount) {
        if (!context.world().isClientSide && (target instanceof Player || target instanceof ISpellCaster)) {
            int duration = (int) context.modifiers().get(SpellModifiers.DURATION, property(DefaultProperties.EFFECT_DURATION));
            int bonus = (int) ((context.modifiers().getFactor(SpellModifiers.POTENCY) - 1) / 0.4f);
            target.addEffect(new MobEffectInstance(EBMobEffects.EMPOWERMENT.get(), duration,
                    Math.max(0, property(DefaultProperties.EFFECT_STRENGTH) + bonus)));
        }
        return true;
    }

    @Override
    protected void spawnParticle(Level level, double x, double y, double z) {
        ParticleBuilder.create(EBParticles.SPARKLE).pos(x, y, z).velocity(0, 0.03, 0).time(50)
                .color(0.5f, 0.4f, 0.75f).spawn(level);
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.APPRENTICE, Elements.HEALING, SpellTypes.BUFF, SpellAction.POINT_UP, 30, 20, 60)
                .add(DefaultProperties.EFFECT_RADIUS, 5)
                .add(DefaultProperties.EFFECT_DURATION, 900)
                .add(DefaultProperties.EFFECT_STRENGTH, 0)
                .add(POTENCY_PER_LEVEL)
                .build();
    }
}
