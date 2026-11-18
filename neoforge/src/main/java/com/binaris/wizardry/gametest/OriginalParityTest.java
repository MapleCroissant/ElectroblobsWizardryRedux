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
