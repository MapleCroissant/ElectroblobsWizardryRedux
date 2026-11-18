package com.binaris.wizardry.content.item;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.entity.construct.LightningHammerConstruct;
import com.binaris.wizardry.content.spell.lightning.LightningHammer;
import com.binaris.wizardry.core.AllyDesignation;
import com.binaris.wizardry.setup.registries.*;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

public final class LightningHammerItem extends Item {
    public LightningHammerItem() {
        super(new Properties().durability(600).rarity(Rarity.EPIC).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 10, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(WizardryMainMod.location("hammer_weight"), -0.25,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND).build()));
    }

    public static ItemStack create(Player owner, int remaining, float potency) {
        ItemStack stack = new ItemStack(EBItems.LIGHTNING_HAMMER.get());
        stack.set(DataComponents.MAX_DAMAGE, remaining);
        CompoundTag data = new CompoundTag();
        data.putUUID("HammerOwner", owner.getUUID());
        data.putFloat("HammerPotency", potency);
        data.putLong("HammerExpiry", owner.level().getGameTime() + remaining);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static boolean belongsTo(ItemStack stack, Player player) {
        var tag = data(stack);
        return tag.hasUUID("HammerOwner") && player.getUUID().equals(tag.getUUID("HammerOwner"));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide) return;
        int remaining = remaining(stack, level);
        if (!(holder instanceof Player player) || !belongsTo(stack, player)
                || remaining <= 0) {
            stack.setCount(0);
        } else {
            stack.setDamageValue(stack.getMaxDamage() - remaining);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int remaining = remaining(stack, level);
        if (!belongsTo(stack, player) || remaining <= 0) return InteractionResultHolder.fail(stack);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        var hammer = new LightningHammerConstruct(level);
        var look = player.getLookAngle();
        hammer.setPos(player.getEyePosition().add(look).add(0, -hammer.getBbHeight() / 2, 0));
        hammer.setYRot(player.getYHeadRot() - 90);
        hammer.setDeltaMovement(look.scale(0.75 * player.getAttackStrengthScale(0)));
        hammer.setCaster(player);
        hammer.damageMultiplier = data(stack).getFloat("HammerPotency");
        hammer.setLifetime(remaining);
        hammer.startSpin();
        if (!level.addFreshEntity(hammer)) return InteractionResultHolder.fail(stack);

        player.playSound(EBSounds.ENTITY_HAMMER_THROW.get(), 1, 0.8f);
        return InteractionResultHolder.consume(ItemStack.EMPTY);
    }

    public static void onAttack(ItemStack stack, Player player, LivingEntity hit) {
        if (!belongsTo(stack, player) || player.level().isClientSide || remaining(stack, player.level()) <= 0) return;
        float strength = player.getAttackStrengthScale(0);
        hit.knockback(2 * strength, player.getX() - hit.getX(), player.getZ() - hit.getZ());
        if (strength < 1) return;
        var source = MagicDamageSource.causeDirectMagicDamage(player, EBDamageSources.SHOCK);
        float damage = 4 * data(stack).getFloat("HammerPotency");
        EntityUtil.getLivingWithinRadius(4, hit.getX(), hit.getY(), hit.getZ(), hit.level()).stream()
                .filter(target -> target != hit && target.isAlive() && AllyDesignation.isValidTarget(player, target))
                .limit(Math.max(0, Spells.LIGHTNING_HAMMER.property(LightningHammer.TARGET_LIMIT) / 2))
                .forEach(target -> {
                    target.hurt(source, damage);
                    ParticleBuilder.create(EBParticles.LIGHTNING).allowServer(true).pos(hit.getBoundingBox().getCenter()).target(target).spawn(hit.level());
                    ParticleBuilder.spawnShockParticles(hit.level(), target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ());
                });
    }

    private static int remaining(ItemStack stack, Level level) {
        if (level.isClientSide) return stack.getMaxDamage() - stack.getDamageValue();
        return (int) Math.clamp(data(stack).getLong("HammerExpiry") - level.getGameTime(), 0, stack.getMaxDamage());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) { return false; }
}
