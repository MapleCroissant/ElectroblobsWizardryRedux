package com.binaris.wizardry.content.spell.sorcery;

import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.entity.construct.BlackHoleConstruct;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.ConstructRangedSpell;
import com.binaris.wizardry.setup.registries.EBEntities;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class BlackHole extends ConstructRangedSpell<BlackHoleConstruct> {
    public BlackHole() {
        super(level -> new BlackHoleConstruct(EBEntities.BLACK_HOLE.get(), level), false);
        soundValues(2, 0.7f, 0);
    }

    @Override
    protected boolean spawnConstruct(CastContext context, Vec3 point, Direction side) {
        var level = context.world();
        var pos = BlockPos.containing(point);
        return level.hasChunkAt(pos) && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                && super.spawnConstruct(context, point, side);
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.MASTER, Elements.SORCERY, SpellTypes.CONSTRUCT, SpellAction.SUMMON, 150, 25, 400)
                .add(DefaultProperties.EFFECT_RADIUS, 3).add(DefaultProperties.RANGE, 10f).add(DefaultProperties.DURATION, 400).build();
    }
}
