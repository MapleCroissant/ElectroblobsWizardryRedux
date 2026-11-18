package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.setup.registries.EBMobEffects;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class, Player.class})
public abstract class MuffleSoundMixin {
    @Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$muteEntitySound(SoundEvent sound, float volume, float pitch, CallbackInfo callback) {
        if ((Object) this instanceof LivingEntity living && living.hasEffect(EBMobEffects.MUFFLE.get())) {
            callback.cancel();
        }
    }
}
