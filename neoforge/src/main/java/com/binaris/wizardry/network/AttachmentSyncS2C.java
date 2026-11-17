package com.binaris.wizardry.network;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.attachments.EBComponents;
import com.binaris.wizardry.core.networking.abst.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AttachmentSyncS2C implements Message {
    public static final ResourceLocation ID = WizardryMainMod.location("attachment_sync");
    private final int entityId;
    private final BlockPos pos;
    private final String key;
    private final CompoundTag data;
    private AttachmentSyncS2C(int entityId, BlockPos pos, String key, CompoundTag data) {
        this.entityId = entityId; this.pos = pos; this.key = key; this.data = data;
    }
    public AttachmentSyncS2C(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readBlockPos(), buffer.readUtf(), buffer.readNbt());
    }
    @Override public ResourceLocation getId() { return ID; }
    @Override public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId); buffer.writeBlockPos(pos); buffer.writeUtf(key); buffer.writeNbt(data);
    }
    @Override public void handleClient() {
        var level = Minecraft.getInstance().level;
        if (level == null || data == null) return;
        IAttachmentHolder holder = entityId >= 0 ? level.getEntity(entityId) : level.getBlockEntity(pos);
        if (holder == null) return;
        EBComponents.ALL.stream().filter(candidate -> candidate.id().equals(key)).findFirst()
                .ifPresent(candidate -> candidate.get(holder).readFromNbt(data, level.registryAccess()));
    }
    public static AttachmentSyncS2C packet(Entity entity, EBComponents.Key<?> key) {
        CompoundTag tag = new CompoundTag(); key.get(entity).writeToNbt(tag, entity.registryAccess());
        return new AttachmentSyncS2C(entity.getId(), BlockPos.ZERO, key.id(), tag);
    }
    public static void send(Entity entity, EBComponents.Key<?> key) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, packet(entity, key));
    }
    public static void send(BlockEntity block, EBComponents.Key<?> key) {
        CompoundTag tag = new CompoundTag(); key.get(block).writeToNbt(tag, block.getLevel().registryAccess());
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) block.getLevel(), new net.minecraft.world.level.ChunkPos(block.getBlockPos()),
                new AttachmentSyncS2C(-1, block.getBlockPos(), key.id(), tag));
    }
}
