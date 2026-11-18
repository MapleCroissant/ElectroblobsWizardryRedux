package com.binaris.wizardry.content.entity.projectile;

import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import com.binaris.wizardry.setup.registries.EBEntities;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public final class LargeMagicFireball extends MagicFireballEntity {
    public static final SpellProperty<Float> EXPLOSION_POWER = SpellProperty.floatProperty("explosion_power", 1);
    public float blastMultiplier = 1;
    private int lifetime = 16;

    public LargeMagicFireball(EntityType<LargeMagicFireball> type, Level level) {
        super(type, level);
    }

    public LargeMagicFireball(Level level) {
        this(EBEntities.LARGE_MAGIC_FIREBALL.get(), level);
    }

    @Override
    public int getLifeTime() { return lifetime; }

    @Override
    public float getDamage(@NotNull EntityHitResult result) {
        return Spells.GREATER_FIREBALL.property(DefaultProperties.DAMAGE);
    }

    @Override
    public ResourceKey<DamageType> getDamageType(@NotNull EntityHitResult result) {
        return result.getEntity() instanceof Ghast ? EBDamageSources.MAGIC : EBDamageSources.FIRE;
    }

    @Override
    protected void onHit(HitResult hit) {
        if (!level().isClientSide) {
            var point = hit.getLocation();
            boolean terrain = canDamageTerrain();
            level().explode(this, point.x, point.y, point.z,
                    Spells.GREATER_FIREBALL.property(EXPLOSION_POWER) * blastMultiplier,
                    terrain, terrain ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        }
        super.onHit(hit);
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        if (level().isClientSide) return;
        var adjacent = hit.getBlockPos().relative(hit.getDirection());
        if (canDamageTerrain() && level().isEmptyBlock(adjacent)
                && getOwner() instanceof LivingEntity caster && BlockUtil.canPlaceBlock(caster, level(), adjacent)) {
            var fire = Blocks.FIRE.defaultBlockState();
            if (fire.canSurvive(level(), adjacent)) level().setBlock(adjacent, fire, Block.UPDATE_ALL);
        }
        discard();
    }

    private boolean canDamageTerrain() {
        return getOwner() instanceof LivingEntity caster
                && (!(caster instanceof Player) || EBServerConfig.PLAYER_BLOCK_DAMAGE.get())
                && EntityUtil.canDamageBlocks(caster, level());
    }

    @Override
    public boolean isPickable() { return true; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || !(source.getEntity() instanceof LivingEntity deflector)) return false;
        if (!level().isClientSide) {
            setDeltaMovement(deflector.getLookAngle().scale(getDeltaMovement().length()));
            setOwner(deflector);
            lifetime = 160;
            hurtMarked = true;
        }
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("BlastMultiplier", blastMultiplier);
        tag.putInt("Lifetime", lifetime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BlastMultiplier")) blastMultiplier = tag.getFloat("BlastMultiplier");
        if (tag.contains("Lifetime")) lifetime = tag.getInt("Lifetime");
    }
}
