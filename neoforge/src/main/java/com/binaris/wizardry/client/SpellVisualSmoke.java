package com.binaris.wizardry.client;

import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.util.CastItemDataHelper;
import com.binaris.wizardry.api.content.util.RegistryUtils;
import com.binaris.wizardry.client.gui.screens.SpellBookScreen;
import com.binaris.wizardry.client.gui.screens.LecternScreen;
import com.binaris.wizardry.content.blockentity.BookshelfBlockEntity;
import com.binaris.wizardry.content.blockentity.LecternBlockEntity;
import com.binaris.wizardry.setup.registries.EBBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.binaris.wizardry.core.EBLogger;
import com.binaris.wizardry.setup.registries.EBEntities;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;

/// Exercises spell effects in the isolated integrated-world smoke profile.
final class SpellVisualSmoke {
    private static boolean sawIceball;
    private static int iceballFirstSeen;
    private static int baselineParticles;
    private static int rayParticles;
    private static int rayFirstSeen;
    private static BlockPos lecternPos;
    private static int levitatingBlockFirstSeen;
    private static BlockPos magicLightPos;
    private static volatile java.util.UUID illusionDecoy;
    private static boolean sawMirage;

    private SpellVisualSmoke() {
    }

    static void tick(Minecraft minecraft, int tick) {
        var localPlayer = minecraft.player;
        var server = minecraft.getSingleplayerServer();
        if (localPlayer == null || server == null) return;
        var playerId = localPlayer.getUUID();

        if (tick == 25) {
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                var pos = player.blockPosition();
                int y = player.level().getMaxBuildHeight() - 8;
                player.setGameMode(GameType.CREATIVE);
                player.level().setBlockAndUpdate(new BlockPos(pos.getX(), y - 1, pos.getZ()), Blocks.STONE.defaultBlockState());
                player.teleportTo(player.serverLevel(), pos.getX() + 0.5, y, pos.getZ() + 0.5, Set.of(), 0, 0);
            });
        }
        if (tick == 55) {
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                Spells.ICE_BALL.cast(new PlayerCastContext(player.level(), player, InteractionHand.MAIN_HAND, 0, new SpellModifiers()));
            });
        }
        if (tick >= 55 && tick < 85 && !sawIceball) {
            sawIceball = !minecraft.level.getEntities(EBEntities.ICE_BALL.get(),
                    new AABB(localPlayer.blockPosition()).inflate(30), entity -> true).isEmpty();
            if (sawIceball) iceballFirstSeen = tick;
        }
        if (sawIceball && tick == iceballFirstSeen + 3) screenshot(minecraft, "repair-iceball.png");
        if (tick == 100) {
            baselineParticles = Integer.parseInt(minecraft.particleEngine.countParticles());
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                var wand = new ItemStack(EBItems.APPRENTICE_WAND.get());
                CastItemDataHelper.setSpells(wand, List.of(Spells.FROST_RAY));
                CastItemDataHelper.setCurrentSpell(wand, Spells.FROST_RAY);
                player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            });
        }
        if (tick >= 108 && tick < 165) {
            minecraft.options.keyUse.setDown(true);
            if (tick < 132 && tick % 4 == 0 && !localPlayer.isUsingItem()) {
                minecraft.gameMode.useItem(localPlayer, InteractionHand.MAIN_HAND);
            }
        }
        if (tick >= 128 && tick < 165) {
            rayParticles = Math.max(rayParticles, Integer.parseInt(minecraft.particleEngine.countParticles()) - baselineParticles);
            if (rayFirstSeen == 0 && rayParticles >= 20) rayFirstSeen = tick;
        }
        if (rayFirstSeen != 0 && tick == rayFirstSeen + 4) screenshot(minecraft, "repair-frost-ray.png");
        if (tick == 165) {
            minecraft.options.keyUse.setDown(false);
            if (localPlayer.isUsingItem()) minecraft.gameMode.releaseUsingItem(localPlayer);
        }
        if (tick == 175) {
            var book = new ItemStack(EBItems.SPELL_BOOK.get());
            RegistryUtils.setSpell(book, Spells.FROST_RAY);
            minecraft.setScreen(new SpellBookScreen(book));
        }
        if (tick == 190) {
            if (!sawIceball || rayParticles < 20 || !(minecraft.screen instanceof SpellBookScreen)) {
                throw new IllegalStateException("Spell smoke failed: Iceball=" + sawIceball + ", Frost Ray particles="
                        + rayParticles + ", spell book=" + minecraft.screen);
            }
            EBLogger.info("Spell smoke observed client Iceball, {} Frost Ray particles above baseline, and spell book screen", rayParticles);
            minecraft.setScreen(null);
        }
    }

    static void tickLectern(Minecraft minecraft, int tick) {
        if (tick < 195 || minecraft.player == null || minecraft.getSingleplayerServer() == null) return;
        var player = minecraft.player;
        if (tick == 195) {
            var playerId = player.getUUID();
            var server = minecraft.getSingleplayerServer();
            lecternPos = player.blockPosition().south(2);
            BlockPos position = lecternPos;
            server.execute(() -> {
                var serverPlayer = server.getPlayerList().getPlayer(playerId);
                var level = serverPlayer.serverLevel();
                level.setBlockAndUpdate(position, EBBlocks.OAK_LECTERN.get().defaultBlockState());
                level.setBlockAndUpdate(position.east(), EBBlocks.OAK_BOOKSHELF.get().defaultBlockState());
                if (level.getBlockEntity(position) instanceof LecternBlockEntity lectern) lectern.select(Spells.NONE.getLocation());
                if (level.getBlockEntity(position.east()) instanceof BookshelfBlockEntity shelf) {
                    var book = new ItemStack(EBItems.SPELL_BOOK.get());
                    RegistryUtils.setSpell(book, Spells.IRONFLESH);
                    shelf.setItem(0, book);
                }
            });
        }
        if (tick == 216) {
            if (!LecternBlockEntity.index(minecraft.level, lecternPos).containsKey(Spells.IRONFLESH)) {
                throw new IllegalStateException("Lectern smoke did not receive the nearby bookshelf entry");
            }
            minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(lecternPos), Direction.NORTH, lecternPos, false));
        }
        if (tick == 231) {
            if (!(minecraft.screen instanceof LecternScreen)) throw new IllegalStateException("Lectern smoke did not open the spell index");
            screenshot(minecraft, "parity-lectern.png");
        }
        if (tick == 233 && minecraft.screen instanceof LecternScreen screen) {
            screen.mouseClicked((screen.width - 288) / 2 + 32, (screen.height - 180) / 2 + 52, 0);
        }
        if (tick == 240 && minecraft.screen instanceof LecternScreen screen) {
            if (screen.getSpell() != Spells.IRONFLESH) throw new IllegalStateException("Lectern smoke did not open the selected spell");
            screenshot(minecraft, "parity-lectern-spell.png");
            screen.mouseClicked((screen.width - 288) / 2 + 94, (screen.height - 180) / 2 + 157, 0);
        }
        if (tick == 246) {
            if (minecraft.screen != null) throw new IllegalStateException("Lectern smoke did not close the book after locating the shelf");
            screenshot(minecraft, "parity-lectern-locate.png");
            EBLogger.info("Lectern smoke opened the index, selected Ironflesh, and highlighted its nearby bookshelf");
        }
    }

    static void tickGreaterTelekinesis(Minecraft minecraft, int tick) {
        if (tick < 255) return;
        var player = minecraft.player;
        var server = minecraft.getSingleplayerServer();
        if (player == null || server == null) return;
        if (tick == 255) {
            var id = player.getUUID();
            server.execute(() -> {
                var caster = server.getPlayerList().getPlayer(id);
                var level = caster.serverLevel();
                var block = caster.blockPosition().east(3).above();
                level.setBlockAndUpdate(block, Blocks.RED_WOOL.defaultBlockState());
                caster.teleportTo(level, caster.getX(), caster.getY(), caster.getZ(), Set.of(), -90, 0);
                var wand = new ItemStack(EBItems.ADVANCED_WAND.get());
                CastItemDataHelper.setSpells(wand, List.of(Spells.GREATER_TELEKINESIS));
                CastItemDataHelper.setCurrentSpell(wand, Spells.GREATER_TELEKINESIS);
                caster.setItemInHand(InteractionHand.MAIN_HAND, wand);
            });
        }
        if (tick >= 274 && tick < 322) {
            minecraft.options.keyUse.setDown(true);
            if (tick % 4 == 2 && !player.isUsingItem()) minecraft.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            var nearby = new AABB(player.blockPosition()).inflate(12);
            if (levitatingBlockFirstSeen == 0 && !minecraft.level.getEntities(EBEntities.LEVITATING_BLOCK.get(), nearby,
                    block -> block.getBlockState().is(Blocks.RED_WOOL)).isEmpty()) levitatingBlockFirstSeen = tick;
        }
        if (levitatingBlockFirstSeen != 0 && tick == levitatingBlockFirstSeen + 3) screenshot(minecraft, "parity-greater-telekinesis.png");
        if (tick == 322) {
            minecraft.options.keyUse.setDown(false);
            if (player.isUsingItem()) minecraft.gameMode.releaseUsingItem(player);
            if (levitatingBlockFirstSeen == 0) throw new IllegalStateException("Greater Telekinesis smoke never received the raised wool block");
            EBLogger.info("Greater Telekinesis smoke observed the raised block entity and its original state");
        }
    }

    static void tickMagicLight(Minecraft minecraft, int tick) {
        if (tick < 330) return;
        var player = minecraft.player;
        var server = minecraft.getSingleplayerServer();
        if (player == null || server == null) return;
        if (tick == 330) {
            var id = player.getUUID();
            magicLightPos = player.blockPosition().north(4).above();
            server.execute(() -> {
                var caster = server.getPlayerList().getPlayer(id);
                caster.teleportTo(caster.serverLevel(), caster.getX(), caster.getY(), caster.getZ(), Set.of(), 180, 0);
                for (BlockPos pos : BlockPos.betweenClosed(caster.blockPosition().above(), caster.blockPosition().north(5).above())) {
                    if (caster.level().getBlockState(pos).is(EBBlocks.MAGIC_LIGHT.get())) caster.level().removeBlock(pos, false);
                }
                Spells.LIGHT.cast(new PlayerCastContext(caster.level(), caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers()));
            });
        }
        if (tick == 352) {
            if (!minecraft.level.getBlockState(magicLightPos).is(EBBlocks.MAGIC_LIGHT.get())) {
                throw new IllegalStateException("Light smoke did not receive the cast light block");
            }
            screenshot(minecraft, "parity-magic-light.png");
            EBLogger.info("Light smoke observed the cast light block and renderer");
        }
    }

    static void tickFrostBarrier(Minecraft minecraft, int tick) {
        if (tick < 365) return;
        var player = minecraft.player;
        var server = minecraft.getSingleplayerServer();
        if (player == null || server == null) return;
        if (tick == 365) {
            var id = player.getUUID();
            server.execute(() -> {
                var caster = server.getPlayerList().getPlayer(id);
                var level = caster.serverLevel();
                var floor = caster.blockPosition().below(8);
                for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-1, 0, -3), floor.offset(5, 0, 3))) {
                    level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
                }
                level.getEntities(EBEntities.ICE_BARRIER.get(), new AABB(floor).inflate(7), barrier -> true).forEach(barrier -> barrier.discard());
                var origin = Vec3.atBottomCenterOf(floor.above());
                caster.teleportTo(level, origin.x, origin.y, origin.z, Set.of(), -90, 8);
                var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                        origin.x, origin.y, origin.z, Direction.EAST, 0, 0, new SpellModifiers());
                if (!Spells.FROST_BARRIER.cast(context)) throw new IllegalStateException("Frost Barrier smoke could not cast on its platform");
            });
        }
        if (tick == 394) {
            var panels = minecraft.level.getEntities(EBEntities.ICE_BARRIER.get(), new AABB(player.blockPosition()).inflate(6), barrier -> true);
            if (panels.size() != 3) throw new IllegalStateException("Frost Barrier smoke expected three received panels");
            screenshot(minecraft, "parity-frost-barrier.png");
            EBLogger.info("Frost Barrier smoke observed three client-side panels");
        }
    }

    static void tickSpellVision(Minecraft minecraft, int tick) {
        if (tick < 405 || minecraft.player == null || minecraft.getSingleplayerServer() == null) return;
        var server = minecraft.getSingleplayerServer();
        var id = minecraft.player.getUUID();
        if (tick == 405 || tick == 450) {
            server.execute(() -> {
                var caster = server.getPlayerList().getPlayer(id);
                var spell = tick == 405 ? Spells.TRANSIENCE : Spells.SIXTH_SENSE;
                var modifiers = new SpellModifiers().multiply(SpellModifiers.DURATION, 0.08f);
                if (!spell.cast(new PlayerCastContext(caster.serverLevel(), caster, InteractionHand.MAIN_HAND, 0, modifiers))) {
                    throw new IllegalStateException("Vision smoke could not cast " + spell.getLocation());
                }
                if (spell == Spells.SIXTH_SENSE) {
                    var cow = net.minecraft.world.entity.EntityType.COW.create(caster.serverLevel());
                    cow.setPos(caster.position().add(caster.getLookAngle().scale(4)));
                    cow.setNoAi(true);
                    caster.serverLevel().addFreshEntity(cow);
                }
            });
        }
        if (tick == 421 || tick == 466) {
            String spell = tick == 421 ? "transience" : "sixth_sense";
            var effect = minecraft.gameRenderer.currentEffect();
            if (effect == null || !effect.getName().equals("ebwizardry:shaders/post/" + spell + ".json")) {
                throw new IllegalStateException("Vision smoke failed to load the " + spell + " post chain");
            }
            screenshot(minecraft, "parity-" + spell + ".png");
        }
        if (tick == 490) {
            if (minecraft.gameRenderer.currentEffect() != null) throw new IllegalStateException("Vision shader survived effect expiry");
            EBLogger.info("Vision smoke observed both post chains and their cleanup");
        }
    }

    static void tickIllusions(Minecraft minecraft, int tick) {
        if (tick < 500 || minecraft.player == null || minecraft.getSingleplayerServer() == null) return;
        var server = minecraft.getSingleplayerServer();
        var id = minecraft.player.getUUID();
        if (tick == 500) {
            server.execute(() -> {
                var caster = server.getPlayerList().getPlayer(id);
                var context = new PlayerCastContext(caster.serverLevel(), caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
                if (!Spells.DECOY.cast(context)) throw new IllegalStateException("Illusion smoke failed to cast Decoy");
                var decoys = caster.serverLevel().getEntities(EBEntities.DECOY.get(), caster.getBoundingBox().inflate(5),
                        entity -> caster.getUUID().equals(entity.getOwnerUUID()));
                var decoy = decoys.getLast();
                illusionDecoy = decoy.getUUID();
                var point = caster.position().add(caster.getLookAngle().scale(3.5));
                decoy.setPos(point);
                decoy.setDeltaMovement(Vec3.ZERO);
                decoy.setNoAi(true);
                decoy.setNoGravity(true);
            });
        }
        if (tick == 520) {
            var decoys = minecraft.level.getEntities(EBEntities.DECOY.get(), minecraft.player.getBoundingBox().inflate(8),
                    entity -> entity.getUUID().equals(illusionDecoy) && entity.getOwner() == minecraft.player);
            if (decoys.isEmpty()) throw new IllegalStateException("Illusion smoke did not receive an owner-resolved Decoy");
            screenshot(minecraft, "parity-decoy.png");
        }
        if (tick == 525) {
            server.execute(() -> {
                var caster = server.getPlayerList().getPlayer(id);
                var decoy = caster.serverLevel().getEntities(EBEntities.DECOY.get(), caster.getBoundingBox().inflate(8),
                        entity -> entity.getUUID().equals(illusionDecoy)).getFirst();
                var context = new com.binaris.wizardry.api.content.spell.internal.EntityCastContext(
                        caster.serverLevel(), decoy, InteractionHand.MAIN_HAND, 0, null, new SpellModifiers());
                if (!Spells.MIRAGE.cast(context)) throw new IllegalStateException("Illusion smoke failed to cast Mirage");
            });
        }
        if (tick >= 535 && tick < 580 && !sawMirage) {
            var decoys = minecraft.level.getEntities(EBEntities.DECOY.get(), minecraft.player.getBoundingBox().inflate(8),
                    entity -> entity.getUUID().equals(illusionDecoy)
                            && entity.hasEffect(com.binaris.wizardry.setup.registries.EBMobEffects.MIRAGE.get()));
            if (decoys.isEmpty()) return;
            sawMirage = true;
            screenshot(minecraft, "parity-mirage.png");
            EBLogger.info("Illusion smoke observed the owner-resolved decoy and synchronized Mirage effect");
        }
    }

    static void verifyIllusions() {
        if (!sawMirage) throw new IllegalStateException("Illusion smoke did not receive Mirage");
    }

    private static void screenshot(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(), message -> EBLogger.info(message.getString()));
    }
}
