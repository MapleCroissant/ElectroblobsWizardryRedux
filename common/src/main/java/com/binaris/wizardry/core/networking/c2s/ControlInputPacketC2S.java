package com.binaris.wizardry.core.networking.c2s;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.core.EBLogger;
import com.binaris.wizardry.api.content.item.ICastItem;
import com.binaris.wizardry.content.menu.ArcaneWorkbenchMenu;
import com.binaris.wizardry.core.networking.abst.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ControlInputPacketC2S implements Message {
    public static final ResourceLocation ID = WizardryMainMod.location("control_input");
    private final ControlType controlType;

    public ControlInputPacketC2S(ControlType type) {
        this.controlType = type;
    }

    public ControlInputPacketC2S(FriendlyByteBuf buf) {
        int ordinal = buf.readInt();
        this.controlType = ordinal >= 0 && ordinal < ControlType.values().length ? ControlType.values()[ordinal] : null;
    }

    @Override
    public void encode(FriendlyByteBuf pBuf) {
        pBuf.writeInt(controlType.ordinal());
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void handleServer(MinecraftServer server, ServerPlayer player) {
        if (player == null || controlType == null) return;
        ItemStack wand = player.getMainHandItem();

        if (!(wand.getItem() instanceof ICastItem)) {
            wand = player.getOffhandItem();
        }

        switch (controlType) {
            case POSSESSION_TAKE_BLOCK:
                com.binaris.wizardry.content.spell.necromancy.Possession.takeBlock(player);
                break;
            case POSSESSION_ABILITY:
                com.binaris.wizardry.content.spell.necromancy.Possession.useAbility(player);
                break;
            case RESURRECT_BUTTON:
                com.binaris.wizardry.content.spell.healing.Resurrection.requestSelf(player);
                break;
            case NEXT_SPELL_KEY:
                if (wand.getItem() instanceof ICastItem castItem) {
                    castItem.selectNextSpell(wand);
                    player.stopUsingItem();
                }
                break;
            case PREVIOUS_SPELL_KEY:
                if (wand.getItem() instanceof ICastItem castItem) {
                    castItem.selectPreviousSpell(wand);
                    player.stopUsingItem();
                }
                break;
            case APPLY_BUTTON:
                if (!(player.containerMenu instanceof ArcaneWorkbenchMenu menu)) {
                    EBLogger.warn("Received a ControlInputPacketC2S, but the player that sent it was not currently using an arcane workbench. This should not happen!");
                } else {
                    menu.onApplyButtonPressed(player);
                }
                break;
            case CLEAR_BUTTON:
                if (!(player.containerMenu instanceof ArcaneWorkbenchMenu menu)) {
                    EBLogger.warn("Received a ControlInputPacketC2S, but the player that sent it was not currently using an arcane workbench. This should not happen!");
                } else {
                    menu.onClearButtonPressed(player);
                }
        }
    }

    public enum ControlType {
        APPLY_BUTTON, NEXT_SPELL_KEY,
        PREVIOUS_SPELL_KEY, RESURRECT_BUTTON,
        CLEAR_BUTTON, POSSESSION_ABILITY, POSSESSION_TAKE_BLOCK
    }
}
