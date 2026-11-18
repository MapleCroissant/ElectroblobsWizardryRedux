package com.binaris.wizardry.core.networking.s2c;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.client.gui.screens.LecternScreen;
import com.binaris.wizardry.core.networking.abst.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record OpenLecternS2C(BlockPos pos) implements Message {
    public static final ResourceLocation ID = WizardryMainMod.location("open_lectern");

    public OpenLecternS2C(FriendlyByteBuf buffer) { this(buffer.readBlockPos()); }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public void encode(FriendlyByteBuf buffer) { buffer.writeBlockPos(pos); }

    @Override
    public void handleClient() { LecternScreen.open(pos); }
}
