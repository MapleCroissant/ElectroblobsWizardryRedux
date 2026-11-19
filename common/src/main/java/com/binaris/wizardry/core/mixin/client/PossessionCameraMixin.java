package com.binaris.wizardry.core.mixin.client;

import com.binaris.wizardry.content.spell.necromancy.Possession;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class PossessionCameraMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeHeight()F"))
    private float ebwizardry$hostEyeHeight(Entity entity) {
        return entity instanceof Player player ? Possession.eyeHeight(player) : entity.getEyeHeight();
    }
}
