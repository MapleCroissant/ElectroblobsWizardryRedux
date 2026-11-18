package com.binaris.wizardry.content.entity;

import com.binaris.wizardry.setup.registries.EBEntities;
import com.binaris.wizardry.setup.registries.EBSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class ShieldEntity extends Entity {
    private UUID owner;
    private long lastRefresh;

    public ShieldEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static boolean refresh(Player player) {
        var level = player.level();
        if (level.isClientSide) return true;
        var shield = level.getEntitiesOfClass(ShieldEntity.class, player.getBoundingBox().inflate(4),
                existing -> player.getUUID().equals(existing.owner)).stream().findFirst().orElse(null);
        if (shield == null) {
            shield = new ShieldEntity(EBEntities.SHIELD.get(), level);
            shield.owner = player.getUUID();
            shield.follow(player);
            return level.addFreshEntity(shield);
        }
        shield.follow(player);
        return true;
    }

    private void follow(Player player) {
        lastRefresh = level().getGameTime();
        var point = player.position().add(0, 0.3, 0).add(player.getLookAngle().scale(0.3));
        setPos(point);
        setYRot(player.getYHeadRot());
        setXRot(player.getXRot());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && level().getGameTime() - lastRefresh > 2) discard();
    }

    @Override
    public boolean isPickable() { return !isRemoved(); }

    @Override
    public ProjectileDeflection deflection(Projectile projectile) {
        if (!level().isClientSide) playSound(EBSounds.ENTITY_SHIELD_DEFLECT.get(), 0.3f, 1.3f);
        return ProjectileDeflection.REVERSE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
