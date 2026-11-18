package com.binaris.wizardry.content.spell.necromancy;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.event.EBLivingHurtEvent;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.RaySpell;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class MindTrick extends RaySpell {
    public MindTrick() {
        soundValues(0.7f, 1, 0.4f);
    }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) {
        if (!(hit.getEntity() instanceof LivingEntity target)) return false;
        if (context.world().isClientSide) {
            for (int i = 0; i < 10; i++) {
                ParticleBuilder.create(EBParticles.DARK_MAGIC, context.world().random,
                                target.getX(), target.getEyeY(), target.getZ(), 0.25, false)
                        .color(0.8f, 0.2f, 1).spawn(context.world());
            }
        } else {
            int duration = (int) context.modifiers().get(SpellModifiers.DURATION, property(DefaultProperties.EFFECT_DURATION));
            if (duration > 0 && target instanceof Player) {
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration));
            } else if (duration > 0 && target instanceof Mob mob) {
                mob.setTarget(null);
                mob.addEffect(new MobEffectInstance(EBMobEffects.MIND_TRICK.get(), duration));
            }
        }
        return true;
    }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) { return false; }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) { return false; }

    public static void onLivingHurt(EBLivingHurtEvent event) {
        if (!event.isCanceled() && event.getSource().getEntity() instanceof LivingEntity) {
            event.getDamagedEntity().removeEffect(EBMobEffects.MIND_TRICK.get());
        }
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.NOVICE, Elements.NECROMANCY, SpellTypes.ATTACK, SpellAction.POINT, 10, 0, 40)
                .add(DefaultProperties.RANGE, 8f).add(DefaultProperties.EFFECT_DURATION, 300).build();
    }
}
