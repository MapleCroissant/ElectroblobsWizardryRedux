package com.binaris.wizardry.client;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.client.effect.ArcaneLockRender;
import com.binaris.wizardry.client.effect.ContainmentFieldRender;
import com.binaris.wizardry.client.effect.WingsRender;
import com.binaris.wizardry.core.config.ConfigManager;
import com.binaris.wizardry.core.networking.c2s.RequestPlayerDataSyncC2S;
import com.binaris.wizardry.core.platform.Services;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@net.neoforged.fml.common.EventBusSubscriber(modid = WizardryMainMod.MOD_ID, value = Dist.CLIENT, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.GAME)
public class ForgeClientEvents {
    private static int migrationSmokeTicks;
    @SubscribeEvent
    public static void migrationSmoke(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("ebwizardry.migrationSmoke")) return;
        var minecraft = Minecraft.getInstance();
        minecraft.options.pauseOnLostFocus = false;
        if (minecraft.level == null || minecraft.player == null) return;

        SpellVisualSmoke.tick(minecraft, ++migrationSmokeTicks);
        SpellVisualSmoke.tickLectern(minecraft, migrationSmokeTicks);
        SpellVisualSmoke.tickGreaterTelekinesis(minecraft, migrationSmokeTicks);
        SpellVisualSmoke.tickMagicLight(minecraft, migrationSmokeTicks);
        SpellVisualSmoke.tickFrostBarrier(minecraft, migrationSmokeTicks);
        SpellVisualSmoke.tickSpellVision(minecraft, migrationSmokeTicks);
        SpellVisualSmoke.tickIllusions(minecraft, migrationSmokeTicks);
        SpellVisualSmoke.tickPossession(minecraft, migrationSmokeTicks);
        if (migrationSmokeTicks >= 680) {
            SpellVisualSmoke.verifyIllusions();
            SpellVisualSmoke.verifyPossession();
            com.binaris.wizardry.core.EBLogger.info("NeoForge integrated-world smoke test completed after 680 client ticks");
            minecraft.stop();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        ContainmentFieldRender.render(event.getCamera(), event.getPoseStack(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
        ArcaneLockRender.render(event.getCamera(), event.getPoseStack(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
        WingsRender.render(event.getCamera(), event.getPoseStack(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft.getInstance().execute(() ->
                Services.NETWORK_HELPER.sendToServer(new RequestPlayerDataSyncC2S()));
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ConfigManager.restoreLocalConfigs();
    }
}
