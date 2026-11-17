package com.binaris.wizardry;

import com.binaris.wizardry.api.content.event.*;
import com.binaris.wizardry.attachments.EBComponents;
import com.binaris.wizardry.core.PropertiesForgeDataManager;
import com.binaris.wizardry.core.event.WizardryEventBus;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.registry.EBRegistriesForge;
import com.binaris.wizardry.setup.registries.*;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import java.util.function.Consumer;

public final class WizardryForgeEvents {
    @EventBusSubscriber(modid = WizardryMainMod.MOD_ID)
    public static class GameEvents {
        private static int migrationServerTicks;
        @SubscribeEvent public static void serverSmoke(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
            if (Boolean.getBoolean("ebwizardry.migrationServerSmoke") && ++migrationServerTicks >= 100) {
                com.binaris.wizardry.core.EBLogger.info("NeoForge dedicated-server smoke test completed after 100 ticks");
                event.getServer().halt(false);
            }
        }
        @SubscribeEvent public static void armorAttributes(ItemAttributeModifierEvent event) {
            var stack = event.getItemStack();
            if (stack.getItem() instanceof com.binaris.wizardry.content.item.armor.WizardArmorItem armor && armor.getMana(stack) == 0) event.clearModifiers();
        }
        @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
            EBCommands.COMMANDS_TO_REGISTER.forEach(command -> command.accept(event.getDispatcher(), event.getBuildContext()));
            if (Services.PLATFORM.isDevelopmentEnvironment() || com.binaris.wizardry.core.config.EBCommonConfig.ENABLE_DEBUG_COMMANDS.get()) {
                EBCommands.DEBUG_COMMANDS.forEach(command -> command.accept(event.getDispatcher(), event.getBuildContext()));
            }
        }
        @SubscribeEvent public static void starting(ServerAboutToStartEvent event) { WizardryEventBus.fireEvent(new EBServerLoad(event.getServer())); }
        @SubscribeEvent public static void level(LevelEvent.Load event) {
            if (event.getLevel() instanceof ServerLevel level) WizardryEventBus.fireEvent(new EBServerLevelLoadEvent(level));
        }
        @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
            WizardryEventBus.fireEvent(new EBPlayerJoinServerEvent(event.getEntity(), event.getEntity().getServer()));
            Services.OBJECT_DATA.syncPlayerData(event.getEntity());
        }
        @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { Services.OBJECT_DATA.syncPlayerData(event.getEntity()); }
        @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { Services.OBJECT_DATA.syncPlayerData(event.getEntity()); }
        @SubscribeEvent public static void track(PlayerEvent.StartTracking event) {
            var target = event.getTarget();
            if (target instanceof LivingEntity living) com.binaris.wizardry.core.networking.MagicEffectSync.sendAllTo((net.minecraft.server.level.ServerPlayer) event.getEntity(), living);
            if (target instanceof Mob) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) event.getEntity(), com.binaris.wizardry.network.AttachmentSyncS2C.packet(target, EBComponents.MINION_DATA));
            if (target instanceof LivingEntity) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) event.getEntity(), com.binaris.wizardry.network.AttachmentSyncS2C.packet(target, EBComponents.CONTAINMENT_DATA));
        }
        @SubscribeEvent public static void chunk(ChunkWatchEvent.Sent event) {
            for (var block : event.getLevel().getChunk(event.getPos().x, event.getPos().z).getBlockEntities().values()) {
                if (block instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity) EBComponents.ARCANE_LOCK_DATA.sync(block);
            }
        }
        @SubscribeEvent public static void reload(AddReloadListenerEvent event) { event.addListener(new PropertiesForgeDataManager()); }
        @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event) {
            if (WizardryEventBus.fireEvent(new EBPlayerInteractEntityEvent(event.getEntity(), event.getTarget()))) event.setCanceled(true);
        }
        @SubscribeEvent public static void use(PlayerInteractEvent.RightClickBlock event) {
            if (WizardryEventBus.fireEvent(new EBPlayerUseBlockEvent(event.getEntity(), event.getLevel(), event.getPos(), event.getHand()))) event.setCanceled(true);
        }
        @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent event) {
            if (WizardryEventBus.fireEvent(new EBPlayerBreakBlockEvent(event.getPlayer(), (Level) event.getLevel(), event.getPos()))) event.setCanceled(true);
        }
        @SubscribeEvent public static void loot(LootTableLoadEvent event) {
            var pools = ((com.binaris.wizardry.mixin.LootTableAccessor) event.getTable()).getPools();
            if (event.getName().equals(net.minecraft.world.level.storage.loot.BuiltInLootTables.FISHING.location()) && !pools.isEmpty()) {
                var access = (com.binaris.wizardry.mixin.LootPoolAccessor) pools.getFirst();
                var entries = new java.util.ArrayList<>(access.getEntries());
                entries.add(EBLootTables.fishingEntry(40).build());
                access.setEntries(java.util.List.copyOf(entries));
            }
            EBLootTables.applyInjections((id, pool) -> { if (id.equals(event.getName())) event.getTable().addPool(pool); });
        }
    }

    @EventBusSubscriber(modid = WizardryMainMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent public static void tests(RegisterGameTestsEvent event) {
            event.register(com.binaris.wizardry.gametest.ArcaneWorkbenchTest.class);
            event.register(com.binaris.wizardry.gametest.WandTest.class);
            event.register(com.binaris.wizardry.gametest.ConjureItemTest.class);
            event.register(com.binaris.wizardry.gametest.ImbuementEnchantsTest.class);
            event.register(com.binaris.wizardry.gametest.WizardArmorTest.class);
            event.register(com.binaris.wizardry.gametest.MigrationTest.class);
            event.register(com.binaris.wizardry.gametest.NeoForgeMigrationTest.class);
        }
        @SubscribeEvent public static void content(RegisterEvent event) {
            var key = event.getRegistryKey();
            if (key == Registries.MOB_EFFECT) register(event, EBMobEffects::register);
            else if (key == Registries.BLOCK) register(event, EBBlocks::register);
            else if (key == Registries.BLOCK_ENTITY_TYPE) register(event, EBBlockEntities::register);
            else if (key == Registries.CREATIVE_MODE_TAB) register(event, EBCreativeTabs::register);
            else if (key == Registries.ENTITY_TYPE) register(event, EBEntities::register);
            else if (key == Registries.ITEM) register(event, EBItems::register);
            else if (key == Registries.PARTICLE_TYPE) register(event, EBParticles::registerType);
            else if (key == Registries.SOUND_EVENT) register(event, EBSounds::register);
            else if (key == Registries.LOOT_FUNCTION_TYPE) register(event, EBLootFunctions::register);
            else if (key == Registries.MENU) register(event, EBMenus::register);
            else if (key == Registries.ATTRIBUTE) register(event, EBAttributes::register);
            else if (key == Registries.RECIPE_TYPE) register(event, EBRecipeTypes::register);
            else if (key == Registries.RECIPE_SERIALIZER) register(event, EBRecipeTypes::registerSerializers);
            else if (key == Registries.DATA_COMPONENT_TYPE) EBDataComponents.init();
            else if (key == Registries.TRIGGER_TYPE) EBAdvancementTriggers.register();
            else if (key == Registries.ARMOR_MATERIAL) {
                for (var type : com.binaris.wizardry.content.item.armor.WizardArmorTypes.values()) type.material();
            }
            else if (key == EBRegistriesForge.ELEMENT.key()) Elements.register(EBRegistriesForge.ELEMENT, net.minecraft.core.Registry::register);
            else if (key == EBRegistriesForge.TIER.key()) SpellTiers.register(EBRegistriesForge.TIER, net.minecraft.core.Registry::register);
            else if (key == EBRegistriesForge.SPELL.key()) Spells.register(EBRegistriesForge.SPELL, net.minecraft.core.Registry::register);
        }
        private static <T> void register(RegisterEvent event, Consumer<RegisterFunction<T>> consumer) {
            consumer.accept((registry, id, value) -> event.register(registry.key(), id, () -> value));
        }
        @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) { EBEntities.registerAttributes(event::put); }
        @SubscribeEvent public static void modify(EntityAttributeModificationEvent event) {
            event.getTypes().forEach(type -> EBAttributes.getAttributes().forEach(attribute -> event.add(type, attribute.get())));
        }
    }
}
