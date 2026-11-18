package com.binaris.wizardry.content.entity.construct;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.entity.construct.ScaledConstructEntity;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.entity.living.LightningWraith;
import com.binaris.wizardry.content.entity.living.StormElemental;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.setup.registries.*;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class StormcloudConstruct extends ScaledConstructEntity {
    public StormcloudConstruct(EntityType<?> type, Level level) {
        super(type, level);
        setBaseSize(Spells.STORMCLOUD.property(DefaultProperties.EFFECT_RADIUS) * 2, 2);
    }

    public StormcloudConstruct(Level level) {
        this(EBEntities.STORMCLOUD.get(), level);
    }

    @Override
    protected boolean shouldScaleHeight() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        var level = level();
        var caster = getCaster();
        boolean homing = caster instanceof Player player && ArtifactChannel.isEquipped(player, EBItems.RING_STORMCLOUD.get());
        if (level.isClientSide) {
            float area = getBbWidth() * getBbWidth() / 36;
            for (int i = 0; i < Math.min(32, 2 * area); i++) {
                ParticleBuilder.create(EBParticles.CLOUD, this).color(0.3f, 0.3f, 0.3f).shaded(true).spawn(level);
            }
        } else if (homing) {
            EntityUtil.getLivingWithinRadius(getBbWidth() * 3, getX(), getY(), getZ(), level).stream()
                    .filter(target -> target.isAlive() && isValidTarget(target))
                    .min(Comparator.comparingDouble(this::distanceToSqr)).ifPresent(target -> {
                        Vec3 delta = target.position().subtract(position()).normalize().multiply(0.2, 0, 0.2);
                        Vec3 next = position().add(delta);
                        if (level.hasChunkAt(BlockPos.containing(next)) && level.getWorldBorder().isWithinBounds(BlockPos.containing(next))) {
                            setPos(next);
                        }
                    });
        }

        for (var target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().expandTowards(0, -10, 0),
                entity -> entity.isAlive() && isValidTarget(entity) && entity.tickCount > 0 && entity.tickCount % 150 == 0)) {
            if (level.isClientSide) {
                ParticleBuilder.create(EBParticles.LIGHTNING).pos(target.getX(), getY() + getBbHeight() / 2, target.getZ())
                        .target(target).scale(2).spawn(level);
                ParticleBuilder.spawnShockParticles(level, target.getX(), target.getY() + target.getBbHeight(), target.getZ());
            } else {
                if (!(target instanceof StormElemental || target instanceof LightningWraith)) {
                    var damage = MagicDamageSource.causeIndirectMagicDamage(this, caster, EBDamageSources.SHOCK);
                    EntityUtil.attackEntityWithoutKnockback(target, damage, Spells.STORMCLOUD.property(DefaultProperties.DAMAGE) * damageMultiplier);
                }
                target.playSound(EBSounds.ENTITY_STORMCLOUD_THUNDER.get(), 1, 1.6f);
                target.playSound(EBSounds.ENTITY_STORMCLOUD_ATTACK.get(), 1, 1);
                if (homing) setLifetime(getLifetime() + 40);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CloudAge", tickCount);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tickCount = tag.getInt("CloudAge");
    }
}
