package com.binaris.wizardry.content.spell.necromancy;

import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class ShadowWard extends Spell {
    private static final SpellProperty<Float> FRACTION = SpellProperty.floatProperty("reflected_fraction", 0.5f);

    public ShadowWard() { soundValues(0.6f, 1, 0); }

    @Override
    public boolean isInstantCast() { return false; }

    @Override
    public boolean cast(PlayerCastContext context) {
        var caster = context.caster();
        var level = context.world();
        if (level.isClientSide) {
            var random = level.random;
            level.addParticle(ParticleTypes.PORTAL, caster.getX(), caster.getEyeY(), caster.getZ(),
                    random.nextDouble() * 2 - 1, random.nextDouble() - 1, random.nextDouble() * 2 - 1);
        }
        playSoundLoop(level, caster, context.castingTicks());
        return true;
    }

    public static float reflectedFraction(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide || source instanceof Reflection || source.is(DamageTypeTags.BYPASSES_SHIELD)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || !(source.getEntity() instanceof LivingEntity attacker) || attacker == victim
                || !EntityUtil.isCasting(victim, Spells.SHADOW_WARD)) return 0;
        return Math.clamp(Spells.SHADOW_WARD.property(FRACTION), 0, 1);
    }

    public static void reflect(LivingEntity victim, LivingEntity attacker, float damage) {
        var source = MagicDamageSource.causeDirectMagicDamage(victim, EBDamageSources.MAGIC);
        attacker.hurt(new Reflection(source, victim), damage);
    }

    private static final class Reflection extends MagicDamageSource {
        private Reflection(DamageSource source, LivingEntity caster) { super(source.typeHolder(), caster, caster); }
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.NECROMANCY, SpellTypes.DEFENCE, SpellAction.BLOCK, 10, 0, 0)
                .add(FRACTION).build();
    }
}
