package com.binaris.wizardry.content.spell.lightning;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.entity.living.LightningWraith;
import com.binaris.wizardry.content.entity.living.StormElemental;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.RaySpell;
import com.binaris.wizardry.core.AllyDesignation;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class ChainLightning extends RaySpell {
    private static final SpellProperty<Float> PRIMARY_DAMAGE = SpellProperty.floatProperty("primary_damage", 10);
    private static final SpellProperty<Float> SECONDARY_DAMAGE = SpellProperty.floatProperty("secondary_damage", 8);
    private static final SpellProperty<Float> TERTIARY_DAMAGE = SpellProperty.floatProperty("tertiary_damage", 6);
    private static final SpellProperty<Float> SECONDARY_RANGE = SpellProperty.floatProperty("secondary_range", 5);
    private static final SpellProperty<Float> TERTIARY_RANGE = SpellProperty.floatProperty("tertiary_range", 5);
    private static final SpellProperty<Integer> SECONDARY_TARGETS = SpellProperty.intProperty("secondary_max_targets", 5);
    private static final SpellProperty<Integer> TERTIARY_TARGETS = SpellProperty.intProperty("tertiary_max_targets", 2);

    public ChainLightning() {
        aimAssist(0.6f);
        soundValues(1, 1.7f, 0.2f);
    }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) {
        if (!(hit.getEntity() instanceof LivingEntity primary) || !primary.isAlive()) return false;
        float potency = context.modifiers().getFactor(SpellModifiers.POTENCY);
        Vec3 primaryCenter = primary.getBoundingBox().getCenter();
        var secondary = targets(context, primaryCenter, property(SECONDARY_RANGE), property(SECONDARY_TARGETS), primary, List.of());
        strike(context, origin, primary, property(PRIMARY_DAMAGE) * potency);
        for (LivingEntity second : secondary) {
            Vec3 secondCenter = second.getBoundingBox().getCenter();
            var tertiary = targets(context, secondCenter, property(TERTIARY_RANGE), property(TERTIARY_TARGETS), primary, secondary);
            strike(context, primaryCenter, second, property(SECONDARY_DAMAGE) * potency);
            for (LivingEntity third : tertiary) strike(context, secondCenter, third, property(TERTIARY_DAMAGE) * potency);
        }
        return true;
    }

    private List<LivingEntity> targets(CastContext context, Vec3 origin, float range, int cap,
                                       LivingEntity primary, List<LivingEntity> excluded) {
        if (range <= 0 || cap <= 0) return List.of();
        return context.world().getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(range),
                        entity -> entity != primary && !excluded.contains(entity) && entity.isAlive()
                                && entity.getBoundingBox().getCenter().distanceToSqr(origin) <= range * range
                                && AllyDesignation.isValidTarget(context.caster(), entity))
                .stream().sorted(Comparator.comparingDouble(entity -> entity.getBoundingBox().getCenter().distanceToSqr(origin)))
                .limit(cap).toList();
    }

    private void strike(CastContext context, Vec3 origin, LivingEntity target, float damage) {
        var level = context.world();
        if (level.isClientSide) {
            ParticleBuilder.create(EBParticles.LIGHTNING).pos(origin).target(target).spawn(level);
            ParticleBuilder.spawnShockParticles(level, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ());
        } else if (target instanceof StormElemental || target instanceof LightningWraith) {
            if (context.caster() instanceof Player player) {
                player.displayClientMessage(Component.translatable("spell.resist", target.getName(), getDescriptionFormatted()), true);
            }
        } else {
            target.hurt(MagicDamageSource.causeDirectMagicDamage(level, context.caster(), EBDamageSources.SHOCK), damage);
        }
    }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) { return false; }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) { return false; }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.LIGHTNING, SpellTypes.ATTACK, SpellAction.POINT, 25, 5, 50)
                .add(DefaultProperties.RANGE, 10f)
                .add(PRIMARY_DAMAGE).add(SECONDARY_DAMAGE).add(TERTIARY_DAMAGE)
                .add(SECONDARY_RANGE).add(TERTIARY_RANGE).add(SECONDARY_TARGETS).add(TERTIARY_TARGETS).build();
    }
}
