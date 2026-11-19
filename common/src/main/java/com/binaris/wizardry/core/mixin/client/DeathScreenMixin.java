package com.binaris.wizardry.core.mixin.client;

import com.binaris.wizardry.content.spell.healing.Resurrection;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.core.networking.c2s.ControlInputPacketC2S;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.EBItems;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {
    @Unique private Button ebwizardry$resurrect;

    protected DeathScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void ebwizardry$addResurrect(CallbackInfo ci) {
        ebwizardry$resurrect = null;
        if (minecraft.player == null
                || !ArtifactChannel.isEquipped(minecraft.player, EBItems.AMULET_RESURRECTION.get())) return;
        boolean hasSpell = false;
        for (int slot = 0; slot < 9; slot++) hasSpell |= Resurrection.supports(minecraft.player.getInventory().getItem(slot));
        if (!hasSpell) return;
        ebwizardry$resurrect = addRenderableWidget(Button.builder(Component.empty(), button ->
                Services.NETWORK_HELPER.sendToServer(new ControlInputPacketC2S(ControlInputPacketC2S.ControlType.RESURRECT_BUTTON)))
                .bounds(width / 2 - 100, height / 4 + 120, 200, 20).build());
        ebwizardry$updateResurrect(ci);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ebwizardry$updateResurrect(CallbackInfo ci) {
        if (ebwizardry$resurrect == null || minecraft.player == null) return;
        int remaining = Math.max(0, Resurrection.waitTicks() - minecraft.player.deathTime);
        ebwizardry$resurrect.active = remaining == 0;
        ebwizardry$resurrect.setMessage(remaining == 0 ? Component.translatable("spell.ebwizardry.resurrection.button_ready")
                : Component.translatable("spell.ebwizardry.resurrection.button_wait", (remaining + 19) / 20));
    }
}
