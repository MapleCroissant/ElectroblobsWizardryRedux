package com.binaris.wizardry.content.entity.projectile;

import com.binaris.wizardry.setup.registries.EBEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ConjuredArrowEntity extends AbstractArrow {
    private int knockback;
    public void setKnockback(int value) { knockback = value; }
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("knockback", knockback);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        knockback = tag.getInt("knockback");
    }

    @Override
    protected void doKnockback(LivingEntity target, net.minecraft.world.damagesource.DamageSource source) {
        super.doKnockback(target, source);
        if (knockback > 0) {
            double resistance = Math.max(0, 1 - target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
            var motion = getDeltaMovement().multiply(1, 0, 1).normalize().scale(knockback * 0.6 * resistance);
            if (motion.lengthSqr() > 0) target.push(motion.x, 0.1, motion.z);
        }
    }

    public ConjuredArrowEntity(EntityType<ConjuredArrowEntity> entityType, Level world) {
        super(entityType, world);
    }

    public ConjuredArrowEntity(double x, double y, double z, Level level) {
        super(EBEntities.CONJURED_ARROW.get(), x, y, z, level, new ItemStack(net.minecraft.world.item.Items.ARROW), null);
    }

    public ConjuredArrowEntity(EntityType<? extends AbstractArrow> type, LivingEntity entity, Level level) {
        super(type, entity, level, new ItemStack(net.minecraft.world.item.Items.ARROW), null);
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 60) this.discard();
        super.tick();
    }

    @Override
    protected @NotNull ItemStack getDefaultPickupItem() {
        return new ItemStack(net.minecraft.world.item.Items.ARROW);
    }

    @Override
    protected boolean tryPickup(net.minecraft.world.entity.player.Player player) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
