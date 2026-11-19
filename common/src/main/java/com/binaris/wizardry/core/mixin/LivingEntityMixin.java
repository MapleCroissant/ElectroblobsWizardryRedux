package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.api.content.event.EBLivingDeathEvent;
import com.binaris.wizardry.api.content.event.EBLivingHurtEvent;
import com.binaris.wizardry.api.content.event.EBLivingTick;
import com.binaris.wizardry.api.content.effect.MagicMobEffect;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.content.effect.FrostStepEffect;
import com.binaris.wizardry.content.spell.fire.Disintegration;
import com.binaris.wizardry.content.spell.necromancy.ShadowWard;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.binaris.wizardry.core.event.WizardryEventBus;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.core.networking.MagicEffectSync;
import com.binaris.wizardry.core.networking.s2c.MagicEffectSyncS2C;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements Disintegration.Animation {
    @Unique
    private int ebwizardry$disintegrationStart = -1;

    @Override
    public int ebwizardry$disintegrationFrame() {
        return ebwizardry$disintegrationStart < 0 ? -1 : livingEntity.tickCount - ebwizardry$disintegrationStart;
    }

    @Override
    public boolean ebwizardry$startDisintegration() {
        if (ebwizardry$disintegrationStart >= 0) return false;
        ebwizardry$disintegrationStart = livingEntity.tickCount;
        return true;
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void ebwizardry$animateDisintegration(byte event, CallbackInfo ci) {
        if (event == Disintegration.ANIMATION_EVENT) ebwizardry$startDisintegration();
    }
    @Unique
    LivingEntity livingEntity = (LivingEntity) (Object) this;

    @Unique
    boolean eventCanceled = false;

    @Inject(at = @At("HEAD"), method = "tick")
    public void EBWIZARDRY$tick(CallbackInfo ci) {
        WizardryEventBus.fireEvent(new EBLivingTick(livingEntity, livingEntity.level()));
    }

    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    public void EBWIZARDRY$livingCanBeAffected(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (!(livingEntity instanceof Player player)) return;

        if (ArtifactChannel.isEquipped(player, EBItems.AMULET_ICE_IMMUNITY.get()))
            if (effect.getEffect() == EBMobEffects.FROST.get()) cir.setReturnValue(false);

        if (ArtifactChannel.isEquipped(player, EBItems.AMULET_WITHER_IMMUNITY.get()))
            if (effect.getEffect() == MobEffects.WITHER) cir.setReturnValue(false);
    }

    @Inject(method = "doPush", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$ignorePossessionPartnerPush(Entity other, CallbackInfo ci) {
        if (livingEntity instanceof Player player && com.binaris.wizardry.content.spell.necromancy.Possession.controls(player, other)
                || other instanceof Player owner && com.binaris.wizardry.content.spell.necromancy.Possession.controls(owner, livingEntity)) {
            ci.cancel();
        }
    }

    @WrapMethod(method = "hurt")
    private boolean ebwizardry$shadowWard(DamageSource source, float amount, Operation<Boolean> original) {
        if (!livingEntity.level().isClientSide && com.binaris.wizardry.content.spell.healing.Transience.blocksDamage(livingEntity, source)){
            return false;
        }
        if (!livingEntity.level().isClientSide
                && com.binaris.wizardry.content.entity.construct.ForcefieldConstruct.blocksDamage(livingEntity, source)) return false;
        if (!livingEntity.level().isClientSide && livingEntity instanceof Player player
                && com.binaris.wizardry.content.spell.necromancy.Possession.divertDamage(player, source, amount)) return false;
        float fraction = amount > 0 ? ShadowWard.reflectedFraction(livingEntity, source) : 0;
        if (fraction == 0 || livingEntity.isInvulnerableTo(source) || !livingEntity.isAlive()) {
            float healthBefore = livingEntity.getHealth();
            boolean applied = original.call(source, amount);
            if (applied && !livingEntity.level().isClientSide && livingEntity instanceof Mob host) {
                com.binaris.wizardry.content.spell.necromancy.Possession.hostDamage(host, healthBefore);
            }
            return applied;
        }
        float remaining = amount * (1 - fraction);
        boolean applied = remaining > 0 && original.call(livingEntity.damageSources().magic(), remaining);
        if (remaining == 0 || applied) {
            ShadowWard.reflect(livingEntity, (LivingEntity) source.getEntity(), amount * fraction);
        }
        return applied;
    }

    @Inject(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;setSpeed(F)V", shift = At.Shift.AFTER), cancellable = true)
    public void EBWIZARDRY$livingEntityHurtCancel(DamageSource source, float f, CallbackInfoReturnable<Boolean> cir) {
        if (eventCanceled) {
            cir.setReturnValue(false);
            eventCanceled = false;
        }
    }

    @ModifyVariable(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;setSpeed(F)V", shift = At.Shift.BEFORE), argsOnly = true, ordinal = 0)
    private float EBWIZARDRY$livingEntityHurtAmount(float amount, DamageSource source) {
        EBLivingHurtEvent event = new EBLivingHurtEvent(livingEntity, source, amount);
        WizardryEventBus.fireEvent(event);
        eventCanceled = event.isCanceled();
        return event.getAmount();
    }


    @Inject(at = @At("HEAD"), method = "jumpFromGround")
    public void EBWIZARDRY$LivingEntityJump(CallbackInfo ci) {
        if (livingEntity.hasEffect(EBMobEffects.FROST.get())) {
            if (livingEntity.getEffect(EBMobEffects.FROST.get()).getAmplifier() == 0) {
                livingEntity.setDeltaMovement(livingEntity.getDeltaMovement().x, 0.5, livingEntity.getDeltaMovement().z);
            } else {
                livingEntity.setDeltaMovement(livingEntity.getDeltaMovement().x, 0, livingEntity.getDeltaMovement().y);
            }
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    public void EBWIZARDRY$LivingEntityDie(DamageSource damageSource, CallbackInfo ci) {
        WizardryEventBus.fireEvent(new EBLivingDeathEvent(livingEntity, damageSource));
    }

    @Inject(method = "shouldDropLoot", at = @At(value = "RETURN"), cancellable = true)
    public void EBWIZARDRY$dropLoot(CallbackInfoReturnable<Boolean> cir) {
        if (livingEntity instanceof Mob mob) {
            if (Services.OBJECT_DATA.isMinion(mob)) cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldDropExperience", at = @At(value = "RETURN"), cancellable = true)
    public void EBWIZARDRY$dropExperience(CallbackInfoReturnable<Boolean> cir) {
        if (livingEntity instanceof Mob mob) {
            if (Services.OBJECT_DATA.isMinion(mob)) cir.setReturnValue(false);
        }
    }

    @Inject(method = "onEffectAdded", at = @At("TAIL"))
    private void EBWIZARDRY$syncMagicEffectAdded(MobEffectInstance instance, Entity source, CallbackInfo ci) {
        if (instance.getEffect() == EBMobEffects.SLOW_TIME.get()) com.binaris.wizardry.content.effect.SlowTimeMobEffect.observe(livingEntity);
        // This already includes the needed checks in the method
        MagicEffectSync.sendToTracking(livingEntity, instance, MagicEffectSyncS2C.Action.ADD);
    }

    @Inject(method = "onEffectUpdated", at = @At("TAIL"))
    private void EBWIZARDRY$syncMagicEffectUpdated(MobEffectInstance instance, boolean same, Entity source, CallbackInfo ci) {
        // This already includes the needed checks in the method
        MagicEffectSync.sendToTracking(livingEntity, instance, MagicEffectSyncS2C.Action.ADD);
    }

    @Inject(method = "onEffectRemoved", at = @At("HEAD"))
    private void ebwizardry$clearMindControl(MobEffectInstance effect, CallbackInfo ci) {
        if (!livingEntity.level().isClientSide && livingEntity instanceof Mob mob
                && effect.getEffect() == EBMobEffects.MIND_CONTROL.get()) {
            com.binaris.wizardry.content.effect.MindControlMobEffect.clear(mob);
        }
    }

    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void EBWIZARDRY$syncMagicEffectRemoved(MobEffectInstance instance, CallbackInfo ci) {
        // This already includes the needed checks in the method
        MagicEffectSync.sendToTracking(livingEntity, instance, MagicEffectSyncS2C.Action.REMOVE);
    }


    // Removes the vanilla effect particles in case any effect asks for that
    @Redirect(method = "tickEffects", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void EBWIZARDRY$hideVanillaEffectParticles(Level level, ParticleOptions options,
                                                      double x, double y, double z, double vx, double vy, double vz) {
        if (level.isClientSide && MagicMobEffect.hidesVanillaParticles(livingEntity)) return;
        level.addParticle(options, x, y, z, vx, vy, vz);
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    public boolean EBWIZARDRY$avoidDamageOnFrost(LivingEntity living, DamageSource entity, float ev) {
        if (living.hasEffect(EBMobEffects.FROST.get())) {
            return false;
        }
        return living.hurt(entity, ev);
    }

    @Inject(method = "onChangedBlock", at = @At("HEAD"))
    public void EBWIZARDRY$frostStep(net.minecraft.server.level.ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (livingEntity.hasEffect(EBMobEffects.FROST_STEP.get())) {
            FrostStepEffect.onEntityMoved(livingEntity, livingEntity.level(), pos,
                    livingEntity.getEffect(EBMobEffects.FROST_STEP.get()).getAmplifier());
        }
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getGravity()D"))
    private double EBWIZARDRY$flightIgnoresGravity(LivingEntity entity) {
        if (EntityUtil.isCasting(entity, Spells.FLIGHT)) return 0.0;
        return entity.getGravity();
    }
}
