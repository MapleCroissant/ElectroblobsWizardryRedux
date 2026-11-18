package com.binaris.wizardry.content.entity.projectile;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.entity.projectile.MagicProjectileEntity;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.EBSounds;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public final class LightningDiscEntity extends MagicProjectileEntity {
    public static final com.binaris.wizardry.api.content.spell.properties.SpellProperty<Float> SEEKING_STRENGTH =
            com.binaris.wizardry.api.content.spell.properties.SpellProperty.floatProperty("seeking_strength", 5);

    public LightningDiscEntity(Level level) {
        this(com.binaris.wizardry.setup.registries.EBEntities.LIGHTNING_DISC.get(), level);
    }
    public LightningDiscEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    public int getLifeTime() { return 30; }

    @Override
    public float getSeekingStrength() { return com.binaris.wizardry.setup.registries.Spells.LIGHTNING_DISC.property(SEEKING_STRENGTH); }

    @Override
    public boolean isNoGravity() { return true; }

    @Override
    public boolean doOverpenetration() { return true; }

    @Override
    public float getDamage(@NotNull EntityHitResult result) { return com.binaris.wizardry.setup.registries.Spells.LIGHTNING_DISC.property(com.binaris.wizardry.content.spell.DefaultProperties.DAMAGE); }

    @Override
    public ResourceKey<DamageType> getDamageType(@NotNull EntityHitResult result) { return EBDamageSources.SHOCK; }

    @Override
    public @NotNull SoundEvent getSoundEvent(HitResult result) { return EBSounds.ENTITY_LIGHTNING_DISC_HIT.get(); }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().scale(1 / 0.99));
        if (level().isClientSide) {
            for (int i = 0; i < 8; i++) {
                ParticleBuilder.create(EBParticles.SPARK)
                        .pos(getX() + random.nextDouble() * 2 - 1, getY(), getZ() + random.nextDouble() * 2 - 1)
                        .spawn(level());
            }
        }
    }
}
