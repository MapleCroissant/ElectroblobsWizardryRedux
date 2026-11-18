package com.binaris.wizardry.network;

import com.binaris.wizardry.client.ClientPacketHandler;
import com.binaris.wizardry.core.networking.abst.Message;
import com.binaris.wizardry.core.networking.c2s.*;
import com.binaris.wizardry.core.networking.s2c.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import java.util.function.Function;

public final class EBForgeNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        register(registrar, BlockUsePacketC2S.ID, BlockUsePacketC2S::new, false);
        register(registrar, ControlInputPacketC2S.ID, ControlInputPacketC2S::new, false);
        register(registrar, SpellAccessPacketC2S.ID, SpellAccessPacketC2S::new, false);
        register(registrar, RequestPlayerDataSyncC2S.ID, RequestPlayerDataSyncC2S::new, false);
        register(registrar, TestParticlePacketS2C.ID, TestParticlePacketS2C::new, true);
        register(registrar, SpellGlyphPacketS2C.ID, SpellGlyphPacketS2C::new, true);
        register(registrar, SpellPropertiesSyncS2C.ID, SpellPropertiesSyncS2C::new, true);
        register(registrar, NPCSpellCastS2C.ID, NPCSpellCastS2C::new, true);
        register(registrar, SelectLecternSpellC2S.ID, SelectLecternSpellC2S::new, false);
        register(registrar, OpenLecternS2C.ID, OpenLecternS2C::new, true);
        register(registrar, DispenserCastS2C.ID, DispenserCastS2C::new, true);
        register(registrar, SpellCastS2C.ID, SpellCastS2C::new, true);
        register(registrar, ScreenShakeS2C.ID, ScreenShakeS2C::new, true);
        register(registrar, ClairvoyancePathS2C.ID, ClairvoyancePathS2C::new, true);
        register(registrar, SlowTimeS2C.ID, SlowTimeS2C::new, true);
        register(registrar, ParticleBuilderS2C.ID, ParticleBuilderS2C::new, true);
        register(registrar, MagicEffectSyncS2C.ID, MagicEffectSyncS2C::new, true);
        register(registrar, ConfigSyncS2C.ID, ConfigSyncS2C::new, true);
        register(registrar, AttachmentSyncS2C.ID, AttachmentSyncS2C::new, true);
    }

    private static void register(PayloadRegistrar registrar, ResourceLocation id,
                                 Function<FriendlyByteBuf, ? extends Message> decoder, boolean client) {
        var type = new CustomPacketPayload.Type<Message>(id);
        StreamCodec<RegistryFriendlyByteBuf, Message> codec = new StreamCodec<>() {
            public Message decode(RegistryFriendlyByteBuf buffer) { return decoder.apply(buffer); }
            public void encode(RegistryFriendlyByteBuf buffer, Message message) { message.encode(buffer); }
        };
        if (client) {
            // Class resolution stays behind a client-only handler invocation.
            registrar.playToClient(type, codec, (message, context) -> context.enqueueWork(() -> ClientPacketHandler.handleClientPacket(message)));
        } else {
            registrar.playToServer(type, codec, (message, context) -> context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();
                message.handleServer(player.server, player);
            }));
        }
    }
}
