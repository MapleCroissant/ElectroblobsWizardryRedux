package com.binaris.wizardry.core.mixin.network;

import com.binaris.wizardry.api.content.effect.MagicMobEffectInstance;
import com.binaris.wizardry.api.network.MagicEffectPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundUpdateMobEffectPacket.class)
public abstract class ClientboundUpdateMobEffectPacketMixin implements MagicEffectPacket {
    @Unique
    private boolean ebwizardry$magic;

    @Inject(method = "<init>(ILnet/minecraft/world/effect/MobEffectInstance;Z)V", at = @At("TAIL"))
    private void EBWIZARDRY$captureMagic(int entityId, MobEffectInstance instance, boolean blend, CallbackInfo ci) {
        this.ebwizardry$magic = instance instanceof MagicMobEffectInstance;
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void EBWIZARDRY$writeMagic(net.minecraft.network.RegistryFriendlyByteBuf buf, CallbackInfo ci) {
        buf.writeBoolean(this.ebwizardry$magic);
    }

    @Inject(method = "<init>(Lnet/minecraft/network/RegistryFriendlyByteBuf;)V", at = @At("TAIL"))
    private void EBWIZARDRY$readMagic(net.minecraft.network.RegistryFriendlyByteBuf buf, CallbackInfo ci) {
        this.ebwizardry$magic = buf.readBoolean();
    }

    @Override
    public boolean ebwizardry$isMagic() {
        return this.ebwizardry$magic;
    }
}
