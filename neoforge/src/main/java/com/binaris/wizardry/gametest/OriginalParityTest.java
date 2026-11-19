package com.binaris.wizardry.gametest;

import com.binaris.wizardry.api.content.event.SpellCastEvent;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.util.RegistryUtils;
import com.binaris.wizardry.content.blockentity.BookshelfBlockEntity;
import com.binaris.wizardry.content.blockentity.LecternBlockEntity;
import com.binaris.wizardry.core.event.WizardryEventBus;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.EBBlocks;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ebwizardry")
@PrefixGameTestTemplate(false)
public class OriginalParityTest {
    private static net.minecraft.server.level.ServerPlayer connectedPlayer(net.minecraft.server.level.ServerLevel level, String name) {
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name);
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }




    @GameTest(template = "empty_3x3x3")
    public static void possessionMovesHostAndReleasesOnExitAndRemoval(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = connectedPlayer(level, "possessor");
        player.setGameMode(GameType.SURVIVAL);
        var origin = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 21, 1)));
        player.setPos(origin);
        player.setYRot(0);
        var host = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        host.setPos(origin.add(0, 0, 3));
        level.addFreshEntity(host);
        var spell = (com.binaris.wizardry.content.spell.necromancy.Possession) Spells.POSSESSION;
        var context = new PlayerCastContext(level, player, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        var item = new ItemStack(net.minecraft.world.item.Items.DIAMOND, 23);
        player.getInventory().setItem(6, item);
        var scroll = new ItemStack(EBItems.SCROLL.get());
        var book = new ItemStack(EBItems.SPELL_BOOK.get());
        RegistryUtils.setSpell(scroll, spell);
        RegistryUtils.setSpell(book, spell);
        Services.OBJECT_DATA.getSpellManagerData(player).discoverSpell(spell);
        var castItem = (com.binaris.wizardry.api.content.item.ICastItem) scroll.getItem();
        helper.assertTrue(RegistryUtils.getSpell(book) == spell && castItem.getCurrentSpell(scroll) == spell,
                "Possession did not resolve from its release book and scroll");
        helper.assertTrue(castItem.canCast(scroll, spell, context) && castItem.cast(scroll, spell, context)
                && scroll.isEmpty() && spell.active(player) && spell.controller(host) == player
                && !spell.cast(context), "Possession did not acquire a single valid host");
        player.setPos(player.position().add(1, 0, 0));
        spell.tick(player);
        helper.assertTrue(host.position().equals(player.position()), "Host did not follow the player's movement");
        player.setShiftKeyDown(true);
        spell.tick(player);
        helper.assertTrue(!spell.active(player) && spell.controller(host) == null
                && player.getInventory().getItem(6) == item && item.getCount() == 23,
                "Manual exit did not restore inventory or clear host ownership");
        player.setShiftKeyDown(false);
        helper.assertTrue(spell.start(player, host, 40), "Released host could not be possessed again");
        host.discard();
        spell.tick(player);
        helper.assertTrue(!spell.active(player) && spell.controller(host) == null, "Invalid host left control active");
        level.getServer().getPlayerList().remove(player);
        helper.succeed();
    }




    @GameTest(template = "empty_3x3x3")
    public static void possessionExpiryAndLogoutRestoreFlight(GameTestHelper helper) {
        var level = helper.getLevel();
        var players = level.getServer().getPlayerList();
        var player = connectedPlayer(level, "host_expiry");
        player.setGameMode(GameType.SURVIVAL);
        var pos = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 19, 1)));
        player.setPos(pos);
        var blaze = net.minecraft.world.entity.EntityType.BLAZE.create(level);
        blaze.setPos(pos.add(0, 0, 1));
        blaze.setNoGravity(true);
        level.addFreshEntity(blaze);
        var spell = (com.binaris.wizardry.content.spell.necromancy.Possession) Spells.POSSESSION;
        helper.assertTrue(spell.start(player, blaze, 2) && player.getAbilities().mayfly,
                "Flying host did not grant temporary flight");
        var save = new net.minecraft.nbt.CompoundTag();
        player.addAdditionalSaveData(save);
        helper.assertTrue(!save.getCompound("abilities").getBoolean("mayfly"), "Possession persisted temporary flight");
        helper.runAfterDelay(7, () -> {
            try {
                helper.assertTrue(!spell.active(player) && !player.getAbilities().mayfly && spell.controller(blaze) == null,
                        "Expiry state: active=" + spell.active(player) + ", flight=" + player.getAbilities().mayfly);
                helper.assertTrue(spell.start(player, blaze, 30), "Could not acquire the expired host again");
                players.remove(player);
                helper.assertTrue(spell.controller(blaze) == null && !spell.active(player) && !player.getAbilities().mayfly,
                        "Logout left a possession relationship or flight active");
                helper.succeed();
            } finally {
                if (players.getPlayer(player.getUUID()) == player) players.remove(player);
                blaze.discard();
            }
        });
    }

    @GameTest(template = "empty_3x3x3")
    public static void possessedEndermanCarriesAndPlacesOneBlock(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(1, 31, 1));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin.north(2)));
        var host = net.minecraft.world.entity.EntityType.ENDERMAN.create(level);
        host.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin));
        level.addFreshEntity(host);
        var spell = (com.binaris.wizardry.content.spell.necromancy.Possession) Spells.POSSESSION;
        helper.assertTrue(spell.start(player, host, 50), "Could not possess the Enderman");
        player.setYRot(0);
        player.setXRot(0);
        var block = origin.south(2).above(2);
        level.setBlockAndUpdate(block, Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(block.south(), Blocks.STONE.defaultBlockState());
        spell.takeBlock(player);
        helper.assertTrue(level.isEmptyBlock(block) && host.getCarriedBlock() != null
                && host.getCarriedBlock().is(Blocks.DIRT), "Enderman failed to take exactly the aimed block");
        spell.useAbility(player);
        helper.assertTrue(level.getBlockState(block).is(Blocks.DIRT) && host.getCarriedBlock() == null,
                "Enderman did not place and clear the carried block");
        spell.end(player);
        host.discard();
        level.setBlockAndUpdate(block, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(block.south(), Blocks.AIR.defaultBlockState());
        helper.succeed();
    }


    @GameTest(template = "empty_3x3x3", timeoutTicks = 240)
    public static void resurrectionScrollConsumesOneAndKeepsCooldown(GameTestHelper helper) {
        if (com.binaris.wizardry.core.integrations.ArtifactChannel.getIntegration()
                != com.binaris.wizardry.core.integrations.VanillaArtifactIntegration.INSTANCE) {
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        var players = level.getServer().getPlayerList();
        var player = connectedPlayer(level, "immortal_scroll");
        player.setGameMode(GameType.SURVIVAL);
        Services.OBJECT_DATA.getSpellManagerData(player).discoverSpell(Spells.RESURRECTION);
        var grave = helper.absolutePos(new BlockPos(1, 35, 1));
        player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(grave));
        level.setBlockAndUpdate(grave.below(), Blocks.STONE.defaultBlockState());
        var scroll = new ItemStack(EBItems.SCROLL.get(), 2);
        RegistryUtils.setSpell(scroll, Spells.RESURRECTION);
        player.getInventory().setItem(3, scroll);
        player.getInventory().setItem(6, new ItemStack(EBItems.AMULET_RESURRECTION.get()));
        player.setHealth(0);
        player.die(level.damageSources().genericKill());
        helper.runAfterDelay(202, () -> {
            com.binaris.wizardry.content.spell.healing.Resurrection.requestSelf(player);
            var revived = players.getPlayer(player.getUUID());
            var retained = revived.getInventory().getItem(3);
            helper.assertTrue(revived != player && revived.isAlive() && retained.getCount() == 1
                    && RegistryUtils.getSpell(retained) == Spells.RESURRECTION
                    && revived.getCooldowns().isOnCooldown(EBItems.SCROLL.get()),
                    "Scroll revival failed to consume one scroll or restore the scroll cooldown");
            players.remove(revived);
            level.setBlockAndUpdate(grave.below(), Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 240)
    public static void resurrectionAmuletRetainsOnlyPaidWand(GameTestHelper helper) {
        if (com.binaris.wizardry.core.integrations.ArtifactChannel.getIntegration()
                != com.binaris.wizardry.core.integrations.VanillaArtifactIntegration.INSTANCE) {
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        var players = level.getServer().getPlayerList();
        var player = connectedPlayer(level, "immortal");
        player.setGameMode(GameType.SURVIVAL);
        Services.OBJECT_DATA.getSpellManagerData(player).discoverSpell(Spells.RESURRECTION);
        var grave = helper.absolutePos(new BlockPos(1, 27, 1));
        var landing = net.minecraft.world.phys.Vec3.atBottomCenterOf(grave);
        player.setPos(landing);
        level.setBlockAndUpdate(grave.below(), Blocks.STONE.defaultBlockState());
        var wand = new ItemStack(EBItems.MASTER_WAND.get());
        var item = (com.binaris.wizardry.content.item.WandItem) wand.getItem();
        item.setMana(wand, item.getManaCapacity(wand));
        int before = item.getMana(wand);
        com.binaris.wizardry.api.content.util.CastItemDataHelper.setSpells(wand, java.util.List.of(Spells.RESURRECTION));
        com.binaris.wizardry.api.content.util.CastItemDataHelper.setCurrentSpell(wand, Spells.RESURRECTION);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 1));
        player.getInventory().setItem(4, wand);
        player.getInventory().setItem(6, new ItemStack(EBItems.AMULET_RESURRECTION.get()));
        player.setHealth(0);
        player.die(level.damageSources().genericKill());
        var resurrection = (com.binaris.wizardry.content.spell.healing.Resurrection) Spells.RESURRECTION;
        resurrection.requestSelf(player);
        helper.assertTrue(!player.isAlive() && player.getInventory().getItem(4) == wand
                && player.getInventory().getItem(6).isEmpty(), "Unexpected death state: alive=" + player.isAlive() + " wand=" + player.getInventory().getItem(4)
                + " amulet=" + player.getInventory().getItem(6));
        helper.runAfterDelay(202, () -> {
            resurrection.requestSelf(player);
            var revived = players.getPlayer(player.getUUID());
            var retained = revived.getInventory().getItem(4);
            helper.assertTrue(revived != player && revived.isAlive() && retained.is(EBItems.MASTER_WAND.get())
                    && item.getMana(retained) == before - 150
                    && com.binaris.wizardry.api.content.util.CastItemDataHelper.getCurrentCooldown(retained, level.getGameTime()) == 400,
                    "Amulet revival cost mismatch: alive=" + revived.isAlive() + " item=" + retained + " mana=" + item.getMana(retained)
                    + " before=" + before + " cooldown=" + com.binaris.wizardry.api.content.util.CastItemDataHelper.getCurrentCooldown(retained, level.getGameTime()));
            helper.assertTrue(revived.getInventory().getItem(6).isEmpty(), "Revival duplicated the amulet");
            resurrection.requestSelf(player);
            helper.assertTrue(players.getPlayer(player.getUUID()) == revived, "The death could be replayed");
            players.remove(revived);
            level.setBlockAndUpdate(grave.below(), Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 240)
    public static void resurrectionRevivesOnlyAfterWaitAndUsesOnePlayer(GameTestHelper helper) {
        var level = helper.getLevel();
        var players = level.getServer().getPlayerList();
        var caster = connectedPlayer(level, "healer");
        var ally = connectedPlayer(level, "revival");
        var grave = helper.absolutePos(new BlockPos(1, 24, 1));
        level.setBlockAndUpdate(grave.below(), Blocks.STONE.defaultBlockState());
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(grave.offset(3, 0, 0)));
        ally.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(grave));
        Services.OBJECT_DATA.getWizardData(caster).toggleAlly(ally);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(!Spells.RESURRECTION.cast(context), "Resurrection selected a living ally");
        ally.setHealth(0);
        ally.die(level.damageSources().genericKill());
        caster.setNoGravity(true);
        helper.assertTrue(!Spells.RESURRECTION.cast(context), "Resurrection bypassed the wait");
        helper.runAfterDelay(202, () -> {
            com.binaris.wizardry.content.spell.healing.Resurrection.requestSelf(ally);
            helper.assertTrue(players.getPlayer(ally.getUUID()) == ally && !ally.isAlive(), "Self-revival bypassed the amulet");
            helper.assertTrue(Spells.RESURRECTION.cast(context), "Resurrection failed to revive the dead ally");
            var replacement = players.getPlayer(ally.getUUID());
            helper.assertTrue(replacement != ally && replacement.isAlive() && replacement.serverLevel() == level
                    && replacement.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(grave)) < 1
                    && replacement.getHealth() == replacement.getMaxHealth() / 2 && replacement.totalExperience == 0,
                    "Resurrection did not restore the ally safely at half health");
            helper.assertTrue(!Spells.RESURRECTION.cast(context)
                    && players.getPlayers().stream().filter(p -> p.getUUID().equals(ally.getUUID())).count() == 1,
                    "A single death produced multiple live player entries");
            players.remove(replacement);
            players.remove(caster);
            level.setBlockAndUpdate(grave.below(), Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    @GameTest(template = "empty_3x3x3")
    public static void transportationRemembersCircleAndRechecksAtArrival(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(1, 25, 1));
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var origin = net.minecraft.world.phys.Vec3.atBottomCenterOf(center.offset(4, 0, 0));
        caster.setPos(origin);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        var stone = EBBlocks.TRANSPORTATION_STONE.get().defaultBlockState();
        for (var point : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            level.setBlockAndUpdate(point.below(), Blocks.STONE.defaultBlockState());
            if (!point.equals(center)) level.setBlockAndUpdate(point, stone);
        }
        var corner = center.offset(1, 0, 1);
        var spell = com.binaris.wizardry.content.spell.sorcery.Transportation.class.cast(Spells.TRANSPORTATION);
        helper.assertTrue(spell.getTier() == com.binaris.wizardry.setup.registries.SpellTiers.ADVANCED
                && spell.getElement() == com.binaris.wizardry.setup.registries.Elements.SORCERY
                && spell.getCost() == 100 && spell.getChargeUp() == 20 && spell.getCooldown() == 100,
                "Transportation did not load the original release metadata");
        helper.assertTrue(!spell.cast(context), "Transportation cast with no remembered circle");
        com.binaris.wizardry.content.spell.sorcery.Transportation.remember(level, corner, caster);
        helper.assertTrue(spell.cast(context) && !spell.cast(context), "Transportation did not start one countdown");
        var tick = new com.binaris.wizardry.api.content.event.EBLivingTick(caster, level);
        for (int i = 0; i < 74; i++) com.binaris.wizardry.core.DataEvents.onPlayerTick(tick);
        helper.assertTrue(caster.position().equals(origin), "Transportation moved the caster before 75 ticks");
        level.setBlockAndUpdate(corner, Blocks.AIR.defaultBlockState());
        com.binaris.wizardry.core.DataEvents.onPlayerTick(tick);
        helper.assertTrue(caster.position().equals(origin) && !spell.cast(context), "Transportation used a broken circle");
        level.setBlockAndUpdate(corner, stone);
        helper.assertTrue(spell.cast(context), "Transportation did not reuse a repaired circle");
        for (int i = 0; i < 75; i++) com.binaris.wizardry.core.DataEvents.onPlayerTick(tick);
        helper.assertTrue(caster.position().equals(net.minecraft.world.phys.Vec3.atBottomCenterOf(center))
                && caster.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "Transportation did not arrive with blindness");
        for (var point : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            level.setBlockAndUpdate(point, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(point.below(), Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void forcefieldDeflectsAndBlocksCrossBoundaryDamage(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 22, 1)));
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        caster.setPos(origin);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(Spells.FORCEFIELD.cast(context) && !Spells.FORCEFIELD.cast(context),
                "Forcefield did not spawn or allowed an overlapping copy");
        var region = new net.minecraft.world.phys.AABB(origin, origin).inflate(8);
        var field = level.getEntities(com.binaris.wizardry.setup.registries.EBEntities.FORCEFIELD.get(), region, entity -> true).getFirst();
        helper.assertTrue(field.radius() == 3 && field.getLifetime() == 600, "Forcefield had the wrong radius or lifetime");
        var arrow = net.minecraft.world.entity.EntityType.ARROW.create(level);
        arrow.setPos(origin.add(4, 0, 0));
        arrow.setDeltaMovement(-2, 0, 0);
        level.addFreshEntity(arrow);
        field.tick();
        helper.assertTrue(arrow.getDeltaMovement().x > 0, "Forcefield failed to deflect an incoming arrow");
        arrow.discard();

        var protectedMob = net.minecraft.world.entity.EntityType.COW.create(level);
        protectedMob.setPos(origin.add(1, 0, 0));
        level.addFreshEntity(protectedMob);
        var attacker = net.minecraft.world.entity.EntityType.COW.create(level);
        attacker.setPos(origin.add(5, 0, 0));
        level.addFreshEntity(attacker);
        helper.assertTrue(!protectedMob.hurt(level.damageSources().mobAttack(attacker), 4) && protectedMob.getHealth() == 10,
                "Outside damage crossed Forcefield");
        helper.assertTrue(com.binaris.wizardry.content.entity.construct.ForcefieldConstruct.crosses(caster, attacker.position()),
                "Forcefield failed to identify cross-boundary interaction");
        level.explode(null, origin.x + 4, origin.y, origin.z, 4, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        helper.assertTrue(protectedMob.getHealth() == 10, "Outside explosion damaged inside Forcefield");
        attacker.setPos(origin.add(-1, 0, 0));
        helper.assertTrue(protectedMob.hurt(level.damageSources().mobAttack(attacker), 4) && protectedMob.getHealth() == 6,
                "Forcefield blocked ordinary damage within the same field");
        field.discard();
        attacker.discard();
        protectedMob.discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void slowTimePausesNearbyTicksAndReleasesOnExpiry(GameTestHelper helper) {
        var level = helper.getLevel();
        var point = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 22, 1)));
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        caster.setPos(point);
        var arrow = net.minecraft.world.entity.EntityType.ARROW.create(level);
        arrow.setPos(point.add(3, 0, 0));
        arrow.setNoGravity(true);
        level.addFreshEntity(arrow);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(Spells.SLOW_TIME.cast(context), "Slow Time failed to cast");
        var effect = caster.getEffect(EBMobEffects.SLOW_TIME.get());
        helper.assertTrue(effect != null && effect.getDuration() == 300, "Slow Time had the wrong duration");
        caster.tickCount = 1;
        com.binaris.wizardry.content.effect.SlowTimeMobEffect.prepare(level);
        int originalAge = arrow.tickCount;
        level.tickNonPassenger(arrow);
        helper.assertTrue(arrow.tickCount == originalAge, "Nearby arrow still ticked during a paused frame");
        helper.assertTrue(!com.binaris.wizardry.content.effect.SlowTimeMobEffect.blocks(caster), "Slow Time blocked its caster");

        caster.tickCount = 5;
        com.binaris.wizardry.content.effect.SlowTimeMobEffect.prepare(level);
        level.tickNonPassenger(arrow);
        helper.assertTrue(arrow.tickCount == originalAge + 1, "Nearby entity did not get its sixth-frame tick");
        caster.tickCount = 1;
        var integration = com.binaris.wizardry.core.integrations.ArtifactChannel.getIntegration();
        if (integration == com.binaris.wizardry.core.integrations.VanillaArtifactIntegration.INSTANCE) {
            caster.getInventory().setItem(1, new ItemStack(EBItems.CHARM_STOP_TIME.get()));
            caster.tickCount = 5;
            com.binaris.wizardry.content.effect.SlowTimeMobEffect.prepare(level);
            helper.assertTrue(com.binaris.wizardry.content.effect.SlowTimeMobEffect.blocks(arrow), "Stop Time charm allowed a sixth-frame tick");
        }
        caster.removeEffect(EBMobEffects.SLOW_TIME.get());
        com.binaris.wizardry.content.effect.SlowTimeMobEffect.prepare(level);
        level.tickNonPassenger(arrow);
        helper.assertTrue(arrow.tickCount == originalAge + 2, "Expired Slow Time left its target paused");
        arrow.discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void iceStatueReleasesTheSameMobWithItsEquipment(GameTestHelper helper) {
        var level = helper.getLevel();
        var start = helper.absolutePos(new BlockPos(1, 22, 1));
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(start));
        caster.setYRot(-90);
        var target = start.east(4);
        level.setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        zombie.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(target));
        zombie.setNoAi(true);
        var helmet = net.minecraft.world.entity.EquipmentSlot.HEAD;
        zombie.setItemSlot(helmet, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
        zombie.setHealth(14);
        level.addFreshEntity(zombie);
        var uuid = zombie.getUUID();
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0,
                new SpellModifiers().multiply(SpellModifiers.DURATION, 0.02f));
        helper.assertTrue(Spells.ICE_STATUE.cast(context) && zombie.isRemoved(), "Ice Statue did not store the struck mob");
        helper.assertTrue(level.getBlockState(target).is(EBBlocks.ICE_STATUE.get())
                && level.getBlockState(target.above()).is(EBBlocks.ICE_STATUE.get()), "Ice Statue did not fill both parts");
        helper.runAfterDelay(18, () -> {
            var restored = level.getEntity(uuid);
            helper.assertTrue(restored instanceof net.minecraft.world.entity.Mob mob && mob.getHealth() == 14
                            && mob.getItemBySlot(helmet).is(net.minecraft.world.item.Items.IRON_HELMET),
                    "Thawing lost the original UUID, health, or equipment");
            helper.assertTrue(level.isEmptyBlock(target) && level.isEmptyBlock(target.above()), "Thaw left a statue part behind");
            restored.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty_3x3x3")
    public static void petrifyRejectsAnObstructedMobAndReleasesFromUpperPart(GameTestHelper helper) {
        var level = helper.getLevel();
        var start = helper.absolutePos(new BlockPos(1, 22, 1));
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(start));
        caster.setYRot(-90);
        var target = start.east(4);
        level.setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        zombie.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(target));
        zombie.setNoAi(true);
        level.addFreshEntity(zombie);
        var uuid = zombie.getUUID();
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        level.setBlockAndUpdate(target.above(), Blocks.STONE.defaultBlockState());
        Spells.PETRIFY.cast(context);
        helper.assertTrue(!zombie.isRemoved() && level.isEmptyBlock(target), "Obstructed conversion modified the mob or lower block");

        level.removeBlock(target.above(), false);
        helper.assertTrue(Spells.PETRIFY.cast(context) && zombie.isRemoved(), "Petrify failed after clearing the obstruction");
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(level.getBlockState(target).is(EBBlocks.PETRIFIED_STONE.get())
                    && level.getBlockState(target.above()).is(EBBlocks.PETRIFIED_STONE.get()), "Petrify expired before its minimum duration");
            level.removeBlock(target.above(), false);
        });
        helper.runAfterDelay(24, () -> {
            var restored = level.getEntity(uuid);
            helper.assertTrue(restored != null && level.isEmptyBlock(target), "Breaking the upper part failed to release the original mob");
            restored.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty_3x3x3")
    public static void wallOfFrostBuildsTwoBlocksAndThawsWithoutWater(GameTestHelper helper) {
        var level = helper.getLevel();
        var start = helper.absolutePos(new BlockPos(1, 22, 1));
        for (var pos : BlockPos.betweenClosed(start.offset(-1, -1, -1), start.offset(8, -1, 1))) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(start));
        caster.setYRot(-90);
        caster.setXRot(25);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0,
                new SpellModifiers().multiply(SpellModifiers.DURATION, 0.02f));
        var ice = EBBlocks.DRY_FROSTED_ICE.get();
        helper.assertTrue(Spells.WALL_OF_FROST.cast(context), "Wall of Frost ray failed to channel");
        var wall = start.east(3);
        helper.assertTrue(level.getBlockState(wall).is(ice) && level.getBlockState(wall.above()).is(ice),
                "Wall of Frost did not place two blocks");
        var thaw = (com.binaris.wizardry.content.block.DryFrostedIceBlock) ice;
        var random = net.minecraft.util.RandomSource.create(21);
        for (var position : java.util.List.of(wall, wall.above())) {
            for (int attempt = 0; attempt < 40 && level.getBlockState(position).is(ice); attempt++) {
                thaw.tick(level.getBlockState(position), level, position, random);
            }
        }
        helper.assertTrue(level.isEmptyBlock(wall) && level.isEmptyBlock(wall.above()), "Frost wall did not thaw cleanly");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void earthquakeThrowsItsWaveAndPreservesTerrainSafety(GameTestHelper helper) {
        var level = helper.getLevel();
        var base = helper.absolutePos(new BlockPos(1, 22, 1));
        var start = net.minecraft.world.phys.Vec3.atBottomCenterOf(base);
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        caster.setPos(start);
        caster.setOnGround(true);
        level.addFreshEntity(caster);
        var moveable = base.east(2).below();
        var protectedBlock = base.west(2).below();
        level.setBlockAndUpdate(base.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(moveable, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(moveable.below(), Blocks.DEEPSLATE.defaultBlockState());
        level.setBlockAndUpdate(protectedBlock, Blocks.BEDROCK.defaultBlockState());
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        zombie.setPos(start.add(1.5, 0, 0));
        level.addFreshEntity(zombie);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(Spells.EARTHQUAKE.cast(context), "Earthquake failed to cast from a grounded caster");
        var region = new net.minecraft.world.phys.AABB(base).inflate(5);
        var quake = level.getEntities(com.binaris.wizardry.setup.registries.EBEntities.EARTHQUAKE.get(), region, entity -> true).getFirst();
        helper.assertTrue(quake.getLifetime() >= 19 && quake.getLifetime() <= 20, "Wrong base ring lifetime");
        quake.tickCount = 1;
        quake.tick();
        var weakness = zombie.getEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
        helper.assertTrue(zombie.getHealth() < 12 && zombie.getDeltaMovement().y == 0.8
                        && weakness != null && weakness.getDuration() == 400 && weakness.getAmplifier() == 1,
                "Earthquake did not damage, launch, and weaken a target in the ring");
        helper.assertTrue(caster.getDeltaMovement().lengthSqr() == 0 && level.getBlockState(base.below()).is(Blocks.STONE),
                "Earthquake disturbed the caster or ground at the epicenter");
        helper.assertTrue(level.getBlockState(moveable).isAir() && level.getBlockState(protectedBlock).is(Blocks.BEDROCK),
                "Earthquake did not lift regular stone or touched unbreakable terrain");
        var debris = level.getEntities(com.binaris.wizardry.setup.registries.EBEntities.LEVITATING_BLOCK.get(), region, block -> true).getFirst();
        helper.assertTrue(debris.getDeltaMovement().y == 0.3, "Earthquake debris did not receive the original launch speed");
        for (int i = 0; i < 40 && !debris.isRemoved(); i++) debris.tick();
        helper.assertTrue(debris.isRemoved() && level.getBlockState(moveable).is(Blocks.STONE), "Earthquake debris did not settle safely");
        quake.discard();
        zombie.discard();
        caster.discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void telekinesisMovesOneClosedBarrelWithoutDroppingContents(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var feet = helper.absolutePos(new BlockPos(0, 1, 1));
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(feet));
        caster.setYRot(-90);
        var target = BlockPos.containing(caster.getEyePosition().add(2, -0.25, 0));
        level.setBlockAndUpdate(target.west(), Blocks.AIR.defaultBlockState());
        var state = Blocks.BARREL.defaultBlockState();
        var open = net.minecraft.world.level.block.BarrelBlock.OPEN;
        level.setBlockAndUpdate(target, state.setValue(open, true));
        var barrel = (net.minecraft.world.level.block.entity.BarrelBlockEntity) level.getBlockEntity(target);
        var item = net.minecraft.world.item.Items.EMERALD;
        barrel.setItem(6, new ItemStack(item, 7));
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(!Spells.GREATER_TELEKINESIS.cast(context), "Greater Telekinesis lifted an open barrel");
        level.setBlockAndUpdate(target, state);
        var lock = Services.OBJECT_DATA.getArcaneLockData(barrel);
        lock.setArcaneLockOwner(caster.getUUID().toString());
        helper.assertTrue(!Spells.GREATER_TELEKINESIS.cast(context), "Greater Telekinesis lifted an arcane-locked barrel");
        lock.setArcaneLockOwner(null);
        helper.assertTrue(Spells.GREATER_TELEKINESIS.cast(context) && level.isEmptyBlock(target), "Closed barrel did not lift");
        var area = new net.minecraft.world.phys.AABB(target).inflate(3);
        helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
                dropped -> dropped.getItem().is(item)).isEmpty(), "Removing the barrel duplicated its contents into drops");

        var type = com.binaris.wizardry.setup.registries.EBEntities.LEVITATING_BLOCK.get();
        var carrier = level.getEntities(type, area, block -> true).getFirst();
        var tag = carrier.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        carrier.discard();
        var restored = type.create(level);
        restored.load(tag);
        restored.suspend(caster);
        level.addFreshEntity(restored);
        level.setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
        for (int i = 0; i < 15 && !restored.isRemoved(); i++) restored.tick();
        helper.assertTrue(restored.isRemoved() && level.getBlockEntity(target) instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity,
                "Saved barrel carrier did not land in its original position");
        var placed = (net.minecraft.world.level.block.entity.BarrelBlockEntity) level.getBlockEntity(target);
        helper.assertTrue(placed.getItem(6).is(item) && placed.getItem(6).getCount() == 7, "Landing lost barrel contents");

        helper.assertTrue(Spells.GREATER_TELEKINESIS.cast(context), "Failed to lift the recovered barrel");
        var failed = level.getEntities(type, area, block -> true).getFirst();
        failed.time = 601;
        failed.tick();
        var salvage = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
                entity -> entity.getItem().is(Blocks.BARREL.asItem()));
        helper.assertTrue(failed.isRemoved() && salvage.size() == 1, "Expired carrier did not yield one barrel");
        var contents = salvage.getFirst().getItem().get(net.minecraft.core.component.DataComponents.CONTAINER);
        helper.assertTrue(contents != null && contents.nonEmptyStream().mapToInt(ItemStack::getCount).sum() == 7,
                "Recovered barrel item lost or duplicated its contents");
        salvage.getFirst().discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 125)
    public static void summonPhoenixHoversAndCastsFlameRay(GameTestHelper helper) {
        var level = helper.getLevel();
        var base = helper.absolutePos(new BlockPos(1, 52, 1));
        for (var ground : BlockPos.betweenClosed(base.offset(-2, -1, -2), base.offset(7, -1, 2))) {
            level.setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());
        }
        var origin = net.minecraft.world.phys.Vec3.atBottomCenterOf(base);
        var cast = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.EAST, 0, 0, new SpellModifiers());
        helper.assertTrue(Spells.SUMMON_PHOENIX.cast(cast), "Summon Phoenix failed from its location-cast path");
        var birds = level.getEntities(com.binaris.wizardry.setup.registries.EBEntities.PHOENIX.get(),
                new net.minecraft.world.phys.AABB(base).inflate(5), entity -> true);
        helper.assertTrue(birds.size() == 1, "Expected one phoenix");
        var phoenix = birds.getFirst();
        // Embedded players in neighboring fixtures must not cause an unrelated natural despawn.
        phoenix.setPersistenceRequired();
        helper.assertTrue(phoenix.fireImmune() && phoenix.getMaxHealth() == 30
                && Services.OBJECT_DATA.getMinionData(phoenix).getLifetime() == 600,
                "Phoenix did not have its original fire immunity, health, or lifetime");
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        zombie.setPos(origin.add(5, 0, 0));
        zombie.setNoAi(true);
        zombie.setPersistenceRequired();
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
        level.addFreshEntity(zombie);
        phoenix.setTarget(zombie);
        helper.runAfterDelay(95, () -> {
            helper.assertTrue(phoenix.getY() >= origin.y + 1.9 && phoenix.getY() <= origin.y + 3.1,
                    "Phoenix did not settle into the two-to-three block hover band: height=" + (phoenix.getY() - origin.y)
                            + ", ticks=" + phoenix.tickCount + ", removed=" + phoenix.isRemoved());
            helper.assertTrue(phoenix.getContinuousSpell() == Spells.FLAME_RAY && zombie.getHealth() < 20,
                    "Phoenix did not channel Flame Ray at its nearby target");
            phoenix.discard();
            zombie.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty_3x3x3")
    public static void miragePreventsMobTargetAcquisition(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        zombie.setTarget(player);
        helper.assertTrue(zombie.getTarget() == player, "Fixture failed to acquire the player");
        var cast = new PlayerCastContext(level, player, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(Spells.MIRAGE.cast(cast), "Mirage failed to cast");
        var effect = player.getEffect(EBMobEffects.MIRAGE.get());
        helper.assertTrue(effect != null && effect.getDuration() == 400 && !effect.isVisible(),
                "Mirage has the wrong duration or reveals the actual caster with effect particles");
        zombie.setTarget(player);
        helper.assertTrue(zombie.getTarget() == null, "Mirage did not hide the target from mob acquisition");
        player.removeEffect(EBMobEffects.MIRAGE.get());
        zombie.setTarget(player);
        helper.assertTrue(zombie.getTarget() == player, "Mirage kept blocking after removal");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void decoySplitsAndDrawsAggroFromItsCaster(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var start = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 22, 1)));
        caster.setPos(start);
        caster.setYRot(-90);
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        zombie.setPos(start.add(3, 0, 0));
        level.addFreshEntity(zombie);
        zombie.setTarget(caster);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0,
                new SpellModifiers().multiply(SpellModifiers.POTENCY, 2));
        helper.assertTrue(Spells.DECOY.cast(context), "Decoy failed to spawn");
        var decoys = level.getEntities(com.binaris.wizardry.setup.registries.EBEntities.DECOY.get(),
                caster.getBoundingBox().inflate(6), entity -> true);
        helper.assertTrue(decoys.size() == 1, "Cast spawned multiple decoys");
        var decoy = decoys.getFirst();
        helper.assertTrue(caster.getUUID().equals(decoy.getOwnerUUID()) && decoy.isCustomNameVisible()
                && zombie.getTarget() == decoy, "Decoy did not copy caster identity and draw aggro");
        helper.assertTrue(Math.abs(decoy.getDeltaMovement().z) > 0.25
                && Math.abs(decoy.getDeltaMovement().z + caster.getDeltaMovement().z) < 0.0001,
                "Caster and decoy did not split in opposite directions");
        helper.assertTrue(!decoy.hurt(level.damageSources().mobAttack(zombie), 100), "Decoy was damageable");
        var data = decoy.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        data.putInt("DecoyRemaining", 1);
        decoy.load(data);
        decoy.tick();
        helper.assertTrue(decoy.isRemoved(), "Decoy failed to expire");
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void mindControlRedirectsAndClearsTargets(GameTestHelper helper) {
        var level = helper.getLevel();
        var start = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 22, 1)));
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        caster.setPos(start);
        caster.setYRot(-90);
        level.addFreshEntity(caster);
        var controlled = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        controlled.setPos(start.add(4, 0, 0));
        level.addFreshEntity(controlled);
        var enemy = net.minecraft.world.entity.EntityType.COW.create(level);
        enemy.setPos(start.add(4, 0, 3));
        level.addFreshEntity(enemy);
        controlled.setTarget(caster);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(Spells.MIND_CONTROL.cast(context), "Mind Control ray missed its target");
        var effect = controlled.getEffect(EBMobEffects.MIND_CONTROL.get());
        helper.assertTrue(effect != null && effect.getDuration() == 600 && controlled.getTarget() == enemy,
                "Mind Control failed to install a tenable target and original duration");
        controlled.setTarget(caster);
        helper.assertTrue(controlled.getTarget() == enemy, "Controlled mob reacquired its controller");
        var saved = controlled.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var restored = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        restored.load(saved);
        helper.assertTrue(caster.getUUID().equals(((com.binaris.wizardry.content.effect.MindControlMobEffect.ControllerAccess)
                restored).ebwizardry$getController()), "Mind Control lost the controller UUID on reload");
        controlled.removeEffect(EBMobEffects.MIND_CONTROL.get());
        helper.assertTrue(controlled.getTarget() == null, "Removing Mind Control left a stale target");
        controlled.setTarget(caster);
        helper.assertTrue(controlled.getTarget() == caster, "Expired Mind Control still filtered attacks");
        caster.discard();
        controlled.discard();
        enemy.discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void grappleReelsPlayerToBlockAndEntityToDispenser(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var start = helper.absolutePos(new BlockPos(1, 22, 1));
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(start));
        caster.setYRot(-90);
        caster.setXRot(0);
        var anchor = start.offset(7, 1, 0);
        level.setBlockAndUpdate(anchor, Blocks.STONE.defaultBlockState());
        var modifiers = new SpellModifiers();
        var opening = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 1, modifiers);
        var channel = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 3, modifiers);
        helper.assertTrue(Spells.GRAPPLE.cast(opening) && caster.getDeltaMovement().lengthSqr() == 0,
                "Grapple reeled the player before its vine reached the block");
        helper.assertTrue(Spells.GRAPPLE.cast(channel) && caster.getDeltaMovement().x > 0.25,
                "Grapple did not pull the player to the anchored block");
        level.removeBlock(anchor, false);
        helper.assertTrue(!Spells.GRAPPLE.cast(channel), "Grapple remained anchored to a removed block");
        Spells.GRAPPLE.endCast(channel);

        var cow = net.minecraft.world.entity.EntityType.COW.create(level);
        cow.setPos(caster.position().add(7, 0, 0));
        level.addFreshEntity(cow);
        var origin = caster.position().add(0, 0.6, 0);
        var location = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.EAST, 3, 20, modifiers);
        helper.assertTrue(Spells.GRAPPLE.cast(location) && cow.getDeltaMovement().x < -0.25,
                "Dispenser Grapple did not pull the target toward the origin");
        cow.discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void sixthSenseStoresDurationAndRangeUpgrade(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 22, 1))));
        var modifiers = new SpellModifiers().multiply(SpellModifiers.RANGE, 1.5f).multiply(SpellModifiers.DURATION, 1.25f);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, modifiers);
        helper.assertTrue(Spells.SIXTH_SENSE.cast(context), "Sixth Sense failed to cast");
        var effect = caster.getEffect(EBMobEffects.SIXTH_SENSE.get());
        helper.assertTrue(effect != null && effect.getDuration() == 500 && effect.getAmplifier() == 2,
                "Sixth Sense did not store its adjusted duration and range level");
        helper.assertTrue(com.binaris.wizardry.content.spell.earth.SixthSense.radius(effect.getAmplifier()) == 30,
                "Two range upgrades did not extend the original twenty-block radius to thirty");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void transienceBlocksDamageAndWorldInteractions(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var base = helper.absolutePos(new BlockPos(1, 22, 1));
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(base));
        var cast = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(Spells.TRANSIENCE.cast(cast) && !Spells.TRANSIENCE.cast(cast), "Transience failed or allowed refreshing itself");
        var effect = caster.getEffect(EBMobEffects.TRANSIENCE.get());
        var invisibility = caster.getEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
        helper.assertTrue(effect != null && effect.getDuration() == 400 && invisibility != null && invisibility.getDuration() == 400,
                "Transience did not apply the paired 400-tick effects");

        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        target.setPos(caster.position().add(1, 0, 0));
        level.addFreshEntity(target);
        helper.assertTrue(!caster.hurt(level.damageSources().mobAttack(target), 4) && caster.getHealth() == 20,
                "Transient player took ordinary damage");
        helper.assertTrue(!target.hurt(level.damageSources().playerAttack(caster), 4) && target.getHealth() == 10,
                "Transient player could deal damage");
        helper.assertTrue(caster.interactOn(target, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.FAIL,
                "Transient player could interact with a mob");
        helper.assertTrue(caster.hurt(level.damageSources().fellOutOfWorld(), 2) && caster.getHealth() == 18,
                "Transience hid void damage");

        caster.removeEffect(EBMobEffects.TRANSIENCE.get());
        caster.invulnerableTime = 0;
        helper.assertTrue(caster.hurt(level.damageSources().mobAttack(target), 4) && caster.getHealth() == 14,
                "Damage immunity persisted after effect removal");
        target.discard();
        caster.discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void blackHolePullsHostilesAndExpires(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 22, 1)));
        var caster = net.minecraft.world.entity.EntityType.COW.create(level);
        caster.setPos(pos.add(-1, 0, 0));
        level.addFreshEntity(caster);
        var victim = net.minecraft.world.entity.EntityType.COW.create(level);
        victim.setPos(pos.add(3, 0, 0));
        level.addFreshEntity(victim);
        var hole = new com.binaris.wizardry.content.entity.construct.BlackHoleConstruct(
                com.binaris.wizardry.setup.registries.EBEntities.BLACK_HOLE.get(), level);
        hole.setPos(pos);
        hole.setCaster(caster);
        hole.setLifetime(400);
        hole.tick();
        helper.assertTrue(victim.getDeltaMovement().x < 0 && victim.getHealth() == 10,
                "Black Hole should pull outside its two-block damage radius");
        helper.assertTrue(caster.getDeltaMovement().lengthSqr() == 0 && caster.getHealth() == 10,
                "Black Hole affected its own caster");
        victim.setPos(pos.add(1, 0, 0));
        hole.tick();
        helper.assertTrue(victim.getHealth() == 8, "Black Hole did not deal two damage near its center");
        hole.tickCount = 401;
        hole.tick();
        helper.assertTrue(hole.isRemoved(), "Black Hole did not expire");
        victim.discard();
        caster.discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void forestOfThornsGrowsRingAndExpiresInPairs(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var base = helper.absolutePos(new BlockPos(1, 22, 1));
        for (var pos : BlockPos.betweenClosed(base.offset(-4, -1, -4), base.offset(4, -1, 4))) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(base));
        var ring = base.offset(2, 0, 0);
        var protectedColumn = base.offset(-2, 0, 0);
        level.setBlockAndUpdate(protectedColumn.above(), Blocks.CHEST.defaultBlockState());
        var modifiers = new SpellModifiers().multiply(SpellModifiers.DURATION, 0.04f);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, modifiers);
        helper.assertTrue(caster.mayBuild() && level.mayInteract(caster, ring), "Mock player lacks permission in thorns fixture");
        var surface = com.binaris.wizardry.api.content.util.BlockUtil.getNearestFloor(level, ring, 3);
        helper.assertTrue(surface != null && surface == ring.getY(), "Thorns fixture found floor " + surface + " instead of " + ring.getY());
        helper.assertTrue(Spells.FOREST_OF_THORNS.cast(context), "Forest of Thorns failed to cast");
        var thorns = EBBlocks.THORNS.get();
        helper.assertTrue(level.getBlockState(ring).is(thorns) && level.getBlockState(ring.above()).is(thorns)
                && !level.getBlockState(base).is(thorns), "Spell did not place a two-high ring");
        helper.assertTrue(level.getBlockState(protectedColumn.above()).is(Blocks.CHEST)
                && !level.getBlockState(protectedColumn).is(thorns), "Thorns displaced a protected container column");
        var data = (com.binaris.wizardry.content.blockentity.ThornsBlockEntity) level.getBlockEntity(ring);
        helper.assertTrue(caster.getUUID().equals(data.caster()) && data.duration() > 14, "Thorns lost owner or duration");

        helper.runAfterDelay(16, () -> helper.assertTrue(level.getBlockState(ring)
                .getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7) == 7,
                "Thorns failed to grow through all eight stages"));
        helper.runAfterDelay(27, () -> {
            helper.assertTrue(level.getBlockState(ring).isAir() && level.getBlockState(ring.above()).isAir(),
                    "Thorns did not expire as a pair");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_3x3x3")
    public static void clairvoyanceRemembersAndPathsToClickedFace(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var start = helper.absolutePos(new BlockPos(1, 22, 1));
        var end = start.offset(4, 0, 0);
        for (var block : BlockPos.betweenClosed(start.offset(-1, -1, -1), end.offset(1, -1, 1))) {
            level.setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
        }
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(start));
        var cast = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(!Spells.CLAIRVOYANCE.cast(cast), "Clairvoyance accepted a missing destination");

        var scroll = new ItemStack(EBItems.SCROLL.get());
        RegistryUtils.setSpell(scroll, Spells.CLAIRVOYANCE);
        caster.setItemInHand(InteractionHand.MAIN_HAND, scroll);
        caster.setShiftKeyDown(true);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atBottomCenterOf(end),
                Direction.UP, end.below(), false);
        var use = new net.minecraft.world.item.context.UseOnContext(caster, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(scroll.getItem().useOn(use).consumesAction() && scroll.getCount() == 1,
                "Sneak click failed to remember its adjacent face without consuming the scroll");
        caster.setShiftKeyDown(false);

        var data = Services.OBJECT_DATA.getSpellManagerData(caster);
        var key = com.binaris.wizardry.content.spell.sorcery.Clairvoyance.DESTINATION;
        helper.assertTrue(net.minecraft.core.GlobalPos.of(level.dimension(), end).equals(data.getVariable(key)),
                "Clairvoyance did not save the point and dimension");
        helper.assertTrue(Spells.CLAIRVOYANCE.cast(cast), "Clairvoyance failed to reach a traversable destination");
        var path = com.binaris.wizardry.content.spell.sorcery.Clairvoyance.findPath(level, start, end, 12);
        helper.assertTrue(!path.isEmpty() && path.getLast().equals(end), "Clairvoyance produced a partial path");
        helper.assertTrue(com.binaris.wizardry.content.spell.sorcery.Clairvoyance.findPath(level, start, end, 2).isEmpty(),
                "Clairvoyance ignored cast range");
        data.setVariable(key, net.minecraft.core.GlobalPos.of(net.minecraft.world.level.Level.NETHER, end));
        helper.assertTrue(!Spells.CLAIRVOYANCE.cast(cast), "Clairvoyance crossed dimensions");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void radiantTotemPrioritizesWoundedOwner(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 22, 1)));
        var owner = net.minecraft.world.entity.EntityType.COW.create(level);
        owner.setPos(pos.add(3, 0, 0));
        level.addFreshEntity(owner);
        owner.setHealth(5);
        owner.tickCount = 8;
        var victim = net.minecraft.world.entity.EntityType.COW.create(level);
        victim.setPos(pos.add(1, 0, 0));
        level.addFreshEntity(victim);
        victim.tickCount = 1;
        var totem = new com.binaris.wizardry.content.entity.construct.TotemConstruct(
                com.binaris.wizardry.setup.registries.EBEntities.RADIANT_TOTEM.get(), level);
        totem.setPos(pos);
        totem.setCaster(owner);
        totem.tick();
        helper.assertTrue(owner.getHealth() == 6 && victim.getHealth() == 10, "Radiant Totem did not prioritize wounded owner");
        owner.setHealth(owner.getMaxHealth());
        totem.tick();
        helper.assertTrue(victim.getHealth() == 9 && owner.getHealth() == 10, "Radiant Totem did not damage the eligible target");
        totem.discard();
        owner.discard();
        victim.discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void witheringTotemStoresAndReleasesDrainedHealth(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 22, 1)));
        var victim = net.minecraft.world.entity.EntityType.COW.create(level);
        victim.setPos(pos.add(1, 0, 0));
        level.addFreshEntity(victim);
        var totem = new com.binaris.wizardry.content.entity.construct.TotemConstruct(
                com.binaris.wizardry.setup.registries.EBEntities.WITHERING_TOTEM.get(), level);
        totem.setPos(pos);
        victim.tickCount = 1;
        for (int i = 0; i < 5; i++) {
            victim.invulnerableTime = 0;
            totem.tick();
        }
        helper.assertTrue(totem.healthDrained() == 5 && victim.getHealth() == 5, "Withering Totem did not store five one-point drains");
        totem.tickCount = 5;
        var saved = totem.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var restored = new com.binaris.wizardry.content.entity.construct.TotemConstruct(totem.getType(), level);
        restored.load(saved);
        helper.assertTrue(restored.healthDrained() == 5 && restored.tickCount == 5, "Reload lost drained health or age");
        victim.invulnerableTime = 0;
        restored.tickCount = restored.getLifetime();
        restored.tick();
        var wither = victim.getEffect(net.minecraft.world.effect.MobEffects.WITHER);
        helper.assertTrue(restored.isRemoved() && victim.getHealth() == 4 && wither != null && wither.getDuration() == 120,
                "Expiry did not spend stored health on a one-point burst with six seconds of Wither");
        totem.discard();
        victim.discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void disintegrationSpawnsOneLethalEmberBurst(GameTestHelper helper) {
        var level = helper.getLevel();
        var point = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 22, 1)));
        var victim = net.minecraft.world.entity.EntityType.COW.create(level);
        victim.setPos(point);
        level.addFreshEntity(victim);
        var origin = point.add(-3, 0.8, 0);
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.EAST, 0, 0, new SpellModifiers());
        var bounds = victim.getBoundingBox().inflate(4);
        var type = com.binaris.wizardry.setup.registries.EBEntities.EMBER.get();
        helper.assertTrue(Spells.DISINTEGRATION.cast(context) && victim.getHealth() == 2
                && victim.getRemainingFireTicks() == 200, "Nonlethal beam should deal eight damage and ignite for ten seconds");
        helper.assertTrue(level.getEntities(type, bounds, ember -> true).isEmpty(), "Nonlethal cast spawned embers");

        victim.invulnerableTime = 0;
        helper.assertTrue(Spells.DISINTEGRATION.cast(context) && victim.isDeadOrDying() && !victim.isOnFire(),
                "Lethal beam failed to extinguish and disintegrate its target");
        com.binaris.wizardry.content.spell.fire.Disintegration.spawnEmbers(victim, null);
        var embers = level.getEntities(type, bounds, ember -> true);
        helper.assertTrue(embers.size() == 12, "Lethal cast should generate exactly twelve embers once");
        var ember = embers.getFirst();
        var contact = net.minecraft.world.entity.EntityType.COW.create(level);
        contact.setPos(ember.position());
        level.addFreshEntity(contact);
        ember.tick();
        helper.assertTrue(contact.isOnFire(), "Ember contact did not ignite a living target");
        ember.tickCount = ember.getLifeTime() + 1;
        ember.tick();
        helper.assertTrue(ember.isRemoved(), "Ember failed to expire");
        embers.forEach(entity -> entity.discard());
        contact.discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void shadowWardSplitsBlockableDamageDuringChannelOnly(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 22, 1))));
        var scroll = new ItemStack(EBItems.SCROLL.get());
        RegistryUtils.setSpell(scroll, Spells.SHADOW_WARD);
        caster.setItemInHand(InteractionHand.MAIN_HAND, scroll);
        caster.startUsingItem(InteractionHand.MAIN_HAND);
        helper.assertTrue(com.binaris.wizardry.api.content.util.EntityUtil.isCasting(caster, Spells.SHADOW_WARD),
                "Fixture did not start channeling the ward");

        var attacker = net.minecraft.world.entity.EntityType.COW.create(level);
        attacker.setPos(caster.position().add(2, 0, 0));
        level.addFreshEntity(attacker);
        var source = level.damageSources().mobAttack(attacker);
        caster.hurt(source, 8);
        helper.assertTrue(caster.getHealth() == 16 && attacker.getHealth() == 6,
                "Ward failed to split eight damage evenly, caster " + caster.getHealth() + ", attacker " + attacker.getHealth());
        caster.invulnerableTime = 0;
        caster.hurt(level.damageSources().magic(), 2);
        helper.assertTrue(caster.getHealth() == 14 && attacker.getHealth() == 6, "Ward intercepted unattributed magic");
        caster.stopUsingItem();
        caster.invulnerableTime = 0;
        caster.hurt(source, 4);
        helper.assertTrue(caster.getHealth() == 10 && attacker.getHealth() == 6, "Ward remained active after channeling stopped");
        attacker.discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void shieldRefreshesResistanceAndDeflectsArrows(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var base = helper.absolutePos(new BlockPos(1, 22, 1));
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(base));
        caster.setYRot(0);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(Spells.SHIELD.cast(context) && Spells.SHIELD.cast(context), "Shield failed to channel");
        var resistance = caster.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
        helper.assertTrue(resistance != null && resistance.getDuration() == 10 && resistance.getAmplifier() == 0,
                "Shield did not grant ten ticks of Resistance I");
        var shields = level.getEntities(com.binaris.wizardry.setup.registries.EBEntities.SHIELD.get(),
                caster.getBoundingBox().inflate(3), shield -> true);
        helper.assertTrue(shields.size() == 1, "Channel spawned duplicate shields");
        var shield = shields.getFirst();
        var arrow = net.minecraft.world.entity.EntityType.ARROW.create(level);
        arrow.setPos(shield.getX(), shield.getY() + 0.7, shield.getZ() + 2);
        arrow.setDeltaMovement(0, 0, -1.7);
        level.addFreshEntity(arrow);
        arrow.tick();
        helper.assertTrue(arrow.getDeltaMovement().z > 0, "Shield did not reverse the incoming arrow");
        arrow.discard();
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(shield.isRemoved(), "Shield survived after channeling stopped");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_3x3x3")
    public static void lightningWebChainsAcrossThreeDamageTiers(GameTestHelper helper) {
        var level = helper.getLevel();
        var first = helper.absolutePos(new BlockPos(1, 20, 1));
        var center = net.minecraft.world.phys.Vec3.atBottomCenterOf(first);
        var primary = net.minecraft.world.entity.EntityType.COW.create(level);
        var secondary = net.minecraft.world.entity.EntityType.COW.create(level);
        var tertiary = net.minecraft.world.entity.EntityType.COW.create(level);
        primary.setPos(center);
        secondary.setPos(center.add(0, 0, 4));
        tertiary.setPos(center.add(0, 0, 8));
        var victims = java.util.List.of(primary, secondary, tertiary);
        victims.forEach(level::addFreshEntity);
        var origin = center.add(-3, 0.8, 0);
        var early = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.EAST, 9, 30, new SpellModifiers());
        var pulse = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.EAST, 10, 30, new SpellModifiers());
        helper.assertTrue(Spells.LIGHTNING_WEB.cast(early) && victims.stream().allMatch(victim -> victim.getHealth() == 10),
                "Lightning Web pulsed early");
        helper.assertTrue(Spells.LIGHTNING_WEB.cast(pulse) && primary.getHealth() == 5
                && secondary.getHealth() == 6 && tertiary.getHealth() == 7,
                "Lightning Web failed the 5/4/3 damage chain");
        victims.forEach(victim -> victim.discard());
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void lightningHammerFallsStrikesAndExpires(GameTestHelper helper) {
        var level = helper.getLevel();
        var base = helper.absolutePos(new BlockPos(1, 20, 1));
        for (var pos : BlockPos.betweenClosed(base.offset(-4, 0, -4), base.offset(4, 0, 4))) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        var top = net.minecraft.world.phys.Vec3.atBottomCenterOf(base.above());
        var origin = top.add(0, 1.5, 0);
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.DOWN, 0, 0, new SpellModifiers());
        level.setBlockAndUpdate(base.above(4), Blocks.STONE.defaultBlockState());
        helper.assertTrue(!Spells.LIGHTNING_HAMMER.cast(context), "Hammer should reject a roof over its landing site");
        level.setBlockAndUpdate(base.above(4), Blocks.AIR.defaultBlockState());
        helper.assertTrue(Spells.LIGHTNING_HAMMER.cast(context), "Hammer did not cast outdoors");
        var hammers = level.getEntities(com.binaris.wizardry.setup.registries.EBEntities.LIGHTNING_HAMMER.get(),
                new net.minecraft.world.phys.AABB(base).inflate(55), entity -> true);
        helper.assertTrue(hammers.size() == 1, "Expected exactly one hammer");
        var hammer = hammers.getFirst();
        helper.assertTrue(hammer.getY() == top.y + 50 && hammer.getLifetime() == 600, "Wrong hammer spawn or lifetime");
        for (int i = 0; i < 40 && !hammer.onGround(); i++) hammer.tick();
        helper.assertTrue(hammer.onGround(), "Hammer failed to land");
        var bolts = level.getEntities(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT,
                hammer.getBoundingBox().inflate(2), entity -> true);
        helper.assertTrue(bolts.size() == 1, "Hard impact did not summon exactly one lightning bolt");
        bolts.forEach(bolt -> bolt.discard());

        var victim = net.minecraft.world.entity.EntityType.COW.create(level);
        victim.setPos(top.add(2, 0, 0));
        level.addFreshEntity(victim);
        victim.tickCount = 39;
        hammer.tick();
        helper.assertTrue(victim.getHealth() == 10, "Hammer struck before the target's attack interval");
        victim.tickCount = 40;
        hammer.tick();
        helper.assertTrue(victim.getHealth() == 4, "Ground strike did not deal six damage");
        hammer.tickCount = 600;
        hammer.tick();
        helper.assertTrue(hammer.isRemoved(), "Hammer failed to expire");
        victim.discard();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void lightningHammerTransfersOnlyToItsOwner(GameTestHelper helper) {
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        var other = helper.makeMockPlayer(GameType.SURVIVAL);
        var level = helper.getLevel();
        var hammer = new com.binaris.wizardry.content.entity.construct.LightningHammerConstruct(level);
        hammer.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))));
        hammer.setCaster(owner);
        hammer.setLifetime(51);
        hammer.damageMultiplier = 1.5f;
        var items = com.binaris.wizardry.core.integrations.ArtifactChannel.getIntegration();
        boolean hotbar = items == com.binaris.wizardry.core.integrations.VanillaArtifactIntegration.INSTANCE;
        if (hotbar) owner.getInventory().setItem(1, new ItemStack(EBItems.RING_HAMMER.get()));
        hammer.tickCount = 10;
        helper.assertTrue(!hammer.interact(owner, InteractionHand.MAIN_HAND).consumesAction(), "Hammer was picked up too early");
        hammer.tickCount = 12;
        helper.assertTrue(!hammer.interact(other, InteractionHand.MAIN_HAND).consumesAction(), "Hammer accepted a non-owner");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.STICK));
        helper.assertTrue(!hammer.interact(owner, InteractionHand.MAIN_HAND).consumesAction(), "Pickup overwrote a held item");
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (hotbar) {
            helper.assertTrue(hammer.interact(owner, InteractionHand.MAIN_HAND).consumesAction() && hammer.isRemoved(), "Ring pickup failed");
            helper.assertTrue(!hammer.interact(owner, InteractionHand.MAIN_HAND).consumesAction(), "Removed hammer was picked up again");
        } else {
            helper.assertTrue(!hammer.interact(owner, InteractionHand.MAIN_HAND).consumesAction(), "Unequipped owner could pick up hammer");
            owner.setItemInHand(InteractionHand.MAIN_HAND, com.binaris.wizardry.content.item.LightningHammerItem.create(owner, 39, 1.5f));
        }
        var stack = owner.getMainHandItem();
        helper.assertTrue(stack.is(EBItems.LIGHTNING_HAMMER.get()) && stack.getMaxDamage() == 39, "Pickup did not preserve remaining lifetime");
        owner.setPos(hammer.position());
        other.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.assertTrue(!stack.getItem().use(level, other, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Non-owner could throw hammer");
        other.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var result = stack.getItem().use(level, owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult().consumesAction() && result.getObject().isEmpty(), "Throw did not consume the held form");
        owner.setItemInHand(InteractionHand.MAIN_HAND, result.getObject());
        var thrown = level.getEntities(com.binaris.wizardry.setup.registries.EBEntities.LIGHTNING_HAMMER.get(),
                hammer.getBoundingBox().inflate(5), entity -> true);
        helper.assertTrue(thrown.size() == 1 && thrown.getFirst().getLifetime() == 39
                && thrown.getFirst().damageMultiplier == 1.5f && owner.getUUID().equals(thrown.getFirst().getOwnerUUID())
                && thrown.getFirst().spinning(), "Throw did not preserve owner, potency, remaining time, or spin");
        thrown.getFirst().discard();
        var expiring = com.binaris.wizardry.content.item.LightningHammerItem.create(owner, 1, 1);
        helper.runAfterDelay(2, () -> {
            expiring.getItem().inventoryTick(expiring, level, owner, 0, false);
            helper.assertTrue(expiring.isEmpty(), "Held hammer failed to expire");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_3x3x3")
    public static void spiritHorseHasAnIntrinsicSaddleAndIdleExpiry(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(1, 22, 1));
        for (var pos : BlockPos.betweenClosed(origin.offset(-3, -1, -3), origin.offset(3, -1, 3))) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin));
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(Spells.SUMMON_SPIRIT_HORSE.cast(context), "Spirit Horse failed to spawn");
        var type = com.binaris.wizardry.setup.registries.EBEntities.SPIRIT_HORSE.get();
        var horse = level.getEntities(type, new net.minecraft.world.phys.AABB(origin).inflate(5), mob -> true).getFirst();
        helper.assertTrue(horse.isTamed() && caster.getUUID().equals(horse.getOwnerUUID()) && horse.getMaxHealth() == 24
                        && horse.isSaddled() && !horse.isSaddleable(),
                "Spirit Horse lost original ownership, health, or innate saddle");
        var saved = horse.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        saved.putInt("WizardryIdleTicks", 200);
        horse.load(saved);
        horse.tick();
        helper.assertTrue(horse.opacity() < 1 && !horse.isRemoved(), "An idle horse did not begin fading");
        for (int i = 0; i < 10; i++) horse.tick();
        helper.assertTrue(horse.isRemoved(), "The idle summon did not expire after fading");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void spiritWolfSummonKeepsOneOwnedCompanion(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(1, 17, 1));
        for (var pos : BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(2, -1, 2))) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin));
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        var type = com.binaris.wizardry.setup.registries.EBEntities.SPIRIT_WOLF.get();
        var area = new net.minecraft.world.phys.AABB(origin).inflate(5);
        helper.assertTrue(Spells.SUMMON_SPIRIT_WOLF.cast(context), "Spirit Wolf could not spawn on the test platform");
        var oldWolf = level.getEntities(type, area, wolf -> true).getFirst();
        helper.assertTrue(oldWolf.isTame() && caster.getUUID().equals(oldWolf.getOwnerUUID()),
                "Spirit Wolf did not bind to its caster");
        helper.assertTrue(Spells.SUMMON_SPIRIT_WOLF.cast(context) && oldWolf.isRemoved(),
                "Second successful summon did not replace the old companion");
        var wolves = level.getEntities(type, area, wolf -> true);
        var active = Services.OBJECT_DATA.getSpellManagerData(caster)
                .getVariable(com.binaris.wizardry.content.spell.earth.SummonSpiritWolf.WOLF_UUID);
        helper.assertTrue(wolves.size() == 1 && wolves.getFirst().getUUID().equals(active),
                "Stored companion does not match the only remaining wolf");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void snareTriggersOnceAndRequiresSupport(GameTestHelper helper) {
        var level = helper.getLevel();
        var ground = helper.absolutePos(new BlockPos(1, 0, 1));
        var position = ground.above();
        level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(position.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());
        var origin = net.minecraft.world.phys.Vec3.atBottomCenterOf(position.above());
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.DOWN, 0, 0, new SpellModifiers());
        helper.assertTrue(Spells.SNARE.cast(context), "Snare failed to place above a supporting block");
        var trap = level.getBlockState(position);
        helper.assertTrue(trap.is(EBBlocks.SNARE.get()), "Snare was placed at another height. Expected " + position + ", found " + trap);
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        target.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(position));
        trap.entityInside(level, position, target);
        var slow = target.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        helper.assertTrue(level.isEmptyBlock(position) && target.getHealth() == 4 && slow != null
                        && slow.getDuration() == 100 && slow.getAmplifier() == 2,
                "Snare trigger state " + level.getBlockState(position) + ", health " + target.getHealth() + ", slow " + slow);
        trap.entityInside(level, position, target);
        helper.assertTrue(target.getHealth() == 4, "A consumed trap dealt damage twice");
        helper.assertTrue(Spells.SNARE.cast(context), "Snare failed to replace consumed trap");
        level.removeBlock(ground, false);
        helper.assertTrue(level.isEmptyBlock(position), "Unsupported snare did not disappear");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void thunderstormSpawnsTenOwnedOutdoorBolts(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var floor = helper.absolutePos(new BlockPos(1, 20, 1));
        for (var pos : BlockPos.betweenClosed(floor.offset(-11, 0, -11), floor.offset(11, 0, 11))) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(floor.above()));
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        level.setBlockAndUpdate(floor.above(3), Blocks.STONE.defaultBlockState());
        helper.assertTrue(!Spells.THUNDERSTORM.cast(context), "Thunderstorm cast under a roof");
        level.setBlockAndUpdate(floor.above(3), Blocks.AIR.defaultBlockState());
        helper.assertTrue(Spells.THUNDERSTORM.cast(context), "Thunderstorm failed outdoors");
        var bolts = level.getEntities(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT,
                new net.minecraft.world.phys.AABB(floor).inflate(12), bolt -> true);
        helper.assertTrue(bolts.size() == 10 && bolts.stream().allMatch(bolt -> {
            var data = (com.binaris.wizardry.content.spell.lightning.LightningBoltSpell.StrikeData) (Object) bolt;
            double dx = bolt.getX() - caster.getX();
            double dz = bolt.getZ() - caster.getZ();
            return data.ebwizardry$isSpellStrike() && caster.getUUID().equals(data.ebwizardry$strikeOwner())
                    && data.ebwizardry$strikeDamage() == 5 && dx * dx + dz * dz >= 25 && dx * dx + dz * dz <= 100;
        }), "Thunderstorm failed to create ten owned bolts in its original annulus, got " + bolts.size());
        bolts.forEach(bolt -> bolt.discard());
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void stormcloudStrikesBelowOnlyOnItsCadence(GameTestHelper helper) {
        var level = helper.getLevel();
        var base = helper.absolutePos(new BlockPos(1, 0, 1));
        level.setBlockAndUpdate(base, Blocks.STONE.defaultBlockState());
        var top = net.minecraft.world.phys.Vec3.atBottomCenterOf(base.above());
        level.setBlockAndUpdate(base.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(base.above(2), Blocks.AIR.defaultBlockState());
        var origin = top.add(0, 1.5, 0);
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.DOWN, 0, 0, new SpellModifiers());
        helper.assertTrue(Spells.STORMCLOUD.cast(context), "Stormcloud failed to cast on a floor");
        var clouds = level.getEntities(com.binaris.wizardry.setup.registries.EBEntities.STORMCLOUD.get(),
                new net.minecraft.world.phys.AABB(base).inflate(9), cloud -> true);
        helper.assertTrue(clouds.size() == 1, "Expected a cloud, found " + clouds.size());
        helper.assertTrue(Math.abs(clouds.getFirst().getY() - top.y - 5) < 0.01
                        && clouds.getFirst().getLifetime() == 600, "Stormcloud altitude/lifetime " + clouds.getFirst().getY() + "/" + clouds.getFirst().getLifetime() + ", expected Y " + (top.y + 5));
        var cloud = clouds.getFirst();
        var victim = net.minecraft.world.entity.EntityType.COW.create(level);
        victim.setPos(top);
        level.addFreshEntity(victim);
        victim.tickCount = 149;
        cloud.tick();
        helper.assertTrue(victim.getHealth() == 10, "Stormcloud struck before the original cadence");
        victim.tickCount = 150;
        cloud.tick();
        helper.assertTrue(victim.getHealth() == 2, "Stormcloud did not deal eight shock damage on cadence");
        cloud.tickCount = 601;
        cloud.tick();
        helper.assertTrue(cloud.isRemoved(), "Stormcloud did not expire");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void lightningBoltUsesSkyAndSpellDamage(GameTestHelper helper) {
        var level = helper.getLevel();
        var base = helper.absolutePos(new BlockPos(1, 0, 1));
        level.setBlockAndUpdate(base, Blocks.STONE.defaultBlockState());
        var origin = net.minecraft.world.phys.Vec3.atBottomCenterOf(base.above(3));
        var modifiers = new SpellModifiers().set(SpellModifiers.POTENCY, 4);
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.DOWN, 0, 0, modifiers);
        level.setBlockAndUpdate(base.above(4), Blocks.STONE.defaultBlockState());
        helper.assertTrue(!Spells.LIGHTNING_BOLT.cast(context), "Lightning Bolt struck under a roof");
        level.setBlockAndUpdate(base.above(4), Blocks.AIR.defaultBlockState());
        helper.assertTrue(Spells.LIGHTNING_BOLT.cast(context), "Lightning Bolt failed with clear sky");
        var bolts = level.getEntities(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT,
                new net.minecraft.world.phys.AABB(base).inflate(3), bolt -> true);
        helper.assertTrue(bolts.size() == 1, "Expected one registered vanilla bolt");
        var bolt = bolts.getFirst();
        var data = (com.binaris.wizardry.content.spell.lightning.LightningBoltSpell.StrikeData) (Object) bolt;
        helper.assertTrue(data.ebwizardry$isSpellStrike() && data.ebwizardry$strikeDamage() == 4,
                "Bolt lost its spell damage metadata");
        var victim = net.minecraft.world.entity.EntityType.COW.create(level);
        victim.setPos(origin.add(0, -2, 0));
        victim.thunderHit(level, bolt);
        helper.assertTrue(victim.getHealth() == 6, "Lightning Bolt did not deal its configured damage");
        bolt.tick();
        helper.assertTrue(bolt.getBlocksSetOnFire() == 0, "An ownerless spell bolt set terrain on fire");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void intimidateClearsMobTargetAndSavesFearSource(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var origin = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(0, 1, 1)));
        caster.setPos(origin);
        var target = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        target.setPos(origin.add(2, 0, 0));
        level.addFreshEntity(target);
        target.setTarget(caster);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(Spells.INTIMIDATE.cast(context) && target.getTarget() == null
                        && target.getEffect(EBMobEffects.FEAR.get()).getDuration() == 600,
                "Intimidate did not clear targeting and apply 600 ticks of fear");
        target.setTarget(caster);
        helper.assertTrue(target.getTarget() == null, "A frightened target reacquired the caster");
        var saved = target.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var reloaded = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        reloaded.load(saved);
        var source = (com.binaris.wizardry.content.effect.FearMobEffect.SourceAccess) reloaded;
        helper.assertTrue(caster.getUUID().equals(source.ebwizardry$getFearSource()) && reloaded.hasEffect(EBMobEffects.FEAR.get()),
                "An entity save did not preserve fear and the caster UUID");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void muffleHidesCasterOutsideTheDetectionArc(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var origin = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        caster.setPos(origin.add(0, 0, -2));
        var listener = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        listener.setPos(origin);
        listener.setYRot(0);
        listener.setXRot(0);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(Spells.MUFFLE.cast(context)
                        && caster.getEffect(EBMobEffects.MUFFLE.get()).getDuration() == 600,
                "Muffle did not apply its original duration");
        listener.setTarget(caster);
        helper.assertTrue(listener.getTarget() == null, "Mob detected a muffled caster behind it");
        caster.setPos(origin.add(0, 0, 2));
        listener.setTarget(caster);
        helper.assertTrue(listener.getTarget() == caster, "Muffle also blocked frontal detection");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void chainLightningDealsThreeDistinctDamageTiers(GameTestHelper helper) {
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(2, 1, 1));
        var origin = net.minecraft.world.phys.Vec3.atBottomCenterOf(anchor);
        var primary = net.minecraft.world.entity.EntityType.COW.create(level);
        var secondary = net.minecraft.world.entity.EntityType.COW.create(level);
        var tertiary = net.minecraft.world.entity.EntityType.COW.create(level);
        for (int dx = -2; dx <= 1; dx++) {
            level.setBlockAndUpdate(anchor.offset(dx, 1, 0), Blocks.AIR.defaultBlockState());
        }
        primary.setPos(origin);
        secondary.setPos(origin.add(0, 0, 4));
        tertiary.setPos(origin.add(0, 0, 8));
        level.addFreshEntity(primary);
        level.addFreshEntity(secondary);
        level.addFreshEntity(tertiary);
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x - 2, origin.y + 1, origin.z, Direction.EAST, 0, 0, new SpellModifiers());
        var start = context.vec3();
        var hit = com.binaris.wizardry.api.content.util.RayTracer.rayTrace(level, null, start, start.add(10, 0, 0),
                0.6f, false, net.minecraft.world.entity.Entity.class,
                com.binaris.wizardry.api.content.util.RayTracer.ignoreEntityFilter(null));
        helper.assertTrue(hit instanceof net.minecraft.world.phys.EntityHitResult entityHit && entityHit.getEntity() == primary,
                "Chain Lightning fixture ray hit " + hit.getType() + " at " + hit.getLocation());
        helper.assertTrue(Spells.CHAIN_LIGHTNING.cast(context), "Chain Lightning failed to strike its primary target");
        helper.assertTrue(primary.getHealth() == 0 && secondary.getHealth() == 2 && tertiary.getHealth() == 4,
                "Expected ten, eight, and six shock damage in successive chains");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void freezingWeaponUsesDifferentMeleeAndArrowDurations(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var bow = new ItemStack(net.minecraft.world.item.Items.BOW);
        var sword = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        caster.getInventory().setItem(0, sword);
        caster.getInventory().setItem(1, bow);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        var frost = EBMobEffects.FROST.get();
        helper.assertTrue(Spells.FREEZING_WEAPON.cast(context) && !Spells.FREEZING_WEAPON.cast(context),
                "Freezing Weapon failed to limit the caster to one active weapon");
        net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffectsWithItemSource(
                level, target, level.damageSources().playerAttack(caster), sword);
        helper.assertTrue(target.getEffect(frost) != null && target.getEffect(frost).getDuration() == 200,
                "An imbued sword did not apply frost for 200 ticks");
        caster.getInventory().setItem(0, bow);
        caster.getInventory().setItem(1, ItemStack.EMPTY);
        target.removeEffect(frost);
        helper.assertTrue(Spells.FREEZING_WEAPON.cast(context), "Bow could not be imbued after putting away the sword");
        var arrow = new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW, level);
        net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffectsWithItemSource(
                level, target, level.damageSources().arrow(arrow, caster), bow);
        helper.assertTrue(target.getEffect(frost) != null && target.getEffect(frost).getDuration() == 150,
                "An imbued arrow did not apply frost for 150 ticks");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void divinationNudgesTowardNearbyOre(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(0, 1, 1));
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(pos));
        caster.setYRot(-90);
        var modifiers = new SpellModifiers().set(SpellModifiers.RANGE, 3);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, modifiers);
        var target = pos.offset(1, 1, 0);
        level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        Spells.DIVINATION.cast(context);
        helper.assertTrue(caster.getDeltaMovement().lengthSqr() == 0, "Common stone produced an ore signal");
        level.setBlockAndUpdate(target, Blocks.DIAMOND_ORE.defaultBlockState());
        modifiers.set(SpellModifiers.POTENCY, 2);
        helper.assertTrue(Spells.DIVINATION.cast(context)
                        && Math.abs(caster.getDeltaMovement().x - 0.2) < 0.001
                        && Math.abs(caster.getDeltaMovement().y) < 0.001,
                "A strong nearby ore signal did not nudge the caster east");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void telekinesisReplacesAndRecoversShulkerContents(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var feet = helper.absolutePos(new BlockPos(0, 1, 1));
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(feet));
        caster.setYRot(-90);
        var target = BlockPos.containing(caster.getEyePosition().add(2, -0.25, 0));
        level.setBlockAndUpdate(target.west(), Blocks.AIR.defaultBlockState());
        var state = Blocks.BLUE_SHULKER_BOX.defaultBlockState();
        level.setBlockAndUpdate(target, state);
        var box = (net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity) level.getBlockEntity(target);
        box.setItem(6, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 5));
        var lock = Services.OBJECT_DATA.getArcaneLockData(box);
        lock.setArcaneLockOwner(caster.getUUID().toString());
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(!Spells.GREATER_TELEKINESIS.cast(context) && box.getItem(6).getCount() == 5,
                "Greater Telekinesis moved an arcane-locked shulker box");
        lock.setArcaneLockOwner(null);
        helper.assertTrue(Spells.GREATER_TELEKINESIS.cast(context) && level.isEmptyBlock(target), "Shulker box was not lifted");
        var type = com.binaris.wizardry.setup.registries.EBEntities.LEVITATING_BLOCK.get();
        var area = new net.minecraft.world.phys.AABB(target).inflate(3);
        var carrier = level.getEntities(type, area, block -> true).getFirst();
        var saved = carrier.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        carrier.discard();
        var loaded = type.create(level);
        loaded.load(saved);
        loaded.suspend(caster);
        level.addFreshEntity(loaded);
        level.setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
        for (int i = 0; i < 15 && !loaded.isRemoved(); i++) loaded.tick();
        helper.assertTrue(loaded.isRemoved() && level.getBlockEntity(target) instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity,
                "The saved carrier could not place its shulker box on landing");
        var landed = (net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity) level.getBlockEntity(target);
        helper.assertTrue(landed.getItem(6).is(net.minecraft.world.item.Items.DIAMOND) && landed.getItem(6).getCount() == 5,
                "Landing duplicated or lost shulker contents");

        helper.assertTrue(Spells.GREATER_TELEKINESIS.cast(context), "Launched box could not be lifted again");
        var failed = level.getEntities(type, area, block -> true).getFirst();
        failed.time = 601;
        failed.tick();
        var recovered = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
                item -> item.getItem().is(Blocks.BLUE_SHULKER_BOX.asItem()));
        helper.assertTrue(failed.isRemoved() && recovered.size() == 1, "Expired shulker carrier did not recover one item");
        var components = recovered.getFirst().getItem().get(net.minecraft.core.component.DataComponents.CONTAINER);
        helper.assertTrue(components != null && components.nonEmptyStream().mapToInt(ItemStack::getCount).sum() == 5,
                "The recovered shulker item lost or duplicated inventory");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void mineRespectsToolTierAndHarvestsItsTarget(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var feet = helper.absolutePos(new BlockPos(0, 1, 1));
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(feet));
        caster.setYRot(-90);
        var target = BlockPos.containing(caster.getEyePosition().add(2, -0.25, 0));
        level.setBlockAndUpdate(target.west(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(target, Blocks.IRON_ORE.defaultBlockState());
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        helper.assertTrue(!Spells.MINE.cast(context) && level.getBlockState(target).is(Blocks.IRON_ORE),
                "A novice cast could harvest iron ore");
        level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        helper.assertTrue(Spells.MINE.cast(context) && level.isEmptyBlock(target), "Mine failed to harvest eligible stone");
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(target).inflate(1), item -> item.getItem().is(net.minecraft.world.item.Items.COBBLESTONE));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1, "Mine did not produce the normal block drop");
        level.setBlockAndUpdate(target, Blocks.BEDROCK.defaultBlockState());
        helper.assertTrue(!Spells.MINE.cast(context) && level.getBlockState(target).is(Blocks.BEDROCK),
                "Mine broke bedrock");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3", timeoutTicks = 60)
    public static void imbueWeaponAddsTemporarySwordAndBowDamage(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var sword = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        var bow = new ItemStack(net.minecraft.world.item.Items.BOW);
        var otherSword = new ItemStack(net.minecraft.world.item.Items.STONE_SWORD);
        caster.getInventory().setItem(0, sword);
        caster.getInventory().setItem(1, otherSword);
        caster.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, bow);
        var strength = com.binaris.wizardry.content.spell.sorcery.ImbueWeapon.MAGIC_SWORD;
        var ranged = com.binaris.wizardry.content.spell.sorcery.ImbueWeapon.MAGIC_BOW;
        var modifiers = new SpellModifiers().set(SpellModifiers.DURATION, 0.02f);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, modifiers);
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        var normal = com.binaris.wizardry.api.content.util.EnchantmentUtil.get(level, net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);
        sword.enchant(normal, 1);
        helper.assertTrue(Spells.IMBUE_WEAPON.cast(context), "Sword imbuement failed");
        helper.assertTrue(Spells.IMBUE_WEAPON.cast(context), "Bow imbuement failed while a sword was imbued");
        helper.assertTrue(!Spells.IMBUE_WEAPON.cast(context) && otherSword.getEnchantments().isEmpty(),
                "More than one sword or bow imbuement was active");
        var damage = level.damageSources().playerAttack(caster);
        helper.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(level, sword, target, damage, 4) == 5.25f,
                "Magic Sword did not add the original melee damage");
        var arrow = new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW, level);
        var arrowDamage = level.damageSources().arrow(arrow, caster);
        helper.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(level, bow, target, arrowDamage, 2) == 3,
                "Magic Bow did not add the original arrow base damage");
        helper.assertTrue(Services.OBJECT_DATA.getImbuementData(sword.copy()).getExpirationTime(strength) == level.getGameTime() + 18,
                "Copied weapon lost its expiry");
        helper.onEachTick(() -> com.binaris.wizardry.core.DataEvents.onPlayerTick(
                new com.binaris.wizardry.api.content.event.EBLivingTick(caster, level)));
        helper.runAtTickTime(40, () -> {
            var util = com.binaris.wizardry.api.content.util.EnchantmentUtil.get(level, strength);
            helper.assertTrue(sword.getEnchantments().getLevel(util) == 0 && sword.getEnchantments().getLevel(normal) == 1
                    && bow.getEnchantments().getLevel(com.binaris.wizardry.api.content.util.EnchantmentUtil.get(level, ranged)) == 0,
                    "Expiry removed a permanent enchantment or retained an imbuement");
            helper.succeed();
        });
    }
    @GameTest(template = "empty_3x3x3")
    public static void frostBarrierRaisesAThreePanelArc(GameTestHelper helper) {
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(0, 1, 1));
        for (int z = -2; z <= 2; z++) {
            level.setBlockAndUpdate(anchor.offset(2, -1, z), Blocks.STONE.defaultBlockState());
        }
        var origin = net.minecraft.world.phys.Vec3.atBottomCenterOf(anchor);
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.EAST, 0, 0, new SpellModifiers());
        var type = com.binaris.wizardry.setup.registries.EBEntities.ICE_BARRIER.get();
        var area = new net.minecraft.world.phys.AABB(anchor).inflate(5);
        helper.assertTrue(Spells.FROST_BARRIER.cast(context), "Frost Barrier failed on solid terrain");
        var panels = level.getEntities(type, area, panel -> true);
        helper.assertTrue(panels.size() == 3, "Expected three panels from an unmodified cast, found " + panels.size());
        var middle = panels.stream().filter(panel -> panel.getLifetime() == 401).findFirst().orElseThrow();
        double restingY = middle.getY();
        for (int tick = 1; tick <= 4; tick++) {
            middle.tickCount = tick;
            middle.tick();
        }
        helper.assertTrue(middle.getY() > restingY + 2 && middle.getBoundingBox().getXsize() < middle.getBoundingBox().getZsize(),
                "Panel did not rise with its collision box aligned to the arc");
        var center = middle.getBoundingBox().getCenter();
        helper.assertTrue(middle.intersectRay(center.add(-2, 0, 0), center.add(2, 0, 0), 0).isPresent(),
                "An incoming ray passed through the raised panel");
        helper.assertTrue(!Spells.FROST_BARRIER.cast(context), "An overlapping second cast inserted more panels");
        middle.tickCount = 402;
        middle.tick();
        helper.assertTrue(middle.isRemoved(), "Panel did not expire");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void sacrificeMarkAmplifiesOnlyTheNextMagicHit(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        var victim = net.minecraft.world.entity.EntityType.COW.create(level);
        victim.setPos(center);
        level.addFreshEntity(victim);
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                center.x - 1, center.y + 1, center.z, Direction.EAST, 0, 0, new SpellModifiers());
        var mark = EBMobEffects.MARK_OF_SACRIFICE.get();
        helper.assertTrue(Spells.MARK_SACRIFICE.cast(context) && victim.getEffect(mark) != null
                && victim.getEffect(mark).getDuration() == 200, "Sacrificial mark was not applied for 200 ticks");
        victim.hurt(level.damageSources().generic(), 1);
        helper.assertTrue(victim.hasEffect(mark), "Nonmagical damage consumed the mark");
        victim.invulnerableTime = 0;
        victim.setHealth(10);
        victim.hurt(level.damageSources().magic(), 2);
        helper.assertTrue(!victim.hasEffect(mark) && Math.abs(victim.getHealth() - 6.8f) < 0.01,
                "Magic damage did not consume the mark and receive a 60 percent bonus");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void iceShroudFrostsNearbyMeleeAttacker(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        var wearer = helper.makeMockPlayer(GameType.SURVIVAL);
        wearer.setPos(center);
        var attacker = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        attacker.setPos(center.add(-1, 0, 0));
        level.addFreshEntity(attacker);
        var context = new PlayerCastContext(level, wearer, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        var shroud = EBMobEffects.ICE_SHROUD.get();
        helper.assertTrue(Spells.ICE_SHROUD.cast(context) && wearer.getEffect(shroud) != null
                && wearer.getEffect(shroud).getDuration() == 600, "Ice Shroud was not applied for 600 ticks");
        wearer.hurt(level.damageSources().mobAttack(attacker), 2);
        var frost = attacker.getEffect(EBMobEffects.FROST.get());
        helper.assertTrue(frost != null && frost.getDuration() == 100, "Melee retaliation did not inflict 100 ticks of frost");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void greaterFireballExplodesWithoutOwnerTerrainDamage(GameTestHelper helper) {
        var level = helper.getLevel();
        var wall = new BlockPos(2, 1, 1);
        helper.setBlock(wall, Blocks.WHITE_WOOL);
        helper.setBlock(wall.west(), Blocks.AIR);
        var point = net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(wall.west()));
        var modifiers = new SpellModifiers().set(SpellModifiers.BLAST, 2);
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                point.x, point.y, point.z, Direction.EAST, 0, 0, modifiers);
        var type = com.binaris.wizardry.setup.registries.EBEntities.LARGE_MAGIC_FIREBALL.get();
        helper.assertTrue(Spells.GREATER_FIREBALL.cast(context), "Greater Fireball did not cast");
        var fireballs = level.getEntities(type, new net.minecraft.world.phys.AABB(BlockPos.containing(point)).inflate(2), ball -> true);
        helper.assertTrue(fireballs.size() == 1, "Expected one large magical fireball");
        var fireball = fireballs.getFirst();
        helper.assertTrue(fireball.blastMultiplier == 2 && fireball.getLifeTime() == 16,
                "Greater Fireball ignored the blast upgrade or original lifetime");
        fireball.setDeltaMovement(1, 0, 0);
        for (int i = 0; i < 3 && !fireball.isRemoved(); i++) fireball.tick();
        helper.assertTrue(fireball.isRemoved() && level.getBlockState(helper.absolutePos(wall)).is(Blocks.WHITE_WOOL)
                        && level.isEmptyBlock(helper.absolutePos(wall.west())),
                "Ownerless fireball: removed " + fireball.isRemoved() + ", wall " + level.getBlockState(helper.absolutePos(wall)) + ", approach " + level.getBlockState(helper.absolutePos(wall.west())));
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void conjureArmorPreservesEquipmentAndExpires(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var boots = net.minecraft.world.entity.EquipmentSlot.FEET;
        var legs = net.minecraft.world.entity.EquipmentSlot.LEGS;
        var head = net.minecraft.world.entity.EquipmentSlot.HEAD;
        var chest = net.minecraft.world.entity.EquipmentSlot.CHEST;
        var realHelmet = new ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET);
        var realChest = new ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE);
        player.setItemSlot(head, realHelmet);
        player.setItemSlot(chest, realChest);
        player.getInventory().items.replaceAll(stack -> new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64));
        player.getInventory().items.set(0, new ItemStack(EBItems.SPECTRAL_LEGGINGS.get()));
        var modifiers = new SpellModifiers();
        modifiers.set(SpellModifiers.DURATION, 2);
        var context = new PlayerCastContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, 0, modifiers);

        helper.assertTrue(Services.REGISTRY_UTIL.getSpell(Spells.CONJURE_ARMOR.getLocation()) == Spells.CONJURE_ARMOR,
                "Conjure Armour is gated in a server build");
        helper.assertTrue(Spells.CONJURE_ARMOR.cast(context), "Conjure Armour failed with one eligible slot");
        var armor = player.getItemBySlot(boots);
        var data = Services.OBJECT_DATA.getConjureData(armor);
        helper.assertTrue(armor.is(EBItems.SPECTRAL_BOOTS.get()) && data.isSummoned() && data.getDuration() == 3600,
                "Conjured boots: " + armor + ", summoned " + data.isSummoned() + ", duration " + data.getDuration());
        helper.assertTrue(ItemStack.matches(player.getItemBySlot(head), realHelmet) && ItemStack.matches(player.getItemBySlot(chest), realChest)
                && player.getItemBySlot(legs).isEmpty() && !Spells.CONJURE_ARMOR.cast(context),
                "Conjuration displaced equipped armor, duplicated carried armor, or ignored duration");
        data.setExpireTime(helper.getLevel().getGameTime() - 1);
        com.binaris.wizardry.core.DataEvents.conjureItemTick(player);
        helper.assertTrue(player.getItemBySlot(boots).isEmpty() && ItemStack.matches(player.getItemBySlot(head), realHelmet),
                "Expiry removed the wrong item");
        helper.assertTrue(Spells.CONJURE_ARMOR.cast(context), "Could not conjure after expiry");
        com.binaris.wizardry.core.DataEvents.onConjureEntityDeath(
                new com.binaris.wizardry.api.content.event.EBLivingDeathEvent(player, helper.getLevel().damageSources().generic()));
        helper.assertTrue(player.getItemBySlot(boots).isEmpty() && ItemStack.matches(player.getItemBySlot(chest), realChest),
                "Death cleanup removed real equipment or retained conjured armor");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void mindTrickSuppressesTargetUntilAttacked(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        zombie.setPos(center);
        level.addFreshEntity(zombie);
        var attacker = helper.makeMockPlayer(GameType.SURVIVAL);
        attacker.setPos(center.add(-1, 0, 0));
        zombie.setTarget(attacker);
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                center.x - 1, center.y + 1, center.z, Direction.EAST, 0, 0, new SpellModifiers());
        var effect = EBMobEffects.MIND_TRICK.get();
        helper.assertTrue(Spells.MIND_TRICK.cast(context) && zombie.getTarget() == null
                && zombie.getEffect(effect) != null && zombie.getEffect(effect).getDuration() == 300,
                "Mind Trick did not clear the target and apply its original duration");
        zombie.setTarget(attacker);
        helper.assertTrue(zombie.getTarget() == null, "Mind Trick allowed the mob to reacquire a target");
        zombie.hurt(level.damageSources().playerAttack(attacker), 2);
        zombie.setTarget(attacker);
        helper.assertTrue(!zombie.hasEffect(effect) && zombie.getTarget() == attacker,
                "An attack did not restore the mob's ability to target");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void celestialSmiteHitsTerrainAndBurnsNearbyTargets(GameTestHelper helper) {
        var level = helper.getLevel();
        var base = new BlockPos(1, 0, 1);
        helper.setBlock(base, Blocks.STONE);
        var floor = helper.absolutePos(base);
        var point = net.minecraft.world.phys.Vec3.atBottomCenterOf(floor.above());
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        target.setPos(point);
        level.addFreshEntity(target);
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                point.x, point.y + 2, point.z, Direction.DOWN, 0, 0, new SpellModifiers());
        helper.assertTrue(Spells.CELESTIAL_SMITE.cast(context), "Celestial Smite failed to strike the terrain");
        helper.assertTrue(target.getHealth() == 2 && target.getRemainingFireTicks() == 200,
                "Celestial Smite did not inflict eight damage and ten seconds of fire");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void silverfishSwarmMultipliesOnKill(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.EAST, 0, 0, new SpellModifiers());
        var area = new net.minecraft.world.phys.AABB(BlockPos.containing(origin)).inflate(2);
        var type = com.binaris.wizardry.setup.registries.EBEntities.SILVERFISH_MINION.get();
        helper.assertTrue(Services.REGISTRY_UTIL.getSpell(Spells.SILVERFISH_SWARM.getLocation()) == Spells.SILVERFISH_SWARM,
                "Silverfish Swarm is missing in the server registry");
        helper.assertTrue(Spells.SILVERFISH_SWARM.cast(context), "Silverfish Swarm location cast failed");
        var initial = level.getEntities(type, area, mob -> true);
        helper.assertTrue(initial.size() == 20, "Expected twenty initial silverfish");
        var parent = initial.getFirst();
        var victim = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        victim.setPos(origin);
        victim.setHealth(0.1f);
        level.addFreshEntity(victim);
        parent.doHurtTarget(victim);
        var offspring = level.getEntities(type, area, mob -> !initial.contains(mob));
        helper.assertTrue(offspring.size() >= 1 && offspring.size() <= 4
                && offspring.stream().allMatch(child -> {
                    var data = Services.OBJECT_DATA.getMinionData(child);
                    return data.isSummoned() && data.getLifetime() == 600 && data.getOwnerUUID() == null;
                }), "An actual silverfish kill did not spawn one to four timed offspring");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void snowballAndLightningDiscLaunchFromBoundItems(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var center = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        caster.setPos(center);
        caster.setYRot(-90);
        caster.setOldPosAndRot();
        var area = new net.minecraft.world.phys.AABB(BlockPos.containing(center)).inflate(5);
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        var wand = new ItemStack(EBItems.APPRENTICE_WAND.get());
        var scroll = new ItemStack(EBItems.SCROLL.get());
        var book = new ItemStack(EBItems.SPELL_BOOK.get());
        for (var spell : java.util.List.of(Spells.SNOWBALL, Spells.LIGHTNING_DISC)) {
            com.binaris.wizardry.api.content.util.CastItemDataHelper.setSpells(wand, java.util.List.of(spell));
            RegistryUtils.setSpell(scroll, spell);
            RegistryUtils.setSpell(book, spell);
            helper.assertTrue(RegistryUtils.getSpell(scroll) == spell && RegistryUtils.getSpell(book) == spell
                    && ((com.binaris.wizardry.api.content.item.ICastItem) wand.getItem()).getCurrentSpell(wand) == spell,
                    "Book, scroll, or wand did not resolve the restored spell");
            helper.assertTrue(spell.cast(context), "Restored projectile cast failed");
        }
        var snowballs = level.getEntities(net.minecraft.world.entity.EntityType.SNOWBALL, area, ball -> ball.getOwner() == caster);
        var discs = level.getEntities(com.binaris.wizardry.setup.registries.EBEntities.LIGHTNING_DISC.get(),
                area, disc -> disc.getOwner() == caster);
        helper.assertTrue(snowballs.size() == 1 && snowballs.getFirst().getDeltaMovement().x > 1.3,
                "Snowball did not launch a vanilla projectile at the expected forward speed");
        helper.assertTrue(discs.size() == 1 && Math.abs(discs.getFirst().getDeltaMovement().length() - 1) < 0.15
                && discs.getFirst().doOverpenetration(), "Player Lightning Disc did not preserve flight speed and piercing");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void spectralGolemSummonExpires(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.EAST, 0, 0, new SpellModifiers());
        var type = com.binaris.wizardry.setup.registries.EBEntities.SPECTRAL_GOLEM.get();
        helper.assertTrue(Spells.SUMMON_IRON_GOLEM.cast(context), "Spectral Golem summon failed");
        var golems = level.getEntities(type, new net.minecraft.world.phys.AABB(BlockPos.containing(origin)).inflate(3), mob -> true);
        helper.assertTrue(golems.size() == 1, "Expected one spectral golem");
        var golem = golems.getFirst();
        var data = Services.OBJECT_DATA.getMinionData(golem);
        helper.assertTrue(golem.getMaxHealth() == 100 && Services.OBJECT_DATA.isMinion(golem)
                && data.getLifetime() == 600 && data.getOwnerUUID() == null, "Spectral Golem stats or lifetime changed");
        golem.tickCount = 601;
        data.tick();
        helper.assertTrue(golem.isRemoved(), "Expired Spectral Golem was not removed");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void paralysisDealsShockAndBreaksOnCriticalDamage(GameTestHelper helper) {
        var level = helper.getLevel();
        var feet = helper.absolutePos(new BlockPos(1, 1, 1));
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        target.setPos(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        level.addFreshEntity(target);
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                target.getX() - 1, target.getY() + 1, target.getZ(), Direction.EAST, 0, 0, new SpellModifiers());

        helper.assertTrue(Services.REGISTRY_UTIL.getSpell(Spells.PARALYSIS.getLocation()) == Spells.PARALYSIS,
                "Paralysis is absent from the runtime registry");
        helper.assertTrue(Spells.PARALYSIS.cast(context), "Paralysis did not cast");
        helper.assertTrue(target.getHealth() == 6 && target.hasEffect(EBMobEffects.PARALYSIS.get())
                        && target.getEffect(EBMobEffects.PARALYSIS.get()).getDuration() == 100,
                "Paralysis did not deal four shock damage and apply the 100-tick effect. Health was " + target.getHealth() + ", effect was " + target.getEffect(EBMobEffects.PARALYSIS.get()));
        target.setHealth(6);
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().magic(), 2);
        helper.assertTrue(!target.hasEffect(EBMobEffects.PARALYSIS.get()),
                "Critical damage did not break paralysis");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void shadowWraithSummonHasOriginalTypeAndLifetime(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.EAST, 0, 0, new SpellModifiers());
        var type = com.binaris.wizardry.setup.registries.EBEntities.SHADOW_WRAITH.get();

        helper.assertTrue(Services.REGISTRY_UTIL.getSpell(Spells.SUMMON_SHADOW_WRAITH.getLocation()) == Spells.SUMMON_SHADOW_WRAITH,
                "Summon Shadow Wraith is absent from the runtime registry");
        helper.assertTrue(Spells.SUMMON_SHADOW_WRAITH.cast(context), "Shadow Wraith location cast failed");
        var wraiths = level.getEntities(type, new net.minecraft.world.phys.AABB(BlockPos.containing(origin)).inflate(2), mob -> true);
        helper.assertTrue(wraiths.size() == 1, "Expected exactly one shadow wraith");
        var wraith = wraiths.getFirst();
        var data = Services.OBJECT_DATA.getMinionData(wraith);
        helper.assertTrue(Services.OBJECT_DATA.isMinion(wraith) && data.getOwnerUUID() == null
                        && data.getLifetime() == 600 && wraith.getMaxHealth() == 30,
                "Shadow wraith did not retain original health and summon lifetime");
        wraith.tickCount = 601;
        data.tick();
        helper.assertTrue(wraith.isRemoved(), "Shadow wraith persisted past its summon lifetime");
        helper.succeed();
    }
    @GameTest(template = "empty_3x3x3")
    public static void stormElementalSummonsAndFiresSeekingShockDisc(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.EAST, 0, 0, new SpellModifiers());
        var area = new net.minecraft.world.phys.AABB(BlockPos.containing(origin)).inflate(5);
        var type = com.binaris.wizardry.setup.registries.EBEntities.STORM_ELEMENTAL.get();

        helper.assertTrue(Services.REGISTRY_UTIL.getSpell(Spells.SUMMON_STORM_ELEMENTAL.getLocation()) == Spells.SUMMON_STORM_ELEMENTAL,
                "Summon Storm Elemental is absent from the runtime registry");
        helper.assertTrue(Spells.SUMMON_STORM_ELEMENTAL.cast(context), "Storm Elemental location cast failed");
        var summoned = level.getEntities(type, area, mob -> true);
        helper.assertTrue(summoned.size() == 1, "Expected exactly one storm elemental");
        var elemental = summoned.getFirst();
        var data = Services.OBJECT_DATA.getMinionData(elemental);
        helper.assertTrue(Services.OBJECT_DATA.isMinion(elemental) && data.getOwnerUUID() == null
                        && data.getLifetime() == 600 && elemental.getMaxHealth() == 30,
                "Storm elemental did not retain original health and summon lifetime");

        var target = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        target.setPos(origin.add(1, 0, 0));
        target.setOldPosAndRot();
        level.addFreshEntity(target);
        elemental.performRangedAttack(target, 1);
        var discType = com.binaris.wizardry.setup.registries.EBEntities.LIGHTNING_DISC.get();
        var discs = level.getEntities(discType, area, disc -> true);
        helper.assertTrue(discs.size() == 1 && discs.getFirst().getOwner() == elemental,
                "Storm elemental did not launch its owned Lightning Disc");
        var disc = discs.getFirst();
        var hit = new net.minecraft.world.phys.EntityHitResult(target);
        helper.assertTrue(disc.getLifeTime() == 30 && disc.getSeekingStrength() == 5 && disc.isNoGravity()
                        && disc.doOverpenetration() && disc.getDamage(hit) == 12
                        && disc.getDamageType(hit) == com.binaris.wizardry.setup.registries.EBDamageSources.SHOCK,
                "Lightning Disc changed its original attack properties");
        var targetCenter = target.getBoundingBox().getCenter();
        disc.setPos(targetCenter.add(-1, 0, 0));
        disc.setDeltaMovement(1, 0, 0);
        for (int tick = 0; tick < 6 && target.getHealth() == target.getMaxHealth() && !disc.isRemoved(); tick++) {
            disc.tick();
        }
        helper.assertTrue(target.getHealth() < target.getMaxHealth() && !disc.isRemoved(),
                "Lightning Disc did not damage its target and continue through it. Health " + target.getHealth() + ", removed " + disc.isRemoved());
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void lightCastsAtTheTargetAndExpires(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        var feet = helper.absolutePos(new BlockPos(0, 1, 1));
        caster.setPos(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        caster.setYRot(-90);
        caster.setXRot(0);
        var target = BlockPos.containing(caster.getEyePosition().add(2, 0, 0));
        level.setBlockAndUpdate(target.west(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(target.west(2), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0,
                new SpellModifiers().set(SpellModifiers.DURATION, 4));
        helper.assertTrue(Spells.LIGHT.cast(context), "Light did not cast at the aimed surface");
        var placed = target.west();
        helper.assertTrue(level.getBlockState(placed).is(EBBlocks.MAGIC_LIGHT.get())
                && level.getBlockState(placed).getLightEmission() == 15, "Light did not place a full-strength light source");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(level.isEmptyBlock(placed), "Light persisted past the modified duration");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_3x3x3")
    public static void greaterTelekinesisLiftsAndThrowsActualBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        var caster = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos feet = helper.absolutePos(new BlockPos(0, 1, 1));
        caster.setPos(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        caster.setYRot(-90);
        caster.setXRot(0);
        BlockPos target = BlockPos.containing(caster.getEyePosition().add(2, -0.25, 0));
        var area = new net.minecraft.world.phys.AABB(feet).inflate(5);
        var spell = Spells.GREATER_TELEKINESIS;
        var context = new PlayerCastContext(level, caster, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        for (BlockPos pos : BlockPos.betweenClosed(target.west(2), target.east())) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(target, Blocks.BEDROCK.defaultBlockState());
        helper.assertTrue(!spell.cast(context) && level.getBlockState(target).is(Blocks.BEDROCK), "Greater Telekinesis moved an unbreakable block");

        var state = Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X);
        level.setBlockAndUpdate(target, state);
        int pigs = level.getEntities(net.minecraft.world.entity.EntityType.PIG, area, entity -> true).size();
        helper.assertTrue(spell.cast(context), "Greater Telekinesis did not lift the targeted block");
        var blocks = level.getEntities(com.binaris.wizardry.setup.registries.EBEntities.LEVITATING_BLOCK.get(), area, entity -> true);
        helper.assertTrue(blocks.size() == 1 && blocks.getFirst().getBlockState() == state && level.isEmptyBlock(target),
                "Greater Telekinesis failed to transfer the exact block state to one entity");
        helper.assertTrue(level.getEntities(net.minecraft.world.entity.EntityType.PIG, area, entity -> true).size() == pigs,
                "Greater Telekinesis spawned a pig");

        var block = blocks.getFirst();
        for (BlockPos pos : BlockPos.betweenClosed(target.east(), target.east(5).above())) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        context.castingTicks(1);
        helper.assertTrue(spell.cast(context) && block.getDeltaMovement().x > 0.5, "Greater Telekinesis did not hold the block ahead of the caster");
        block.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        caster.setShiftKeyDown(true);
        helper.assertTrue(spell.cast(context) && Math.abs(block.getDeltaMovement().x - 0.15) < 0.001,
                "Sneak-casting did not add the original throw velocity");

        caster.setShiftKeyDown(false);
        for (int tick = 2; tick < 22; tick++) {
            context.castingTicks(tick);
            helper.assertTrue(spell.cast(context), "Suspension lost the block during a steady cast on tick " + tick);
            block.tick();
        }
        helper.assertTrue(Math.abs(block.getX() - caster.getX() - 5) < 0.15, "The held block drifted away from its hold range");

        BlockPos floor = BlockPos.containing(block.position()).below(2);
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(floor.above(), Blocks.AIR.defaultBlockState());
        for (int tick = 0; tick < 30 && !block.isRemoved(); tick++) block.tick();
        helper.assertTrue(block.isRemoved() && level.getBlockState(floor.above()) == state, "Released block did not settle");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void casterlessLightningRayDamagesAtOriginalInterval(GameTestHelper helper) {
        var target = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(2, 1, 1));
        target.setNoAi(true);
        var origin = target.getBoundingBox().getCenter().subtract(1.8, 0, 0);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(origin), target.blockPosition().east())) {
            helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        var hit = com.binaris.wizardry.api.content.util.RayTracer.rayTrace(helper.getLevel(), null,
                origin, target.getBoundingBox().getCenter().add(1, 0, 0), 0.6f, false,
                net.minecraft.world.entity.Entity.class, entity -> false);
        helper.assertTrue(hit instanceof net.minecraft.world.phys.EntityHitResult entityHit && entityHit.getEntity() == target,
                "The ray test did not put the target in the ray path: " + hit.getType());
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(helper.getLevel(),
                origin.x, origin.y, origin.z, Direction.EAST, 1, 120, new SpellModifiers());
        float before = target.getHealth();
        helper.assertTrue(Spells.LIGHTNING_RAY.cast(context) && target.getHealth() == before, "Lightning Ray damaged early");
        context.castingTicks(10);
        helper.assertTrue(Spells.LIGHTNING_RAY.cast(context) && target.getHealth() < before, "Lightning Ray missed its damage interval, health " + before + " to " + target.getHealth());
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void restoredIceSummonsUseOriginalLifetime(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        var context = new com.binaris.wizardry.api.content.spell.internal.LocationCastContext(level,
                origin.x, origin.y, origin.z, Direction.EAST, 0, 0, new SpellModifiers());
        helper.assertTrue(Spells.SUMMON_ICE_WRAITH.cast(context), "Ice Wraith location cast failed");
        helper.assertTrue(Spells.SUMMON_ICE_GIANT.cast(context), "Ice Giant location cast failed");
        var area = new net.minecraft.world.phys.AABB(BlockPos.containing(origin)).inflate(3);
        var wraith = level.getEntities(com.binaris.wizardry.setup.registries.EBEntities.ICE_WRAITH.get(), area, entity -> true);
        var giant = level.getEntities(com.binaris.wizardry.setup.registries.EBEntities.ICE_GIANT.get(), area, entity -> true);
        helper.assertTrue(wraith.size() == 1 && Services.OBJECT_DATA.isMinion(wraith.getFirst())
                        && Services.OBJECT_DATA.getMinionData(wraith.getFirst()).getLifetime() == 600,
                "Ice Wraith did not spawn for 600 ticks");
        helper.assertTrue(giant.size() == 1 && Services.OBJECT_DATA.isMinion(giant.getFirst())
                        && Services.OBJECT_DATA.getMinionData(giant.getFirst()).getLifetime() == 600,
                "Ice Giant did not spawn for 600 ticks");
        var expired = giant.getFirst();
        expired.tickCount = 601;
        Services.OBJECT_DATA.getMinionData(expired).tick();
        helper.assertTrue(expired.isRemoved(), "A dispenser summon persisted past its lifetime");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void lecternIndexesRegisteredBooksAndPersistsSelection(GameTestHelper helper) {
        var level = helper.getLevel();
        var lecternPos = new BlockPos(1, 1, 1);
        var shelfPos = lecternPos.east();
        helper.setBlock(lecternPos, EBBlocks.OAK_LECTERN.get());
        helper.setBlock(shelfPos, EBBlocks.OAK_BOOKSHELF.get());
        var lectern = (LecternBlockEntity) level.getBlockEntity(helper.absolutePos(lecternPos));
        var shelf = (BookshelfBlockEntity) level.getBlockEntity(helper.absolutePos(shelfPos));
        var spell = Services.REGISTRY_UTIL.getSpells().stream()
                .filter(entry -> entry.getLocation().getNamespace().equals("tfspellpack"))
                .findFirst().orElse(Spells.FROST_RAY);

        var book = new ItemStack(EBItems.SPELL_BOOK.get());
        RegistryUtils.setSpell(book, spell);
        shelf.setItem(0, book);
        shelf.setItem(1, book.copy());
        var locations = LecternBlockEntity.index(level, lectern.getBlockPos()).get(spell);
        helper.assertTrue(locations != null && locations.size() == 1 && locations.contains(shelf.getBlockPos()), "Index should deduplicate a shelf");
        helper.assertTrue(!lectern.select(Spells.ICE_BALL.getLocation()), "Selection must exist in nearby shelves");
        helper.assertTrue(lectern.select(spell.getLocation()), "Index should select the registered spell");
        var saved = lectern.saveWithoutMetadata(level.registryAccess());
        var restored = new LecternBlockEntity(lectern.getBlockPos(), lectern.getBlockState());
        restored.loadWithComponents(saved, level.registryAccess());
        helper.assertTrue(spell.getLocation().equals(restored.selected()), "Selected spell ID was not persisted");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = 165)
    public static void dispenserConsumesScrollsAndStopsWithPower(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var power = pos.west();
        var absolute = helper.absolutePos(pos);
        helper.setBlock(pos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.NORTH));
        var dispenser = (DispenserBlockEntity) helper.getLevel().getBlockEntity(absolute);
        var scrolls = new ItemStack(EBItems.SCROLL.get(), 3);
        RegistryUtils.setSpell(scrolls, Spells.FROST_RAY);
        dispenser.setItem(0, scrolls);
        helper.setBlock(pos.north(), Blocks.STONE);
        helper.setBlock(power, Blocks.REDSTONE_BLOCK);
        helper.runAtTickTime(6, () -> {
            helper.assertTrue(dispenser.getItem(0).getCount() == 3, "Blocked dispenser consumed a scroll");
            helper.setBlock(power, Blocks.AIR);
            helper.setBlock(pos.north(), Blocks.AIR);
        });
        helper.runAtTickTime(9, () -> helper.setBlock(power, Blocks.REDSTONE_BLOCK));
        helper.runAtTickTime(18, () -> helper.assertTrue(dispenser.getItem(0).getCount() == 2, "Successful cast did not consume one scroll"));
        helper.runAtTickTime(140, () -> {
            helper.assertTrue(dispenser.getItem(0).getCount() == 1, "Powered channel did not use a matching refill");
            helper.setBlock(power, Blocks.AIR);
        });
        helper.runAtTickTime(151, () -> {
            helper.assertTrue(dispenser.getItem(0).getCount() == 1, "Unpowered dispenser consumed another scroll");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void glideAndEmpowermentRetainOriginalModifiers(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        var modifiers = new SpellModifiers().multiplyTotal(SpellModifiers.POTENCY, 1.2f);
        var context = new PlayerCastContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, 0, modifiers);
        player.setDeltaMovement(0, -0.8, 0);
        player.setYRot(0);
        player.setXRot(0);
        helper.assertTrue(Spells.GLIDE.cast(context), "Glide did not start");
        helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.1) < 0.0001
                && Math.abs(player.getDeltaMovement().z - 0.12) < 0.0001, "Glide fall speed or acceleration changed");

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(EBMobEffects.EMPOWERMENT.get(), 900));
        WizardryEventBus.fireEvent(new SpellCastEvent.Prepare(SpellCastEvent.Sources.WAND, Spells.HEAL, context));
        WizardryEventBus.fireEvent(new SpellCastEvent.Pre(SpellCastEvent.Sources.WAND, Spells.HEAL, context));
        helper.assertTrue(Math.abs(modifiers.getFactor(SpellModifiers.POTENCY) - 1.38f) < 0.0001,
                "Empowerment did not multiply the existing potency");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mineralFleshUsesOriginalBonuses(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var ctx = new PlayerCastContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
        double armor = player.getAttributeValue(Attributes.ARMOR);
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double knockback = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double toughness = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        helper.assertTrue(Spells.IRONFLESH.cast(ctx), "Ironflesh failed");
        helper.assertTrue(player.getEffect(EBMobEffects.IRONFLESH.get()).getDuration() == 600, "Ironflesh duration changed");
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == armor + 4, "Ironflesh armor changed");
        helper.assertTrue(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == knockback + 0.3, "Ironflesh knockback changed");
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.9 * speed) < 0.0001, "Slowness changed");
        player.removeEffect(EBMobEffects.IRONFLESH.get());
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == armor, "Armor was retained after removal");
        helper.assertTrue(Spells.DIAMONDFLESH.cast(ctx), "Diamondflesh failed");
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == toughness + 3, "Diamondflesh toughness changed");
        helper.succeed();
    }
}
