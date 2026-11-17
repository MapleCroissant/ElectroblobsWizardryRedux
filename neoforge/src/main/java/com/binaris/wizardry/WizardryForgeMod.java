package com.binaris.wizardry;

import com.binaris.wizardry.attachments.EBComponents;
import com.binaris.wizardry.client.NotImplementedItems;
import com.binaris.wizardry.client.WizardryForgeClient;
import com.binaris.wizardry.content.menu.BookshelfMenu;
import com.binaris.wizardry.network.EBForgeNetwork;
import com.binaris.wizardry.registry.*;
import com.binaris.wizardry.setup.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(WizardryMainMod.MOD_ID)
public final class WizardryForgeMod {
    public WizardryForgeMod(IEventBus bus) {
        if (Boolean.getBoolean("ebwizardry.migrationJarSmoke")) {
            com.binaris.wizardry.core.EBLogger.info("Packaged NeoForge mod origin: {}", WizardryForgeMod.class.getProtectionDomain().getCodeSource().getLocation());
        }
        WizardryMainMod.init();
        bus.addListener(EBRegistriesForge::register);
        EBComponents.register(bus);
        EBArgumentTypesForge.register(bus);
        bus.addListener(EBForgeNetwork::register);
        bus.addListener(WizardryForgeMod::commonSetup);
        if (FMLEnvironment.dist.isClient()) bus.addListener(WizardryForgeClient::clientSetup);
    }
    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BookshelfMenu.initBookItems(); WandUpgrades.initUpgrades();
            EntityTransformations.init(); NotImplementedItems.init();
            com.binaris.wizardry.integration.curios.CuriosIntegration.load();
        });
    }
}
