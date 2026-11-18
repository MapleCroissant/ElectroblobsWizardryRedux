package com.binaris.wizardry.content.spell.lightning;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.EntityCastContext;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.entity.living.LightningWraith;
import com.binaris.wizardry.content.entity.living.StormElemental;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.AllyDesignation;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.Spells;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class Thunderstorm extends Spell {
    private static final SpellProperty<Integer> BOLTS = SpellProperty.intProperty("lightning_bolts", 10);
    private static final SpellProperty<Float> SECONDARY_DAMAGE = SpellProperty.floatProperty("secondary_damage", 10);
    private static final SpellProperty<Float> TERTIARY_DAMAGE = SpellProperty.floatProperty("tertiary_damage", 8);
    private static final SpellProperty<Float> SECONDARY_RANGE = SpellProperty.floatProperty("secondary_range", 10);
    private static final SpellProperty<Float> TERTIARY_RANGE = SpellProperty.floatProperty("tertiary_range", 10);
    private static final SpellProperty<Integer> SECONDARY_CAP = SpellProperty.intProperty("secondary_max_targets", 12);
    private static final SpellProperty<Integer> TERTIARY_CAP = SpellProperty.intProperty("tertiary_max_targets", 3);

    public Thunderstorm() {
        soundValues(1, 1.7f, 0.2f);
    }

    @Override
    public boolean requiresPacket() { return false; }

    @Override
    public boolean canCastByEntity() { return true; }

    @Override
    public boolean cast(PlayerCastContext context) { return castStorm(context); }

    @Override
    public boolean cast(EntityCastContext context) { return castStorm(context); }

    private boolean castStorm(CastContext context) {
        var caster = context.caster();
        if (caster == null || !LightningBoltSpell.isOpenToSky(context.world(), caster.blockPosition())) return false;
        if (!(context.world() instanceof ServerLevel level)) return context.world().isClientSide;
        double radius = Math.clamp(property(DefaultProperties.EFFECT_RADIUS), 0, 32);
        double blast = Math.clamp(context.modifiers().getFactor(SpellModifiers.BLAST), 0, 4);
        float potency = context.modifiers().getFactor(SpellModifiers.POTENCY);
        for (int i = 0; i < Math.clamp(property(BOLTS), 0, 32); i++) {
            double distance = radius * 0.5 * (1 + level.random.nextDouble() * blast);
            double angle = level.random.nextDouble() * Math.PI * 2;
            var x = caster.getX() + distance * Math.cos(angle);
            var z = caster.getZ() + distance * Math.sin(angle);
            var pos = BlockPos.containing(x, caster.getY(), z);
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) continue;
            Integer floor = BlockUtil.getNearestFloor(level, pos, (int) radius);
            if (floor == null || !LightningBoltSpell.isOpenToSky(level, pos.atY(floor))) continue;
            var strike = new Vec3(x, floor, z);
            if (!LightningBoltSpell.summon(level, strike, caster, Spells.LIGHTNING_BOLT.property(DefaultProperties.DAMAGE) * potency)) continue;

            var secondary = targets(level, caster, strike.add(0, 1, 0), property(SECONDARY_RANGE), property(SECONDARY_CAP), List.of());
            for (var second : secondary) {
                Vec3 middle = second.getBoundingBox().getCenter();
                var tertiary = targets(level, caster, middle, property(TERTIARY_RANGE), property(TERTIARY_CAP), secondary);
                arc(level, caster, strike, second, property(SECONDARY_DAMAGE) * potency);
                for (var third : tertiary) arc(level, caster, middle, third, property(TERTIARY_DAMAGE) * potency);
            }
        }
        return true;
    }

    private List<LivingEntity> targets(ServerLevel level, LivingEntity caster, Vec3 origin, float range, int max,
                                       List<LivingEntity> excluded) {
        if (range <= 0 || max <= 0) return List.of();
        double radius = Math.min(32, range);
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(radius),
                        target -> target.isAlive() && !excluded.contains(target) && AllyDesignation.isValidTarget(caster, target)
                                && target.distanceToSqr(origin) < radius * radius)
                .stream().sorted(Comparator.comparingDouble(target -> target.distanceToSqr(origin)))
                .limit(Math.min(max, 32)).toList();
    }

    private void arc(ServerLevel level, LivingEntity caster, Vec3 origin, LivingEntity target, float damage) {
        ParticleBuilder.create(EBParticles.LIGHTNING).pos(origin).target(target).allowServer(true).spawn(level);
        playSound(level, target, 0, -1);
        if (!(target instanceof StormElemental || target instanceof LightningWraith)) {
            target.hurt(MagicDamageSource.causeDirectMagicDamage(level, caster, EBDamageSources.SHOCK), damage);
        }
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.MASTER, Elements.LIGHTNING, SpellTypes.ATTACK, SpellAction.POINT_UP, 100, 20, 250)
                .add(DefaultProperties.EFFECT_RADIUS, 10).add(BOLTS)
                .add(SECONDARY_DAMAGE).add(TERTIARY_DAMAGE).add(SECONDARY_RANGE).add(TERTIARY_RANGE)
                .add(SECONDARY_CAP).add(TERTIARY_CAP).build();
    }
}
