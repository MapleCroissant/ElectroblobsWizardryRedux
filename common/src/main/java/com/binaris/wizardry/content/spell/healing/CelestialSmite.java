package com.binaris.wizardry.content.spell.healing;

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
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CelestialSmite extends RaySpell {
    private static final SpellProperty<Integer> BURN_DURATION = SpellProperty.intProperty("burn_duration", 10);

    public CelestialSmite() {
        ignoreLivingEntities(true);
    }

    @Override
    protected boolean shootSpell(CastContext context, Vec3 origin, Vec3 direction) {
        double range = context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE));
        var hit = context.world().clip(new ClipContext(origin, origin.add(direction.scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context.caster()));
        return hit.getType() == HitResult.Type.BLOCK && onBlockHit(context, hit, origin);
    }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) { return false; }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) { return false; }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) {
        var level = context.world();
        var point = hit.getLocation();
        if (level.isClientSide) {
            ParticleBuilder.create(EBParticles.BEAM).pos(point.x, level.getMaxBuildHeight(), point.z)
                    .target(point).scale(8).color(1, 0.75f, 0).time(10).spawn(level);
            ParticleBuilder.create(EBParticles.SPHERE).pos(point).scale(4).color(1, 0.94f, 0.6f).spawn(level);
            if (hit.getDirection() == Direction.UP) {
                ParticleBuilder.create(EBParticles.SCORCH).pos(point.add(0, 0.01, 0))
                        .face(Direction.UP).scale(3).spawn(level);
            }
        } else {
            double radius = context.modifiers().get(SpellModifiers.BLAST, property(DefaultProperties.EFFECT_RADIUS));
            float damage = context.modifiers().get(SpellModifiers.POTENCY, property(DefaultProperties.DAMAGE));
            var source = context.caster() == null ? level.damageSources().magic()
                    : MagicDamageSource.causeDirectMagicDamage(level, context.caster(), EBDamageSources.RADIANT);
            for (var target : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius),
                    entity -> entity.isAlive() && entity.distanceToSqr(point) <= radius * radius)) {
                if (EntityUtil.attackEntityWithoutKnockback(target, source, damage)) {
                    target.igniteForSeconds(property(BURN_DURATION));
                }
            }
        }
        return true;
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.MASTER, Elements.HEALING, SpellTypes.ATTACK, SpellAction.POINT, 100, 25, 200)
                .add(DefaultProperties.RANGE, 40f).add(DefaultProperties.EFFECT_RADIUS, 3)
                .add(DefaultProperties.DAMAGE, 8f).add(BURN_DURATION).build();
    }
}
