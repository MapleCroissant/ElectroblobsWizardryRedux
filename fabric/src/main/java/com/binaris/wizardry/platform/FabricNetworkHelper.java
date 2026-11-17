package com.binaris.wizardry.platform;

import com.binaris.wizardry.core.networking.abst.Message;
import com.binaris.wizardry.core.platform.services.INetworkHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public class FabricNetworkHelper implements INetworkHelper {
    @Override
    public <T extends Message> void sendTo(ServerPlayer pPlayer, T pMessage) {
        ServerPlayNetworking.send(pPlayer, pMessage);
    }

    @Override
    public <T extends Message> void sendToServer(T pMessage) {
        ClientPlayNetworking.send(pMessage);
    }

    @Override
    public <T extends Message> void sendToTracking(ServerLevel pLevel, BlockPos pPos, T pMessage) {
        net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(pLevel, pPos)
                .forEach(player -> ServerPlayNetworking.send(player, pMessage));
    }

    @Override
    public <T extends Message> void sendToTracking(Entity pEntity, T pMessage) {
        net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(pEntity)
                .forEach(player -> ServerPlayNetworking.send(player, pMessage));
    }

    @Override
    public <T extends Message> void sendToDimension(MinecraftServer server, T packet, ResourceKey<Level> dimension) {
        if (server == null) return;
        ServerLevel level = server.getLevel(dimension);
        if (level != null) level.players().forEach(player -> sendTo(player, packet));
    }
}
