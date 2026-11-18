package com.binaris.wizardry.content.spell.healing;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.event.EBLivingHurtEvent;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.BuffSpell;
import com.binaris.wizardry.content.spell.abstr.RaySpell;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class MarkSacrifice extends RaySpell {
    public MarkSacrifice() {
        soundValues(1, 1.1f, 0.2f);
    }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) {
        if (!context.world().isClientSide && hit.getEntity() instanceof LivingEntity target) {
            int duration = (int) (property(DefaultProperties.EFFECT_DURATION) * context.modifiers().getFactor(SpellModifiers.DURATION));
            int amplifier = property(DefaultProperties.EFFECT_STRENGTH)
                    + BuffSpell.getStandardBonusAmplifier(context.modifiers().getFactor(SpellModifiers.POTENCY));
            if (duration > 0) target.addEffect(new MobEffectInstance(EBMobEffects.MARK_OF_SACRIFICE.get(), duration, Math.max(0, amplifier)));
        }
        return true;
    }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) { return false; }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) { return true; }

    @Override
    protected void spawnParticle(CastContext context, double x, double y, double z, double vx, double vy, double vz) {
        ParticleBuilder.create(EBParticles.DARK_MAGIC).pos(x, y, z).color(0.91f, 0.055f, 0.28f).spawn(context.world());
        ParticleBuilder.create(EBParticles.SPARKLE).pos(x, y, z).color(1, 0.48f, 0.73f)
                .time(12 + context.world().random.nextInt(8)).spawn(context.world());
    }

    public static void onLivingHurt(EBLivingHurtEvent event) {
        var victim = event.getDamagedEntity();
        if (victim.level().isClientSide || event.isCanceled() || event.getAmount() <= 0 || !EBDamageSources.isMagic(event.getSource())) return;
        var mark = victim.getEffect(EBMobEffects.MARK_OF_SACRIFICE.get());
        if (mark != null) {
            event.setAmount(event.getAmount() * (1 + 0.6f * (1 + mark.getAmplifier())));
            victim.removeEffect(EBMobEffects.MARK_OF_SACRIFICE.get());
        }
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.APPRENTICE, Elements.HEALING, SpellTypes.ALTERATION, SpellAction.POINT, 25, 0, 60)
                .add(DefaultProperties.RANGE, 8f).add(DefaultProperties.EFFECT_DURATION, 200)
                .add(DefaultProperties.EFFECT_STRENGTH, 0).build();
    }
}
