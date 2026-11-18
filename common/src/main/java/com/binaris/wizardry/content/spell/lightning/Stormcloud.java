package com.binaris.wizardry.content.spell.lightning;

import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.EntityCastContext;
import com.binaris.wizardry.api.content.spell.internal.LocationCastContext;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.content.entity.construct.StormcloudConstruct;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.ConstructSpell;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Stormcloud extends ConstructSpell<StormcloudConstruct> {
    public Stormcloud() {
        super(StormcloudConstruct::new, false);
    }

    @Override
    public boolean cast(PlayerCastContext context) {
        var caster = context.caster();
        if (!caster.onGround() || !castAtFloor(context, caster.getEyePosition(), caster.getLookAngle())) return false;
        playSound(context.world(), caster, context.castingTicks(), -1);
        return true;
    }

    @Override
    public boolean cast(EntityCastContext context) {
        var target = context.target();
        double range = context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE));
        if (target == null || context.caster().distanceToSqr(target) > range * range) return false;
        var level = context.world();
        var hit = level.clip(new ClipContext(context.caster().getEyePosition(), target.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context.caster()));
        if (hit.getType() != HitResult.Type.MISS || !level.hasChunkAt(target.blockPosition())) return false;
        Integer floor = BlockUtil.getNearestFloor(level, target.blockPosition(), 3);
        if (floor == null || !spawnAt(context, new Vec3(target.getX(), floor, target.getZ()))) return false;
        playSound(level, context.caster(), context.castingTicks(), -1);
        return true;
    }

    @Override
    public boolean cast(LocationCastContext context) {
        if (!castAtFloor(context, context.vec3(), new Vec3(context.direction().step()))) return false;
        playSound(context.world(), context.vec3().subtract(new Vec3(context.direction().step())), context.castingTicks(), context.duration());
        return true;
    }

    private boolean castAtFloor(CastContext context, Vec3 start, Vec3 direction) {
        double range = context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE));
        var hit = context.world().clip(new ClipContext(start, start.add(direction.scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context.caster()));
        return hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP && spawnAt(context, hit.getLocation());
    }

    private boolean spawnAt(CastContext context, Vec3 floor) {
        var point = floor.add(0, 5, 0);
        var pos = BlockPos.containing(point);
        return !context.world().isOutsideBuildHeight(pos) && context.world().hasChunkAt(pos)
                && context.world().getWorldBorder().isWithinBounds(pos) && spawnConstruct(context, point, Direction.UP);
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.LIGHTNING, SpellTypes.CONSTRUCT, SpellAction.SUMMON, 60, 10, 150)
                .add(DefaultProperties.DURATION, 600).add(DefaultProperties.RANGE, 10f)
                .add(DefaultProperties.DAMAGE, 8f).add(DefaultProperties.EFFECT_RADIUS, 3).build();
    }
}
