package com.binaris.wizardry.content.spell.lightning;

import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.EntityCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.util.BlockUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.content.entity.construct.LightningHammerConstruct;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.ConstructRangedSpell;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class LightningHammer extends ConstructRangedSpell<LightningHammerConstruct> {
    public static final SpellProperty<Integer> TARGET_LIMIT = SpellProperty.intProperty("secondary_max_targets", 8);
    public static final SpellProperty<Integer> ATTACK_INTERVAL = SpellProperty.intProperty("attack_interval", 40);
    public static final SpellProperty<Float> DIRECT_DAMAGE = SpellProperty.floatProperty("direct_damage", 10);
    public static final SpellProperty<Float> SPLASH_DAMAGE = SpellProperty.floatProperty("splash_damage", 6);

    public LightningHammer() {
        super(LightningHammerConstruct::new, false);
        floor(true);
        overlap(true);
        soundValues(3, 1, 0);
    }

    @Override
    public boolean cast(EntityCastContext context) {
        var caster = context.caster();
        var target = context.target();
        var level = context.world();
        double range = context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE));
        if (target == null || level.isClientSide || caster.distanceToSqr(target) >= range * range) return false;
        var hit = level.clip(new ClipContext(caster.getEyePosition(), target.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (hit.getType() != HitResult.Type.MISS) return false;
        Integer floor = BlockUtil.getNearestFloor(level, target.blockPosition(), 3);
        if (floor == null || !spawnConstruct(context, new Vec3(target.getX(), floor, target.getZ()), Direction.UP)) return false;
        caster.swing(context.hand());
        playSound(level, caster, context.castingTicks(), -1);
        return true;
    }

    @Override
    protected boolean spawnConstruct(CastContext context, Vec3 floor, Direction side) {
        var level = context.world();
        var start = floor.add(0, 50, 0);
        var pos = BlockPos.containing(start);
        return level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
                && !level.isOutsideBuildHeight(pos) && LightningBoltSpell.isOpenToSky(level, BlockPos.containing(floor))
                && super.spawnConstruct(context, start, side);
    }

    @Override
    protected void addConstructExtras(CastContext context, LightningHammerConstruct hammer, Direction side) {
        hammer.setDeltaMovement(0, -2, 0);
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.MASTER, Elements.LIGHTNING, SpellTypes.ATTACK, SpellAction.SUMMON, 100, 25, 300)
                .add(DefaultProperties.RANGE, 40f).add(DefaultProperties.DURATION, 600)
                .add(DefaultProperties.EFFECT_RADIUS, 10).add(TARGET_LIMIT, 8).add(ATTACK_INTERVAL, 40)
                .add(DIRECT_DAMAGE, 10f).add(SPLASH_DAMAGE, 6f).build();
    }
}
