package com.binaris.wizardry.network;

import com.binaris.wizardry.core.networking.abst.Message;
import com.binaris.wizardry.core.networking.c2s.*;
import com.binaris.wizardry.core.networking.s2c.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** Register both directions on both physical sides before play connections open. */
public final class EBFabricPayloads {
    private EBFabricPayloads() {
    }

    public static void register() {
        register(PayloadTypeRegistry.playC2S(), BlockUsePacketC2S.ID, BlockUsePacketC2S::new);
        register(PayloadTypeRegistry.playC2S(), ControlInputPacketC2S.ID, ControlInputPacketC2S::new);
        register(PayloadTypeRegistry.playC2S(), SpellAccessPacketC2S.ID, SpellAccessPacketC2S::new);
        register(PayloadTypeRegistry.playC2S(), RequestPlayerDataSyncC2S.ID, RequestPlayerDataSyncC2S::new);
        register(PayloadTypeRegistry.playS2C(), TestParticlePacketS2C.ID, TestParticlePacketS2C::new);
        register(PayloadTypeRegistry.playS2C(), SpellGlyphPacketS2C.ID, SpellGlyphPacketS2C::new);
        register(PayloadTypeRegistry.playS2C(), SpellPropertiesSyncS2C.ID, SpellPropertiesSyncS2C::new);
        register(PayloadTypeRegistry.playS2C(), NPCSpellCastS2C.ID, NPCSpellCastS2C::new);
        register(PayloadTypeRegistry.playC2S(), SelectLecternSpellC2S.ID, SelectLecternSpellC2S::new);
        register(PayloadTypeRegistry.playS2C(), OpenLecternS2C.ID, OpenLecternS2C::new);
        register(PayloadTypeRegistry.playS2C(), DispenserCastS2C.ID, DispenserCastS2C::new);
        register(PayloadTypeRegistry.playS2C(), SpellCastS2C.ID, SpellCastS2C::new);
        register(PayloadTypeRegistry.playS2C(), ScreenShakeS2C.ID, ScreenShakeS2C::new);
        register(PayloadTypeRegistry.playS2C(), ClairvoyancePathS2C.ID, ClairvoyancePathS2C::new);
        register(PayloadTypeRegistry.playS2C(), SlowTimeS2C.ID, SlowTimeS2C::new);
        register(PayloadTypeRegistry.playS2C(), ParticleBuilderS2C.ID, ParticleBuilderS2C::new);
        register(PayloadTypeRegistry.playS2C(), MagicEffectSyncS2C.ID, MagicEffectSyncS2C::new);
        register(PayloadTypeRegistry.playS2C(), ConfigSyncS2C.ID, ConfigSyncS2C::new);
    }

    private static void register(PayloadTypeRegistry<RegistryFriendlyByteBuf> registry,
                                 ResourceLocation id, Function<FriendlyByteBuf, ? extends Message> decoder) {
        registry.register(new CustomPacketPayload.Type<Message>(id), new StreamCodec<>() {
            @Override
            public Message decode(RegistryFriendlyByteBuf buffer) {
                return decoder.apply(buffer);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Message message) {
                message.encode(buffer);
            }
        });
    }
}
