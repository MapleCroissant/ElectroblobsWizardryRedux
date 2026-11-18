package com.binaris.wizardry.content.spell.sorcery;

import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.api.content.util.RayTracer;
import com.binaris.wizardry.content.blockentity.MagicLightBlockEntity;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.setup.registries.EBBlocks;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class Light extends Spell {
    @Override
    public boolean requiresPacket() {
        return false;
    }

    @Override
    public boolean cast(PlayerCastContext context) {
        var player = context.caster();
        var level = context.world();
        double range = context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE));
        HitResult hit = RayTracer.standardBlockRayTrace(level, player, range, false, true, false);
        BlockPos pos = hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                ? blockHit.getBlockPos().relative(blockHit.getDirection()) : BlockPos.containing(hit.getLocation());
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.isEmptyBlock(pos)) return false;
        if (level.isClientSide) return true;
        if (!BlockUtil.canPlaceBlock(player, level, pos) || !level.setBlock(pos, EBBlocks.MAGIC_LIGHT.get().defaultBlockState(), Block.UPDATE_ALL)) return false;

        if (level.getBlockEntity(pos) instanceof MagicLightBlockEntity light) {
            int duration = (int) context.modifiers().get(SpellModifiers.DURATION, property(DefaultProperties.DURATION));
            light.setLifetime(ArtifactChannel.isEquipped(player, EBItems.CHARM_LIGHT.get()) ? -1 : Math.max(1, duration));
        }
        playSound(level, player, 0, -1);
        return true;
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.NOVICE, Elements.SORCERY, SpellTypes.UTILITY, SpellAction.POINT, 5, 0, 15)
                .add(DefaultProperties.RANGE, 4f)
                .add(DefaultProperties.DURATION, 600)
                .build();
    }
}
