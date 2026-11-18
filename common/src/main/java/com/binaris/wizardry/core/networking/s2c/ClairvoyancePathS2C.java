package com.binaris.wizardry.core.networking.s2c;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.spell.sorcery.Clairvoyance;
import com.binaris.wizardry.core.networking.abst.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ClairvoyancePathS2C(List<BlockPos> points, int duration) implements Message {
    public static final ResourceLocation ID = WizardryMainMod.location("clairvoyance_path");

    public ClairvoyancePathS2C(FriendlyByteBuf buffer) { this(readPath(buffer), buffer.readVarInt()); }

    private static List<BlockPos> readPath(FriendlyByteBuf buffer) {
        int length = buffer.readVarInt();
        if (length < 1 || length > Clairvoyance.MAX_PATH_POINTS) throw new IllegalArgumentException("Invalid clairvoyance path length");
        var path = new ArrayList<BlockPos>(length);
        for (int i = 0; i < length; i++) path.add(buffer.readBlockPos());
        return path;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(points.size());
        points.forEach(buffer::writeBlockPos);
        buffer.writeVarInt(duration);
    }

    @Override
    public void handleClient() {
        com.binaris.wizardry.client.ClairvoyanceParticles.spawn(points, duration);
    }

    @Override
    public ResourceLocation getId() { return ID; }
}
