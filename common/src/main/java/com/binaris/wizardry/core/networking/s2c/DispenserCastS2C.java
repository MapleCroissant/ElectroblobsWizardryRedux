package com.binaris.wizardry.core.networking.s2c;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.core.networking.ClientMessageHandler;
import com.binaris.wizardry.core.networking.abst.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record DispenserCastS2C(BlockPos pos, Direction direction, ResourceLocation spell, int tick, int duration,
                               SpellModifiers modifiers, boolean finished) implements Message {
    public static final ResourceLocation ID = WizardryMainMod.location("dispenser_cast");

    public DispenserCastS2C(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readEnum(Direction.class), buffer.readResourceLocation(), buffer.readVarInt(),
                buffer.readVarInt(), SpellModifiers.fromTag(buffer.readNbt()), buffer.readBoolean());
    }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeEnum(direction);
        buffer.writeResourceLocation(spell);
        buffer.writeVarInt(tick);
        buffer.writeVarInt(duration);
        buffer.writeNbt(modifiers.toTag());
        buffer.writeBoolean(finished);
    }

    @Override
    public void handleClient() { ClientMessageHandler.dispenserCast(this); }
}
