package com.binaris.wizardry.content.spell.shared;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.content.block.StatueBlock;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.RaySpell;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.binaris.wizardry.setup.registries.*;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class StatueRay extends RaySpell {
    private static final SpellProperty<Integer> MINIMUM_DURATION = SpellProperty.intProperty("minimum_effect_duration", 900);
    public enum Variant { ICE, STONE, WALL }

    private final Variant variant;

    public StatueRay(Variant variant) {
        this.variant = variant;
        assignProperties(propertiesFor(variant));
        switch (variant) {
            case ICE -> soundValues(1, 1.4f, 0.4f);
            case STONE -> soundValues(1, 1.1f, 0.2f);
            case WALL -> {
                soundValues(0.5f, 1, 0);
                particleVelocity(1);
                particleSpacing(0.5);
            }
        }
    }

    @Override
    public boolean isInstantCast() { return variant != Variant.WALL; }

    private int duration(CastContext context) {
        var key = switch (variant) {
            case ICE -> DefaultProperties.EFFECT_DURATION;
            case STONE -> MINIMUM_DURATION;
            case WALL -> DefaultProperties.DURATION;
        };
        return (int) context.modifiers().get(SpellModifiers.DURATION, property(key));
    }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) {
        if (!context.world().isClientSide && hit.getEntity() instanceof Mob mob) {
            var block = (StatueBlock) (variant == Variant.STONE ? EBBlocks.PETRIFIED_STONE : EBBlocks.ICE_STATUE).get();
            if (block.convert(mob, context.caster(), duration(context)) && variant != Variant.STONE) {
                var world = context.world();
                var sound = (variant == Variant.ICE ? EBSounds.SPELL_ICE_STATUE_FREEZE : EBSounds.MISC_FREEZE).get();
                world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), sound, SoundSource.PLAYERS, 1,
                        0.8f + 0.4f * world.random.nextFloat());
            }
        }
        return true;
    }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) {
        if (variant != Variant.WALL) return false;
        var world = context.world();
        var caster = context.caster();
        if (world.isClientSide || caster == null
                || caster instanceof Player && !EBServerConfig.PLAYER_BLOCK_DAMAGE.get()) return true;
        var pos = hit.getBlockPos();
        var side = hit.getDirection();
        if (BlockUtil.canBlockBeReplaced(world, pos, true)) pos = pos.relative(side.getOpposite());
        if (origin.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= 4
                || world.getBlockState(pos).is(EBBlocks.ICE_STATUE.get())
                || world.getBlockState(pos).is(EBBlocks.DRY_FROSTED_ICE.get())) return true;

        int duration = duration(context);
        if (duration <= 0) return true;
        var freezeAt = pos.relative(side);
        placeIce(caster, world, freezeAt, duration);
        if (side == Direction.UP) placeIce(caster, world, freezeAt.above(), duration);
        return true;
    }

    private void placeIce(LivingEntity caster, Level level, BlockPos pos, int duration) {
        var ice = EBBlocks.DRY_FROSTED_ICE.get();
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || level.getBlockEntity(pos) != null || !BlockUtil.canBlockBeReplaced(level, pos, true)
                || !BlockUtil.canPlaceBlock(caster, level, pos)) return;
        if (level.setBlockAndUpdate(pos, ice.defaultBlockState())) level.scheduleTick(pos, ice, duration);
    }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) { return true; }

    @Override
    protected void spawnParticle(CastContext context, double x, double y, double z, double vx, double vy, double vz) {
        var world = context.world();
        if (variant == Variant.STONE) {
            ParticleBuilder.create(EBParticles.SPARKLE).pos(x, y, z).time(12 + world.random.nextInt(8)).color(0.2f, 0.2f, 0.2f).spawn(world);
            ParticleBuilder.create(EBParticles.DARK_MAGIC).pos(x, y, z).color(0.1f, 0.1f, 0.1f).spawn(world);
        } else {
            float brightness = world.random.nextFloat();
            ParticleBuilder.create(EBParticles.SPARKLE).pos(x, y, z).velocity(vx, vy, vz).time(8 + world.random.nextInt(12))
                    .color(0.4f + 0.6f * brightness, 0.6f + 0.4f * brightness, 1f).spawn(world);
            ParticleBuilder.create(EBParticles.SNOW).pos(x, y, z).velocity(vx, vy, vz).time(12 + world.random.nextInt(12)).spawn(world);
        }
    }

    @Override
    protected void playSound(Level level, LivingEntity caster, int ticks, int duration) {
        if (variant == Variant.WALL) playSoundLoop(level, caster, ticks);
        else super.playSound(level, caster, ticks, duration);
    }

    @Override
    protected void playSound(Level level, double x, double y, double z, int ticks, int duration) {
        if (variant == Variant.WALL) playSoundLoop(level, x, y, z, ticks, duration);
        else if (variant == Variant.ICE) {
            if (!level.isClientSide) level.playSound(null, x, y, z, EBSounds.SPELL_ICE_STATUE_SHOOT.get(), SoundSource.PLAYERS,
                    1, 1.4f + (level.random.nextFloat() - 0.5f) * 0.4f);
        } else super.playSound(level, x, y, z, ticks, duration);
    }

    @Override
    protected SpellProperties properties() { return SpellProperties.empty(); }

    private static SpellProperties propertiesFor(Variant variant) {
        var builder = SpellProperties.builder().add(DefaultProperties.RANGE, 10f);
        return switch (variant) {
            case ICE -> builder.assignBaseProperties(SpellTiers.APPRENTICE, Elements.ICE, SpellTypes.ATTACK, SpellAction.POINT, 15, 0, 40)
                    .add(DefaultProperties.EFFECT_DURATION, 400).build();
            case STONE -> builder.assignBaseProperties(SpellTiers.ADVANCED, Elements.SORCERY, SpellTypes.ATTACK, SpellAction.POINT, 40, 20, 100)
                    .add(MINIMUM_DURATION).build();
            case WALL -> builder.assignBaseProperties(SpellTiers.MASTER, Elements.ICE, SpellTypes.UTILITY, SpellAction.POINT, 15, 15, 70)
                    .add(DefaultProperties.DURATION, 600).build();
        };
    }
}
