package com.binaris.wizardry.core.networking.c2s;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.blockentity.LecternBlockEntity;
import com.binaris.wizardry.core.networking.abst.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public record SelectLecternSpellC2S(BlockPos pos, ResourceLocation spell) implements Message {
    public static final ResourceLocation ID = WizardryMainMod.location("select_lectern_spell");

    public SelectLecternSpellC2S(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readResourceLocation());
    }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeResourceLocation(spell);
    }

    @Override
    public void handleServer(MinecraftServer server, ServerPlayer player) {
        var level = player.serverLevel();
        if (level.hasChunkAt(pos) && player.canInteractWithBlock(pos, 0)
                && level.getBlockEntity(pos) instanceof LecternBlockEntity lectern
                && !spell.equals(lectern.selected())) lectern.select(spell);
    }
}
