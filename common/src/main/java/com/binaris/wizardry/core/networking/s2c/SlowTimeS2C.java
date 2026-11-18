package com.binaris.wizardry.core.networking.s2c;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.client.effect.SlowTimeClient;
import com.binaris.wizardry.core.networking.abst.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SlowTimeS2C(boolean blocked) implements Message {
    public static final ResourceLocation ID = WizardryMainMod.location("slow_time_pause");

    public SlowTimeS2C(FriendlyByteBuf buffer) { this(buffer.readBoolean()); }

    @Override
    public void encode(FriendlyByteBuf buffer) { buffer.writeBoolean(blocked); }

    @Override
    public void handleClient() { SlowTimeClient.setBlocked(blocked); }

    @Override
    public ResourceLocation getId() { return ID; }
}
