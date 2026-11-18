package com.binaris.wizardry.content.spell.earth;

import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.*;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.content.blockentity.ThornsBlockEntity;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.setup.registries.EBBlocks;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.binaris.wizardry.core.config.EBServerConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class ForestOfThorns extends Spell {
    @Override
    public boolean requiresPacket() { return false; }

    @Override
    public boolean canCastByEntity() { return true; }

    @Override
    public boolean canCastByLocation() { return true; }

    @Override
    public boolean cast(PlayerCastContext context) { return castAroundCaster(context); }

    @Override
    public boolean cast(EntityCastContext context) { return castAroundCaster(context); }

    private boolean castAroundCaster(CastContext context) {
        var caster = context.caster();
        if (!grow(context, caster.blockPosition())) return false;
        playSound(context.world(), caster, context.castingTicks(), -1);
        return true;
    }

    @Override
    public boolean cast(LocationCastContext context) {
        if (!grow(context, context.pos().relative(context.direction()))) return false;
        playSound(context.world(), context.vec3(), context.castingTicks(), context.duration());
        return true;
    }

    private boolean grow(CastContext context, BlockPos center) {
        var level = context.world();
        if (level.isClientSide) return true;
        var caster = context.caster();
        var modifiers = context.modifiers();
        if (caster instanceof Player && !EBServerConfig.PLAYER_BLOCK_DAMAGE.get()) return false;
        if (caster == null && !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;
        float radius = Math.clamp(modifiers.get(SpellModifiers.BLAST, property(DefaultProperties.EFFECT_RADIUS)), 0, 24);
        int duration = (int) modifiers.get(SpellModifiers.DURATION, property(DefaultProperties.DURATION));
        if (radius <= 0 || duration <= 0) return false;
        var block = EBBlocks.THORNS.get();
        boolean placed = false;
        for (int x = -(int) radius; x <= radius; x++) {
            for (int z = -(int) radius; z <= radius; z++) {
                double distance = Math.hypot(x, z);
                if (distance > radius || distance < radius - 1.5) continue;
                var column = center.offset(x, 0, z);
                if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) continue;
                Integer floor = BlockUtil.getNearestFloor(level, column, (int) radius);
                if (floor == null) continue;
                var lower = column.atY(floor);
                var upper = lower.above();
                if (level.isOutsideBuildHeight(upper)
                        || !level.getBlockState(lower.below()).isFaceSturdy(level, lower.below(), Direction.UP)
                        || !BlockUtil.canBlockBeReplaced(level, lower, true) || !BlockUtil.canBlockBeReplaced(level, upper, true)
                        || level.getBlockEntity(lower) != null || level.getBlockEntity(upper) != null
                        || (caster != null && (!BlockUtil.canPlaceBlock(caster, level, lower) || !BlockUtil.canPlaceBlock(caster, level, upper)))) continue;
                var oldLower = level.getBlockState(lower);
                if (!level.setBlock(lower, block.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) continue;
                if (!level.setBlock(upper, block.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                        DoubleBlockHalf.UPPER), Block.UPDATE_ALL)) {
                    level.setBlock(lower, oldLower, Block.UPDATE_ALL);
                    continue;
                }
                if (level.getBlockEntity(lower) instanceof ThornsBlockEntity thorns) {
                    thorns.configure(caster, duration, modifiers.getFactor(SpellModifiers.POTENCY));
                    level.scheduleTick(lower, block, Math.min(2, duration));
                    level.updateNeighborsAt(lower, block);
                    placed = true;
                }
            }
        }
        return placed;
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.MASTER, Elements.EARTH, SpellTypes.CONSTRUCT, SpellAction.SUMMON, 100, 20, 250)
                .add(DefaultProperties.EFFECT_RADIUS, 3).add(DefaultProperties.DURATION, 600).add(DefaultProperties.DAMAGE, 1f).build();
    }
}
