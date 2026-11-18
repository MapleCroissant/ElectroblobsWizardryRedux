package com.binaris.wizardry.content.entity.living;

import com.binaris.wizardry.api.client.ParticleBuilder;
import net.minecraft.server.level.ServerLevel;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public final class DecoyEntity extends PathfinderMob implements OwnableEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(DecoyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(DecoyEntity.class, EntityDataSerializers.INT);
    private static final byte VANISH = 71;
    private int life = 600;

    public DecoyEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new RandomStrollGoal(this, 1));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, LivingEntity.class, 6));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    public void configure(LivingEntity caster, int lifetime) {
        entityData.set(OWNER, Optional.of(caster.getUUID()));
        entityData.set(CASTER_ID, caster.getId());
        life = lifetime;
        setPersistenceRequired();
        if (caster instanceof Player) {
            setCustomName(caster.getDisplayName());
            setCustomNameVisible(true);
        }
    }

    @Override
    public UUID getOwnerUUID() { return entityData.get(OWNER).orElse(null); }

    @Override
    public LivingEntity getOwner() {
        if (level().isClientSide && level().getEntity(entityData.get(CASTER_ID)) instanceof LivingEntity caster
                && caster.getUUID().equals(getOwnerUUID())) return caster;
        var entity = getOwnerUUID() != null && level() instanceof ServerLevel server ? server.getEntity(getOwnerUUID()) : null;
        return entity instanceof LivingEntity caster ? caster : null;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) { return true; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            var owner = getOwner();
            if (--life <= 0 || (owner == null && tickCount > 20) || (owner != null && !owner.isAlive())) {
                level().broadcastEntityEvent(this, VANISH);
                discard();
            } else if (owner != null) {
                entityData.set(CASTER_ID, owner.getId());
            }
        }
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event != VANISH) {
            super.handleEntityEvent(event);
            return;
        }
        for (int i = 0; i < 20; i++) {
            ParticleBuilder.create(EBParticles.DUST, this).time(40).color(0.2f, 1, 0.8f).shaded(true).spawn(level());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty()).define(CASTER_ID, -1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (getOwnerUUID() != null) tag.putUUID("DecoyOwner", getOwnerUUID());
        tag.putInt("DecoyRemaining", life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(OWNER, tag.hasUUID("DecoyOwner") ? Optional.of(tag.getUUID("DecoyOwner")) : Optional.empty());
        life = tag.getInt("DecoyRemaining");
    }
}
