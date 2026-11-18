package com.binaris.wizardry.content.entity.living;

import com.binaris.wizardry.api.content.entity.living.ISpellCaster;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.content.entity.goal.AttackSpellBasicGoal;
import com.binaris.wizardry.setup.registries.EBEntities;
import com.binaris.wizardry.setup.registries.EBSounds;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class Phoenix extends PathfinderMob implements ISpellCaster {
    private Spell continuous = Spells.NONE;
    private int spellCounter;

    public Phoenix(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public Phoenix(Level level) { this(EBEntities.PHOENIX.get(), level); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new AttackSpellBasicGoal<>(this, 15, 60, 140));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, LivingEntity.class, 15));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                entity -> entity instanceof Enemy && entity != this));
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            Integer floor = BlockUtil.getNearestFloor(level(), blockPosition(), 4);
            var motion = getDeltaMovement();
            double rise = floor == null || getY() - floor > 3 ? -0.1 : getY() - floor < 2 ? 0.1 : 0;
            setDeltaMovement(motion.x, rise, motion.z);
            if (tickCount % 22 == 0) playSound(EBSounds.ENTITY_PHOENIX_FLAP.get(), 1, 1);
            if (random.nextInt(24) == 0) playSound(EBSounds.ENTITY_PHOENIX_BURN.get(), 1 + random.nextFloat(), 0.3f + 0.7f * random.nextFloat());
        }
        super.tick();

        if (level().isClientSide) {
            int count = tickCount <= 1 || deathTime == 1 ? 15 : 2;
            for (int i = 0; i < count; i++) {
                level().addParticle(ParticleTypes.FLAME, getRandomX(0.5), getY() + getBbHeight() / 2
                        + random.nextDouble() * getBbHeight() / 2, getRandomZ(0.5), 0, -0.1, 0);
            }
        }
    }

    @Override
    public @NotNull List<Spell> getSpells() { return List.of(Spells.FLAME_RAY); }

    @Override
    public @NotNull Spell getContinuousSpell() { return continuous; }

    @Override
    public void setContinuousSpell(Spell spell) { continuous = spell; }

    @Override
    public int getSpellCounter() { return spellCounter; }

    @Override
    public void setSpellCounter(int count) { spellCounter = count; }

    @Override
    protected SoundEvent getAmbientSound() { return EBSounds.ENTITY_PHOENIX_AMBIENT.get(); }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return EBSounds.ENTITY_PHOENIX_HURT.get(); }

    @Override
    protected SoundEvent getDeathSound() { return EBSounds.ENTITY_PHOENIX_DEATH.get(); }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    public boolean displayFireAnimation() { return false; }
}
