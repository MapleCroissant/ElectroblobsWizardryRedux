package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.content.spell.lightning.LightningBoltSpell;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(LightningBolt.class)
public abstract class WizardryLightningMixin implements LightningBoltSpell.StrikeData {
    @Unique private boolean ebwizardry$spellStrike;
    @Unique private UUID ebwizardry$strikeOwner;
    @Unique private float ebwizardry$strikeDamage;

    @Override
    public void ebwizardry$configureStrike(@Nullable UUID owner, float damage) {
        ebwizardry$spellStrike = true;
        ebwizardry$strikeOwner = owner;
        ebwizardry$strikeDamage = damage;
    }

    @Override
    public boolean ebwizardry$isSpellStrike() { return ebwizardry$spellStrike; }

    @Override
    public @Nullable UUID ebwizardry$strikeOwner() { return ebwizardry$strikeOwner; }

    @Override
    public float ebwizardry$strikeDamage() { return ebwizardry$strikeDamage; }

    @Unique
    private boolean ebwizardry$canModify(Level level, BlockPos pos) {
        return !ebwizardry$spellStrike || (ebwizardry$strikeOwner != null && level instanceof ServerLevel server
                && server.getEntity(ebwizardry$strikeOwner) instanceof LivingEntity caster
                && (!(caster instanceof Player) || EBServerConfig.PLAYER_BLOCK_DAMAGE.get())
                && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos) && BlockUtil.canPlaceBlock(caster, level, pos));
    }

    @WrapOperation(method = "spawnFire", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean ebwizardry$protectFire(Level level, BlockPos pos, BlockState fire, Operation<Boolean> original) {
        return ebwizardry$canModify(level, pos) && original.call(level, pos, fire);
    }

    @Inject(method = "powerLightningRod", at = @At("HEAD"), cancellable = true)
    private void ebwizardry$protectRod(CallbackInfo callback) {
        LightningBolt bolt = (LightningBolt) (Object) this;
        if (!ebwizardry$canModify(bolt.level(), BlockPos.containing(bolt.getX(), bolt.getY() - 1.0e-6, bolt.getZ()))) callback.cancel();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LightningBolt;clearCopperOnLightningStrike(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void ebwizardry$protectCopper(Level level, BlockPos pos, Operation<Void> original) {
        // Copper cleaning walks across neighboring blocks. Preserve that behavior for vanilla strikes.
        if (!ebwizardry$spellStrike) original.call(level, pos);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void ebwizardry$saveStrike(CompoundTag tag, CallbackInfo callback) {
        if (!ebwizardry$spellStrike) return;
        tag.putFloat("WizardryStrikeDamage", ebwizardry$strikeDamage);
        if (ebwizardry$strikeOwner != null) tag.putUUID("WizardryStrikeOwner", ebwizardry$strikeOwner);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void ebwizardry$loadStrike(CompoundTag tag, CallbackInfo callback) {
        if (tag.contains("WizardryStrikeDamage")) {
            ebwizardry$configureStrike(tag.hasUUID("WizardryStrikeOwner") ? tag.getUUID("WizardryStrikeOwner") : null,
                    tag.getFloat("WizardryStrikeDamage"));
        }
    }
}
