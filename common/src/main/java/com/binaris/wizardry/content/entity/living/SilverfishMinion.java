package com.binaris.wizardry.content.entity.living;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.EBEntities;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;

public final class SilverfishMinion extends Silverfish {
    private static final int MAX_GENERATION = 5;
    private int generation = 1;

    public SilverfishMinion(EntityType<? extends Silverfish> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public SilverfishMinion(Level level) {
        this(EBEntities.SILVERFISH_MINION.get(), level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1, false));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true,
                mob -> !(mob instanceof SilverfishMinion)));
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        var parent = Services.OBJECT_DATA.getMinionData(this);
        if (parent.isSummoned() && generation < MAX_GENERATION) {
            int count = 1 + random.nextInt(4);
            for (int i = 0; i < count; i++) {
                var child = new SilverfishMinion(level);
                child.setPos(victim.position());
                child.generation = generation + 1;
                var data = Services.OBJECT_DATA.getMinionData(child);
                data.setOwnerUUID(parent.getOwnerUUID());
                data.setSummoned(true);
                data.setShouldFollowOwner(parent.shouldFollowOwner());
                data.setLifetime(parent.getLifetime());
                level.addFreshEntity(child);
            }
        }
        return super.killedEntity(level, victim);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && (tickCount == 1 || deathTime == 1)) spawnDarkParticles(false);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.DISCARDED && !level().isClientSide && !isRemoved()) spawnDarkParticles(true);
        super.remove(reason);
    }

    private void spawnDarkParticles(boolean sendFromServer) {
        for (int i = 0; i < 15; i++) {
            ParticleBuilder.create(EBParticles.DARK_MAGIC).pos(getRandomX(1), getRandomY(), getRandomZ(1))
                    .color(0.3f, 0.3f, 0.3f).allowServer(sendFromServer).spawn(level());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Generation", generation);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        generation = Math.clamp(tag.getInt("Generation"), 1, MAX_GENERATION);
    }
}
