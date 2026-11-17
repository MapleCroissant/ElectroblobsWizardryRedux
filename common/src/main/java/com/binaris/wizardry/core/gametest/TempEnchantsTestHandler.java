package com.binaris.wizardry.core.gametest;

import com.binaris.wizardry.api.content.data.ImbuementEnchantData;
import com.binaris.wizardry.api.content.event.EBLivingTick;
import com.binaris.wizardry.core.DataEvents;
import com.binaris.wizardry.core.platform.Services;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("all")
public final class TempEnchantsTestHandler {
    private static final Vec3 PLAYER_POS = new Vec3(1.5, 2.0, 1.5);

    static void simpleEnchantTest(GameTestHelper helper, Item itemToEnchant, net.minecraft.resources.ResourceKey<Enchantment> tempEnchant) {
        // The check is done in setupTest
        TestContext ctx = setupTest(helper, itemToEnchant.getDefaultInstance(), tempEnchant);
        helper.succeed();
    }


    static void simpleEnchantCompanyTest(GameTestHelper helper, Item itemToEnchant, net.minecraft.resources.ResourceKey<Enchantment> tempEnchant, net.minecraft.resources.ResourceKey<Enchantment> normalEnchant) {
        TestContext ctx = setupTest(helper, itemToEnchant.getDefaultInstance(), tempEnchant);
        ctx.stack.enchant(com.binaris.wizardry.api.content.util.EnchantmentUtil.get(ctx.player.level(), normalEnchant), 1);
        GST.assertTrue(helper, "Imbuement enchant not present after applying", com.binaris.wizardry.api.content.util.EnchantmentUtil.level(tempEnchant, ctx.player) > 0);
        GST.assertTrue(helper, "Normal enchant not present after applying with imbuement enchant", com.binaris.wizardry.api.content.util.EnchantmentUtil.level(normalEnchant, ctx.player) > 0);
        helper.succeed();
    }


    static void imbuementTickTest(GameTestHelper helper, Item itemToEnchant, net.minecraft.resources.ResourceKey<Enchantment> tempEnchant) {
        TestContext ctx = setupTest(helper, itemToEnchant.getDefaultInstance(), tempEnchant);

        helper.onEachTick(() -> DataEvents.onPlayerTick(new EBLivingTick(ctx.player, ctx.player.level())));

        helper.runAtTickTime(60, () -> {
            DataEvents.onPlayerTick(new EBLivingTick(ctx.player, ctx.player.level()));
            GST.assertFalse(helper, "Imbuement enchant should be removed after 40 ticks (end time)", com.binaris.wizardry.api.content.util.EnchantmentUtil.level(tempEnchant, ctx.player) > 0);
            helper.succeed();
        });
    }


    static void imbuementTickCompanyTest(GameTestHelper helper, Item itemToEnchant, net.minecraft.resources.ResourceKey<Enchantment> tempEnchantment, net.minecraft.resources.ResourceKey<Enchantment> enchantment) {
        TestContext ctx = setupTest(helper, itemToEnchant.getDefaultInstance(), tempEnchantment);
        ctx.stack.enchant(com.binaris.wizardry.api.content.util.EnchantmentUtil.get(ctx.player.level(), enchantment), 1);

        helper.onEachTick(() -> DataEvents.onPlayerTick(new EBLivingTick(ctx.player, ctx.player.level())));

        helper.runAtTickTime(60, () -> {
            DataEvents.onPlayerTick(new EBLivingTick(ctx.player, ctx.player.level()));
            GST.assertFalse(helper, "Imbuement enchant should be removed after 40 ticks (end time)", com.binaris.wizardry.api.content.util.EnchantmentUtil.level(tempEnchantment, ctx.player) > 0);
            GST.assertTrue(helper, "Normal enchant not present after removing imbuement", com.binaris.wizardry.api.content.util.EnchantmentUtil.level(enchantment, ctx.player) > 0);
            helper.succeed();
        });
    }

    static TestContext setupTest(GameTestHelper helper, ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> enchantment) {
        Player player = GST.mockPlayer(helper, PLAYER_POS);
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        ImbuementEnchantData data = Services.OBJECT_DATA.getImbuementData(stack);
        stack.enchant(com.binaris.wizardry.api.content.util.EnchantmentUtil.get(player.level(), enchantment), 1);
        data.addImbuement(enchantment, player.level().getGameTime() + 40);

        GST.assertTrue(helper, "Imbuement enchant not present after applying", com.binaris.wizardry.api.content.util.EnchantmentUtil.level(enchantment, player) > 0);
        return new TestContext(player, stack, enchantment, data);
    }

    record TestContext(Player player, ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> enchantment, ImbuementEnchantData data) {
    }

    private TempEnchantsTestHandler() {
    }
}