package com.binaris.wizardry.core.mixin.client;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.effect.CurseMobEffect;
import com.binaris.wizardry.api.content.effect.MagicMobEffectInstance;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EffectRenderingInventoryScreen.class)
public abstract class EffectRenderingInventoryScreenMixin {
    @Redirect(method = "renderBackgrounds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void ebwizardry$effectBackground(GuiGraphics graphics, ResourceLocation sprite, int x, int y,
                                             int width, int height, @Local MobEffectInstance effect) {
        String texture = effect instanceof MagicMobEffectInstance ? "mobeffect_curse_blue" :
                effect.getEffect().value() instanceof CurseMobEffect ? "mobeffect_curse_red" : null;
        if (texture == null) graphics.blitSprite(sprite, x, y, width, height);
        else graphics.blit(WizardryMainMod.location("textures/gui/effect/" + texture + ".png"),
                x, y, 0, width > 32 ? 166 : 198, width, height);
    }
}