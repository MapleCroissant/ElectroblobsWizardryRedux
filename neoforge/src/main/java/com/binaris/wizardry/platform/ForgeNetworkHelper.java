package com.binaris.wizardry.platform;

import com.binaris.wizardry.core.networking.abst.Message;
import com.binaris.wizardry.core.platform.services.INetworkHelper;
import com.binaris.wizardry.network.EBForgeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class ForgeNetworkHelper implements INetworkHelper {
    @Override
    public <T extends Message> void sendTo(ServerPlayer pPlayer, T pMessage) {
        PacketDistributor.sendToPlayer(pPlayer, pMessage);
    }

    @Override
    public <T extends Message> void sendToServer(T pMessage) {
        PacketDistributor.sendToServer(pMessage);
    }

    @Override
    public <T extends Message> void sendToTracking(ServerLevel pLevel, BlockPos pPos, T pMessage) {
        PacketDistributor.sendToPlayersTrackingChunk(pLevel, new net.minecraft.world.level.ChunkPos(pPos), pMessage);
    }

    @Override
    public <T extends Message> void sendToTracking(Entity pEntity, T pMessage) {
        PacketDistributor.sendToPlayersTrackingEntity(pEntity, pMessage);
    }

    @Override
    public <T extends Message> void sendToDimension(MinecraftServer server, T packet, ResourceKey<Level> dimension) {
        if (server == null) return;
        if (server.getLevel(dimension) != null) PacketDistributor.sendToPlayersInDimension(server.getLevel(dimension), packet);
    }
}
