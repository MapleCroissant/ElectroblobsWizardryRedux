package com.binaris.wizardry.gametest;

import com.binaris.wizardry.attachments.EBComponents;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("ebwizardry")
@PrefixGameTestTemplate(false)
public class NeoForgeMigrationTest {
    @GameTest(template = "empty")
    public static void elementalBiomeBonusesUseConventionalTags(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        for (var key : java.util.List.of(net.minecraft.world.level.biome.Biomes.FOREST, net.minecraft.world.level.biome.Biomes.BIRCH_FOREST, net.minecraft.world.level.biome.Biomes.DARK_FOREST, net.minecraft.world.level.biome.Biomes.JUNGLE)) {
            if (!Services.PLATFORM.inEarthBiomes(biomes.getHolderOrThrow(key))) helper.fail("Earth artifact biome bonus omitted " + key.location());
        }
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void activeArtifactIntegrationEquipsAndUnequips(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var ring = EBItems.getArtifacts().entrySet().stream()
                .filter(entry -> entry.getValue() == com.binaris.wizardry.api.content.item.ArtifactItem.Type.RING)
                .findFirst().orElseThrow().getKey().get();
        if (Services.PLATFORM.isModLoaded("accessories")) OptionalEquipmentChecks.accessories(player, ring);
        else if (Services.PLATFORM.isModLoaded("curios")) OptionalEquipmentChecks.curios(player, ring);
        else {
            player.getInventory().setItem(0, new ItemStack(ring));
            if (!com.binaris.wizardry.core.integrations.ArtifactChannel.isEquipped(player, ring)) helper.fail("Vanilla artifact fallback did not detect the ring");
            player.getInventory().setItem(0, ItemStack.EMPTY);
            if (com.binaris.wizardry.core.integrations.ArtifactChannel.isEquipped(player, ring)) helper.fail("Vanilla artifact fallback retained the removed ring");
        }
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void legacyEntityAttachmentImport(GameTestHelper helper) {
        var mob = EntityType.ZOMBIE.create(helper.getLevel());
        CompoundTag saved = mob.saveWithoutId(new CompoundTag());
        saved.remove("neoforge:attachments");
        CompoundTag old = new CompoundTag(), minion = new CompoundTag();
        minion.putBoolean("summoned", true); minion.putInt("lifetime", 900);
        old.put("ebwizardry:minion_data", minion); saved.put("cardinal_components", old);
        mob.load(saved);
        if (!Services.OBJECT_DATA.isMinion(mob) || Services.OBJECT_DATA.getMinionData(mob).getLifetime() != 900) helper.fail("Legacy Fabric minion data was not imported");
        CompoundTag modern = mob.saveWithoutId(new CompoundTag());
        minion.putInt("lifetime", 1); old.put("ebwizardry:minion_data", minion); modern.put("cardinal_components", old);
        var restored = EntityType.ZOMBIE.create(helper.getLevel()); restored.load(modern);
        if (Services.OBJECT_DATA.getMinionData(restored).getLifetime() != 900) helper.fail("Legacy data overwrote a modern attachment");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void nativeEntityAttachmentsPersist(GameTestHelper helper) {
        var mob = EntityType.ZOMBIE.create(helper.getLevel());
        var minion = Services.OBJECT_DATA.getMinionData(mob);
        UUID owner = UUID.randomUUID();
        minion.setSummoned(true); minion.setLifetime(600); minion.setOwnerUUID(owner);
        Services.OBJECT_DATA.getContainmentData(mob).setContainmentPos(new BlockPos(3, 60, 4));
        CompoundTag saved = mob.saveWithoutId(new CompoundTag());
        var restored = EntityType.ZOMBIE.create(helper.getLevel()); restored.load(saved);
        var data = Services.OBJECT_DATA.getMinionData(restored);
        if (!data.isSummoned() || data.getLifetime() != 600 || !owner.equals(data.getOwnerUUID())
                || !new BlockPos(3, 60, 4).equals(Services.OBJECT_DATA.getContainmentData(restored).getContainmentPos())) helper.fail("Native minion or containment attachment did not survive saving");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void nativePlayerAttachmentsPersist(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        UUID ally = UUID.randomUUID();
        EBComponents.WIZARD_DATA.get(player).allies.add(ally);
        Services.OBJECT_DATA.getSpellManagerData(player).discoverSpell(Spells.HEAL);
        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        var restored = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        restored.load(saved);
        if (!EBComponents.WIZARD_DATA.get(restored).allies.contains(ally)
                || !Services.OBJECT_DATA.getSpellManagerData(restored).hasSpellBeenDiscovered(Spells.HEAL)) helper.fail("Native player attachment lost ally or spell discovery state");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void nativeArmorAttributesRespectMana(GameTestHelper helper) {
        ItemStack stack = new ItemStack(EBItems.WIZARD_HAT.get());
        var armor = (com.binaris.wizardry.content.item.armor.WizardArmorItem) stack.getItem();
        if (stack.getAttributeModifiers().modifiers().isEmpty()) helper.fail("Charged armor has no native attribute modifiers");
        armor.setMana(stack, 0);
        if (!stack.getAttributeModifiers().modifiers().isEmpty()) helper.fail("Empty armor still provides native attribute modifiers");
        helper.succeed();
    }
}
