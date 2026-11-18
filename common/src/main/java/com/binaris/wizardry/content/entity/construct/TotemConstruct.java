package com.binaris.wizardry.content.entity.construct;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.entity.construct.ScaledConstructEntity;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.shared.TotemSpell;
import com.binaris.wizardry.core.AllyDesignation;
import com.binaris.wizardry.core.ClientSpellSoundManager;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.binaris.wizardry.setup.registries.*;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Comparator;

public final class TotemConstruct extends ScaledConstructEntity {
    private static final EntityDataAccessor<Float> DRAINED = SynchedEntityData.defineId(TotemConstruct.class, EntityDataSerializers.FLOAT);

    public TotemConstruct(EntityType<?> type, Level level) { super(type, level); }

    public boolean withering() { return getType() == EBEntities.WITHERING_TOTEM.get(); }

    public float healthDrained() { return entityData.get(DRAINED); }

    private Spell spell() { return withering() ? Spells.WITHERING_TOTEM : Spells.RADIANT_TOTEM; }

    private float radius() { return Math.clamp(spell().property(DefaultProperties.EFFECT_RADIUS) * sizeMultiplier, 0, 32); }

    @Override
    protected boolean shouldScaleWidth() { return false; }

    @Override
    protected boolean shouldScaleHeight() { return false; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DRAINED, 0f);
    }

    @Override
    public void tick() {
        if (!level().isClientSide && getLifetime() >= 0 && tickCount >= getLifetime()) {
            despawn();
            return;
        }
        super.tick();
        if (isRemoved()) return;
        if (level().isClientSide) {
            if (tickCount == 1) ClientSpellSoundManager.playMovingSound(this,
                    (withering() ? EBSounds.ENTITY_WITHERING_TOTEM_AMBIENT : EBSounds.ENTITY_RADIANT_TOTEM_AMBIENT).get(),
                    SoundSource.PLAYERS, 1, 1, true);
            particles();
            return;
        }
        float step = EBServerConfig.POTENCY_INCREASE_PER_TIER.get();
        int bonus = step > 0 ? (int) ((damageMultiplier - 1) / step) : 0;
        int limit = Math.clamp(spell().property(TotemSpell.MAX_TARGETS) + bonus, 0, 32);
        var caster = getCaster();
        var targets = EntityUtil.getLivingWithinRadius(radius(), getX(), getY(), getZ(), level());
        targets.stream().filter(LivingEntity::isAlive)
                .filter(target -> isValidTarget(target) || canHeal(caster, target))
                .sorted(Comparator.<LivingEntity, Boolean>comparing(target -> !canHeal(caster, target))
                        .thenComparingDouble(this::distanceToSqr))
                .limit(limit).forEach(target -> affect(caster, target));
    }

    private boolean canHeal(LivingEntity caster, LivingEntity target) {
        return !withering() && caster != null && target.getHealth() < target.getMaxHealth()
                && (target == caster || AllyDesignation.isAllied(caster, target));
    }

    private void affect(LivingEntity caster, LivingEntity target) {
        if (canHeal(caster, target)) {
            if (target.tickCount % 8 == 0) target.heal(spell().property(DefaultProperties.HEALTH));
        } else if (target.tickCount % Math.max(1, target.invulnerableDuration) == 1) {
            float damage = spell().property(DefaultProperties.DAMAGE);
            var source = MagicDamageSource.causeIndirectMagicDamage(this, caster, withering() ? EBDamageSources.WITHER : EBDamageSources.RADIANT);
            if (EntityUtil.attackEntityWithoutKnockback(target, source, damage) && withering()) {
                entityData.set(DRAINED, healthDrained() + damage);
            }
        }
        if (tickCount % 3 == 0) {
            var center = getBoundingBox().getCenter();
            ParticleBuilder.create(EBParticles.BEAM).pos(center).target(target)
                    .color(withering() ? 0x39004d : 0xffb633).time(4).allowServer(true).spawn(level());
        }
    }

    private void particles() {
        int color = withering() ? 0xf575f5 : 0xffffff;
        int fade = withering() ? 0x382366 : 0xffec90;
        ParticleBuilder.create(EBParticles.DUST, this).pos(getX(), getY() + 0.2, getZ())
                .velocity(0, -0.02 - random.nextFloat() * 0.01, 0).color(color).fade(fade).spawn(level());
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * (i + random.nextFloat());
            double x = getX() + radius() * Math.sin(angle);
            double z = getZ() + radius() * Math.cos(angle);
            var position = BlockPos.containing(x, getY(), z);
            if (!level().hasChunkAt(position)) continue;
            Integer floor = BlockUtil.getNearestFloor(level(), position, 5);
            if (floor != null) ParticleBuilder.create(EBParticles.DUST).pos(x, floor, z)
                    .velocity(0, 0.01, 0).color(color).fade(fade).spawn(level());
        }
    }

    @Override
    public void despawn() {
        if (isRemoved()) return;
        if (!level().isClientSide) {
            if (withering()) {
                float damage = Math.min(healthDrained() * 0.2f, spell().property(TotemSpell.MAX_BURST_DAMAGE));
                var source = MagicDamageSource.causeIndirectMagicDamage(this, getCaster(), EBDamageSources.MAGIC);
                for (var target : EntityUtil.getLivingWithinRadius(radius(), getX(), getY(), getZ(), level())) {
                    if (damage > 0 && target.isAlive() && isValidTarget(target)
                            && EntityUtil.attackEntityWithoutKnockback(target, source, damage)) {
                        target.addEffect(new MobEffectInstance(MobEffects.WITHER, spell().property(DefaultProperties.EFFECT_DURATION),
                                spell().property(DefaultProperties.EFFECT_STRENGTH)));
                    }
                }
                ParticleBuilder.create(EBParticles.SPHERE).pos(getBoundingBox().getCenter()).scale(radius())
                        .color(0xbe1a53).fade(0x210f4a).allowServer(true).spawn(level());
            }
            playSound((withering() ? EBSounds.ENTITY_WITHERING_TOTEM_EXPLODE : EBSounds.ENTITY_RADIANT_TOTEM_VANISH).get(), 1, 1);
        }
        super.despawn();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TotemAge", tickCount);
        tag.putFloat("HealthDrained", healthDrained());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tickCount = tag.getInt("TotemAge");
        entityData.set(DRAINED, tag.getFloat("HealthDrained"));
    }
}
