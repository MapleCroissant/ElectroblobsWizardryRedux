package com.binaris.wizardry.content.spell.lightning;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.RaySpell;
import com.binaris.wizardry.core.AllyDesignation;
import com.binaris.wizardry.setup.registries.*;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class LightningWeb extends RaySpell {
    private static final SpellProperty<Float> PRIMARY = SpellProperty.floatProperty("primary_damage", 5);
    private static final SpellProperty<Float> SECONDARY = SpellProperty.floatProperty("secondary_damage", 4);
    private static final SpellProperty<Float> TERTIARY = SpellProperty.floatProperty("tertiary_damage", 3);
    private static final SpellProperty<Float> SECONDARY_RANGE = SpellProperty.floatProperty("secondary_range", 5);
    private static final SpellProperty<Float> TERTIARY_RANGE = SpellProperty.floatProperty("tertiary_range", 5);
    private static final SpellProperty<Integer> SECONDARY_LIMIT = SpellProperty.intProperty("secondary_max_targets", 5);
    private static final SpellProperty<Integer> TERTIARY_LIMIT = SpellProperty.intProperty("tertiary_max_targets", 2);

    public LightningWeb() { aimAssist(0.6f); }

    @Override
    public boolean isInstantCast() { return false; }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) {
        if (!(hit.getEntity() instanceof LivingEntity primary) || context.castingTicks() % 10 != 0) return true;
        var center = primary.getBoundingBox().getCenter();
        var nearby = EntityUtil.getLivingWithinRadius(Math.clamp(property(SECONDARY_RANGE), 0, 32),
                center.x, center.y, center.z, context.world());
        arc(context, origin, primary, PRIMARY);
        nearby.stream().filter(target -> target != primary && eligible(context, target))
                .limit(Math.clamp(property(SECONDARY_LIMIT), 0, 32)).forEach(secondary -> {
                    var branch = secondary.getBoundingBox().getCenter();
                    arc(context, center, secondary, SECONDARY);
                    EntityUtil.getLivingWithinRadius(Math.clamp(property(TERTIARY_RANGE), 0, 32),
                                    branch.x, branch.y, branch.z, context.world()).stream()
                            .filter(target -> target != primary && target != secondary && !nearby.contains(target)
                                    && eligible(context, target))
                            .limit(Math.clamp(property(TERTIARY_LIMIT), 0, 32))
                            .forEach(target -> arc(context, branch, target, TERTIARY));
                });
        return true;
    }

    private boolean eligible(CastContext context, LivingEntity target) {
        return target.isAlive() && AllyDesignation.isValidTarget(context.caster(), target);
    }

    private void arc(CastContext context, Vec3 origin, LivingEntity target, SpellProperty<Float> damage) {
        if (context.world().isClientSide) {
            ParticleBuilder.create(EBParticles.BEAM).pos(origin).target(target).color(0.2f, 0.6f, 1).spawn(context.world());
            if (context.castingTicks() % 3 == 0) ParticleBuilder.create(EBParticles.LIGHTNING).pos(origin).target(target).spawn(context.world());
            for (int i = 0; i < 5; i++) ParticleBuilder.create(EBParticles.SPARK, target).spawn(context.world());
        } else {
            EntityUtil.attackEntityWithoutKnockback(target,
                    MagicDamageSource.causeDirectMagicDamage(context.world(), context.caster(), EBDamageSources.SHOCK),
                    context.modifiers().get(SpellModifiers.POTENCY, property(damage)));
        }
    }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) { return false; }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) {
        if (context.world().isClientSide) {
            double length = 0.8 * context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE));
            var end = origin.add(direction.scale(length));
            ParticleBuilder.create(EBParticles.BEAM).pos(origin).target(end).color(0.2f, 0.6f, 1).spawn(context.world());
            if (context.castingTicks() % 4 == 0) ParticleBuilder.create(EBParticles.LIGHTNING).pos(origin).target(end).spawn(context.world());
        }
        return true;
    }

    @Override
    protected void playSound(Level level, LivingEntity entity, int ticks, int duration) { playSoundLoop(level, entity, ticks); }

    @Override
    protected void playSound(Level level, double x, double y, double z, int ticks, int duration) { playSoundLoop(level, x, y, z, ticks, duration); }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.MASTER, Elements.LIGHTNING, SpellTypes.ATTACK, SpellAction.POINT, 15, 15, 70)
                .add(DefaultProperties.RANGE, 10f).add(PRIMARY).add(SECONDARY).add(TERTIARY)
                .add(SECONDARY_RANGE).add(TERTIARY_RANGE).add(SECONDARY_LIMIT).add(TERTIARY_LIMIT).build();
    }
}
