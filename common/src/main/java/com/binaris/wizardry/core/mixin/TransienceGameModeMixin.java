package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.setup.registries.EBMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class TransienceGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$blockBreaking(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (player.hasEffect(EBMobEffects.TRANSIENCE.get())
                || com.binaris.wizardry.content.entity.construct.ForcefieldConstruct.crosses(player, net.minecraft.world.phys.Vec3.atCenterOf(pos))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$blockItemUse(ServerPlayer user, Level level, ItemStack stack, InteractionHand hand,
                                       CallbackInfoReturnable<InteractionResult> cir) {
        if (user.hasEffect(EBMobEffects.TRANSIENCE.get())) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$blockBlockUse(ServerPlayer user, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit,
                                        CallbackInfoReturnable<InteractionResult> cir) {
        if (user.hasEffect(EBMobEffects.TRANSIENCE.get())
                || com.binaris.wizardry.content.entity.construct.ForcefieldConstruct.crosses(user,
                        net.minecraft.world.phys.Vec3.atCenterOf(hit.getBlockPos()))) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
