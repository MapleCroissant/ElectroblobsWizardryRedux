package com.binaris.wizardry.content.spell.sorcery;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.util.EnchantmentUtil;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;

public class ImbueWeapon extends Spell {
    public static final ResourceKey<Enchantment> MAGIC_SWORD = ResourceKey.create(Registries.ENCHANTMENT, WizardryMainMod.location("magic_sword"));
    public static final ResourceKey<Enchantment> MAGIC_BOW = ResourceKey.create(Registries.ENCHANTMENT, WizardryMainMod.location("magic_bow"));

    public static boolean isSword(ItemStack stack) {
        return stack.getItem() instanceof SwordItem;
    }

    public static boolean isBow(ItemStack stack) {
        return stack.getItem() instanceof BowItem;
    }

    @Override
    public boolean cast(PlayerCastContext context) {
        var caster = context.caster();
        var world = context.world();
        if (world.isClientSide) {
            for (int i = 0; i < 10; i++) {
                ParticleBuilder.create(EBParticles.SPARKLE).pos(caster.getX() + world.random.nextDouble() * 2 - 1,
                                caster.getEyeY() - 0.5 + world.random.nextDouble(), caster.getZ() + world.random.nextDouble() * 2 - 1)
                        .velocity(0, 0.1, 0).color(0.9f, 0.7f, 1).spawn(world);
            }
        } else {
            int duration = (int) (property(DefaultProperties.EFFECT_DURATION) * context.modifiers().getFactor(SpellModifiers.DURATION));
            if (duration <= 0) return false;
            long now = world.getGameTime();
            var inventory = EntityUtil.getAllItems(caster);
            boolean applied = false;
            for (ItemStack stack : EntityUtil.getHotBarAndHandItems(caster)) {
                ResourceKey<Enchantment> key = enchantmentFor(stack);
                if (key == null || EnchantmentUtil.itemLevel(world, key, stack) > 0
                        || inventory.stream().anyMatch(item -> isActive(item, key, now))) continue;
                var data = Services.OBJECT_DATA.getImbuementData(stack);
                if (data == null) continue;
                float step = EBServerConfig.POTENCY_INCREASE_PER_TIER.get();
                float potency = context.modifiers().getFactor(SpellModifiers.POTENCY);
                int level = step > 0 ? Math.clamp(Math.round((potency - 1) / step), 1, 4) : 1;
                stack.enchant(EnchantmentUtil.get(world, key), level);
                data.addImbuement(key, now + duration);
                applied = true;
                break;
            }
            if (!applied) return false;
            caster.getInventory().setChanged();
        }
        playSound(world, caster, context.castingTicks(), -1);
        return true;
    }

    protected ResourceKey<Enchantment> enchantmentFor(ItemStack stack) {
        return isSword(stack) ? MAGIC_SWORD : isBow(stack) ? MAGIC_BOW : null;
    }

    private static boolean isActive(ItemStack stack, ResourceKey<Enchantment> key, long now) {
        if (stack.isEmpty() || stack.getEnchantments().isEmpty()) return false;
        var data = Services.OBJECT_DATA.getImbuementData(stack);
        return data != null && data.getImbuements().getOrDefault(key.location(), 0L) > now;
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.APPRENTICE, Elements.SORCERY, SpellTypes.UTILITY, SpellAction.IMBUE, 20, 0, 50)
                .add(DefaultProperties.EFFECT_DURATION, 900).build();
    }
}
