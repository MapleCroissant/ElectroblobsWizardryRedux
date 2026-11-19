package com.binaris.wizardry.core.mixin.client;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.client.effect.BlinkOverlay;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class SpellVisionRendererMixin {
    @Unique
    private static final ResourceLocation EBWIZARDRY_TRANSIENCE = WizardryMainMod.location("shaders/post/transience.json");

    @Unique
    private static final ResourceLocation EBWIZARDRY_SIXTH_SENSE = WizardryMainMod.location("shaders/post/sixth_sense.json");

    @Unique
    private static final ResourceLocation EBWIZARDRY_SLOW_TIME = WizardryMainMod.location("shaders/post/slow_time.json");

    @Unique private static final ResourceLocation EBWIZARDRY_POSSESSION = WizardryMainMod.location("shaders/post/possession.json");

    @Shadow PostChain postEffect;
    @Shadow public abstract void loadEffect(ResourceLocation id);
    @Shadow public abstract void shutdownEffect();

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"))
    private net.minecraft.world.phys.EntityHitResult ebwizardry$ignoreOwnHostWhilePicking(
            net.minecraft.world.entity.Entity camera, net.minecraft.world.phys.Vec3 origin, net.minecraft.world.phys.Vec3 end,
            net.minecraft.world.phys.AABB box, java.util.function.Predicate<net.minecraft.world.entity.Entity> allowed, double reach) {
        if (camera instanceof net.minecraft.world.entity.player.Player player
                && com.binaris.wizardry.content.spell.necromancy.Possession.active(player)) {
            return net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                    camera, origin, end, box, other -> allowed.test(other)
                            && !com.binaris.wizardry.content.spell.necromancy.Possession.controls(player, other), reach);
        }
        return net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(camera, origin, end, box, allowed, reach);
    }

    @Unique private ResourceLocation ebwizardry$lastVision;

    @Inject(method = "tick", at = @At("TAIL"))
    private void ebwizardry$updateSpellVision(CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        ResourceLocation desired = null;
        if (player != null) {
            if (com.binaris.wizardry.content.spell.necromancy.Possession.active(player)) desired = EBWIZARDRY_POSSESSION;
            else if (player.hasEffect(EBMobEffects.TRANSIENCE.get())) desired = EBWIZARDRY_TRANSIENCE;
            else if (player.hasEffect(EBMobEffects.SIXTH_SENSE.get())) desired = EBWIZARDRY_SIXTH_SENSE;
            else if (player.hasEffect(EBMobEffects.SLOW_TIME.get())) desired = EBWIZARDRY_SLOW_TIME;
        }
        if (desired == ebwizardry$lastVision) return;
        if (postEffect != null && ebwizardry$lastVision != null
                && ebwizardry$lastVision.toString().equals(postEffect.getName())) shutdownEffect();
        if (desired != null) {
            BlinkOverlay.start(player);
            if (postEffect == null) loadEffect(desired);
        }
        ebwizardry$lastVision = desired;
    }
}