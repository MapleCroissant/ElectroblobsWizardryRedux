package com.binaris.wizardry.content.spell.shared;

import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.content.entity.construct.TotemConstruct;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.ConstructRangedSpell;
import com.binaris.wizardry.setup.registries.EBEntities;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class TotemSpell extends ConstructRangedSpell<TotemConstruct> {
    public static final SpellProperty<Integer> MAX_TARGETS = SpellProperty.intProperty("max_targets", 1);
    public static final SpellProperty<Float> MAX_BURST_DAMAGE = SpellProperty.floatProperty("max_explosion_damage", 10);


    public TotemSpell(boolean withering) {
        super(level -> new TotemConstruct((withering ? EBEntities.WITHERING_TOTEM : EBEntities.RADIANT_TOTEM).get(), level), false);
        assignProperties(propertiesFor(withering));
    }

    @Override
    protected boolean spawnConstruct(CastContext context, Vec3 base, Direction side) {
        var position = base.add(0, 1.2, 0);
        var level = context.world();
        var pos = BlockPos.containing(position);
        return level.hasChunkAt(pos) && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                && super.spawnConstruct(context, position, side);
    }

    private static SpellProperties propertiesFor(boolean withering) {
        var builder = SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, withering ? Elements.NECROMANCY : Elements.HEALING,
                        SpellTypes.CONSTRUCT, SpellAction.SUMMON, withering ? 50 : 40, withering ? 20 : 15, withering ? 200 : 175)
                .add(DefaultProperties.RANGE, 8f).add(DefaultProperties.DURATION, 600)
                .add(DefaultProperties.EFFECT_RADIUS, 6).add(MAX_TARGETS).add(DefaultProperties.DAMAGE, 1f);
        if (withering) {
            builder.add(MAX_BURST_DAMAGE).add(DefaultProperties.EFFECT_DURATION, 120).add(DefaultProperties.EFFECT_STRENGTH, 0);
        } else {
            builder.add(DefaultProperties.HEALTH, 1f);
        }
        return builder.build();
    }
}
