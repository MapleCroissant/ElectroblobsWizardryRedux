package com.binaris.wizardry.content.spell.lightning;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.event.EBLivingHurtEvent;
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
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.Spells;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class Paralysis extends RaySpell {
    private static final SpellProperty<Float> CRITICAL_HEALTH = SpellProperty.floatProperty("critical_health", 4);
    private static final SpellProperty<Float> PLAYER_DURATION = SpellProperty.floatProperty("player_effect_duration_multiplier", 0.5f);

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) {
        if (!(hit.getEntity() instanceof LivingEntity target)) return false;
        if (context.world().isClientSide) {
            Vec3 position = context.caster() == null ? origin : origin.subtract(context.caster().position());
            ParticleBuilder.create(EBParticles.BEAM).entity(context.caster()).pos(position)
                    .color(0.2f, 0.6f, 1).target(target).spawn(context.world());
            ParticleBuilder.create(EBParticles.LIGHTNING).entity(context.caster()).pos(position)
                    .target(target).spawn(context.world());
        } else {
            if (target instanceof LightningWraith || target instanceof StormElemental) {
                if (context.caster() instanceof Player caster) {
                    caster.displayClientMessage(Component.translatable("spell.resist", target.getName(), getDescriptionFormatted()), true);
                }
            } else {
                target.hurt(MagicDamageSource.causeDirectMagicDamage(context.world(), context.caster(), EBDamageSources.SHOCK),
                        context.modifiers().get(SpellModifiers.POTENCY, property(DefaultProperties.DAMAGE)));
            }
            float duration = context.modifiers().get(SpellModifiers.DURATION, property(DefaultProperties.EFFECT_DURATION));
            if (target instanceof Player) duration *= property(PLAYER_DURATION);
            if (target.isAlive() && duration >= 1) {
                target.addEffect(new MobEffectInstance(EBMobEffects.PARALYSIS.get(), (int) duration));
            }
        }
        // In the original, an impact also plays the full-range discharge.
        return false;
    }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) {
        if (context.world().isClientSide && context.world().getBlockState(hit.getBlockPos()).isSolid()) {
            ParticleBuilder.create(EBParticles.SCORCH).pos(hit.getLocation().relative(hit.getDirection(), 0.01))
                    .face(hit.getDirection()).color(0.4f, 0.8f, 1).spawn(context.world());
        }
        return false;
    }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) {
        if (context.world().isClientSide) {
            Vec3 endpoint = origin.add(direction.scale(context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE))));
            ParticleBuilder.create(EBParticles.LIGHTNING).time(4).pos(origin).target(endpoint).scale(0.5f).spawn(context.world());
            ParticleBuilder.create(EBParticles.BEAM).color(0.2f, 0.6f, 1).time(4).pos(origin).target(endpoint).spawn(context.world());
        }
        return true;
    }

    public static void onLivingHurt(EBLivingHurtEvent event) {
        LivingEntity victim = event.getDamagedEntity();
        if (!victim.level().isClientSide && !event.isCanceled() && event.getAmount() > 0
                && victim.hasEffect(EBMobEffects.PARALYSIS.get())
                && victim.getHealth() - event.getAmount() <= Spells.PARALYSIS.property(CRITICAL_HEALTH)) {
            victim.removeEffect(EBMobEffects.PARALYSIS.get());
        }
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.LIGHTNING, SpellTypes.ALTERATION, SpellAction.POINT, 20, 10, 60)
                .add(DefaultProperties.RANGE, 10f).add(DefaultProperties.DAMAGE, 4f)
                .add(DefaultProperties.EFFECT_DURATION, 100).add(CRITICAL_HEALTH).add(PLAYER_DURATION).build();
    }
}
