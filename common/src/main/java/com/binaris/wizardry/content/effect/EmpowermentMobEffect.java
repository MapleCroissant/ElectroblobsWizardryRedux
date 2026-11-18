package com.binaris.wizardry.content.effect;

import com.binaris.wizardry.api.content.effect.MagicMobEffect;
import com.binaris.wizardry.api.content.event.SpellCastEvent;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.content.spell.healing.EmpoweringPresence;
import com.binaris.wizardry.core.MobEffectContext;
import com.binaris.wizardry.setup.registries.Spells;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import net.minecraft.world.effect.MobEffectCategory;

public final class EmpowermentMobEffect extends MagicMobEffect {
    public EmpowermentMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8367bd);
    }

    @Override
    public void spawnCustomParticle(net.minecraft.world.level.Level level, double x, double y, double z) {
    }

    @Override
    public void onSpellPreCast(SpellCastEvent.Pre event, MobEffectContext context) {
        if (event.getSource() == SpellCastEvent.Sources.WAND || event.getSource() == SpellCastEvent.Sources.SCROLL) return;
        apply(event, context.getEffect().getAmplifier());
    }

    public static void prepareItemCast(SpellCastEvent.Prepare event) {
        if (event.getCaster() == null) return;
        var effect = event.getCaster().getEffect(EBMobEffects.EMPOWERMENT.get());
        if (effect != null) apply(event, effect.getAmplifier());
    }

    private static void apply(SpellCastEvent event, int amplifier) {
        if (event.getSpell() == Spells.EMPOWERING_PRESENCE) return;
        float bonus = Spells.EMPOWERING_PRESENCE.property(EmpoweringPresence.POTENCY_PER_LEVEL);
        event.getModifiers().multiplyTotal(SpellModifiers.POTENCY, 1 + bonus * (amplifier + 1));
    }
}
