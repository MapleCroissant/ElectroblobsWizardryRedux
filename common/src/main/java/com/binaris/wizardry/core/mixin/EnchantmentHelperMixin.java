package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.content.item.FlamingAxeItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Inject(method = "doPostAttackEffects", at = @At("TAIL"))
    private static void EBWIZARDRY$flamingAxe(net.minecraft.server.level.ServerLevel level,
                                           net.minecraft.world.entity.Entity target,
                                           net.minecraft.world.damagesource.DamageSource source,
                                           org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!(source.getEntity() instanceof LivingEntity player)) return;
        if (source.getDirectEntity() != player) return;
        if (player.getOffhandItem().getItem() instanceof FlamingAxeItem || player.getMainHandItem().getItem() instanceof FlamingAxeItem) {
            target.igniteForSeconds(4);
        }
    }
}
