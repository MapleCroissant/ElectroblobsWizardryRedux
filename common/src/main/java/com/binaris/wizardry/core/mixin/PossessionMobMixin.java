package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.content.spell.necromancy.Possession;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class PossessionMobMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$pauseHostAI(CallbackInfo ci) {
        if (Possession.controller((Mob) (Object) this) != null) ci.cancel();
    }
}
