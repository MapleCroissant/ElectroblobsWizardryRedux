package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.content.entity.construct.ForcefieldConstruct;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Explosion.class)
public abstract class ForcefieldExplosionMixin {
    @Shadow @Final private Level level;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow public abstract List<BlockPos> getToBlow();

    @ModifyExpressionValue(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> ebwizardry$protectEntities(List<Entity> entities) {
        if (level.isClientSide || entities.isEmpty()) return entities;
        var field = ForcefieldConstruct.surrounding(level, new Vec3(x, y, z));
        var filtered = new ArrayList<>(entities);
        filtered.removeIf(entity -> ForcefieldConstruct.surrounding(level, entity.getBoundingBox().getCenter()) != field);
        return filtered;
    }

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void ebwizardry$protectBlocks(boolean particles, CallbackInfo ci) {
        if (level.isClientSide) return;
        var field = ForcefieldConstruct.surrounding(level, new Vec3(x, y, z));
        getToBlow().removeIf(pos -> ForcefieldConstruct.surrounding(level, Vec3.atCenterOf(pos)) != field);
    }
}
