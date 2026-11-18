package com.binaris.wizardry.content.spell.lightning;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.RaySpell;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class LightningRay extends RaySpell {
    public LightningRay() {
        aimAssist(0.6f);
    }

    @Override
    public boolean isInstantCast() {
        return false;
    }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) {
        if (!(hit.getEntity() instanceof LivingEntity target)) return true;
        if (context.world().isClientSide) {
            if (context.castingTicks() % 3 == 0) {
                var caster = context.caster();
                ParticleBuilder.create(EBParticles.LIGHTNING).entity(caster)
                        .pos(caster == null ? origin : origin.subtract(caster.position())).target(target).spawn(context.world());
            }
            for (int i = 0; i < 5; i++) ParticleBuilder.create(EBParticles.SPARK, target).spawn(context.world());
        } else if (context.castingTicks() % 10 == 0) {
            EntityUtil.attackEntityWithoutKnockback(target,
                    MagicDamageSource.causeDirectMagicDamage(context.world(), context.caster(), EBDamageSources.SHOCK),
                    context.modifiers().get(SpellModifiers.POTENCY, property(DefaultProperties.DAMAGE)));
        }
        return true;
    }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) {
        return false;
    }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) {
        if (context.world().isClientSide && context.castingTicks() % 4 == 0) {
            double length = 0.8 * context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE));
            var caster = context.caster();
            var lightning = ParticleBuilder.create(EBParticles.LIGHTNING);
            if (caster == null) lightning.pos(origin).target(origin.add(direction.scale(length)));
            else lightning.entity(caster).pos(origin.subtract(caster.position())).length(length);
            lightning.spawn(context.world());
        }
        return true;
    }

    @Override
    protected void playSound(Level world, LivingEntity entity, int ticks, int duration) {
        playSoundLoop(world, entity, ticks);
    }

    @Override
    protected void playSound(Level world, double x, double y, double z, int ticks, int duration) {
        playSoundLoop(world, x, y, z, ticks, duration);
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.APPRENTICE, Elements.LIGHTNING, SpellTypes.ATTACK, SpellAction.POINT, 5, 0, 0)
                .add(DefaultProperties.RANGE, 10f)
                .add(DefaultProperties.DAMAGE, 3f)
                .build();
    }
}
