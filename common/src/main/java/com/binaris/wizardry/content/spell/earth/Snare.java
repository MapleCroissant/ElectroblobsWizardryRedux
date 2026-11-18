package com.binaris.wizardry.content.spell.earth;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.content.blockentity.SnareBlockEntity;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.RaySpell;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.binaris.wizardry.setup.registries.EBBlocks;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Snare extends RaySpell {
    public Snare() {
        ignoreLivingEntities(true);
        soundValues(1, 1.4f, 0.4f);
    }

    @Override
    protected boolean shootSpell(CastContext context, Vec3 origin, Vec3 direction) {
        var end = origin.add(direction.scale(context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE))));
        var hit = context.world().clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context.caster()));
        if (hit.getType() != HitResult.Type.BLOCK || !onBlockHit(context, hit, origin)) return false;
        if (context.world().isClientSide) spawnParticleRay(context, origin, direction, origin.distanceTo(hit.getLocation()));
        return true;
    }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) {
        var level = context.world();
        var to = hit.getBlockPos().above();
        var state = EBBlocks.SNARE.get().defaultBlockState();
        if (hit.getDirection() != Direction.UP || level.isOutsideBuildHeight(to) || !level.hasChunkAt(to)
                || !level.getWorldBorder().isWithinBounds(to) || !BlockUtil.canBlockBeReplaced(level, to, true)
                || !state.canSurvive(level, to)) return false;
        if (level.isClientSide) return true;
        var caster = context.caster();
        if (caster != null && ((caster instanceof Player && !EBServerConfig.PLAYER_BLOCK_DAMAGE.get())
                || !BlockUtil.canPlaceBlock(caster, level, to))) return false;
        if (!level.setBlock(to, state, Block.UPDATE_ALL)) return false;
        if (level.getBlockEntity(to) instanceof SnareBlockEntity snare) snare.setCaster(caster);
        return true;
    }

    @Override
    protected void spawnParticle(CastContext context, double x, double y, double z, double vx, double vy, double vz) {
        var world = context.world();
        float shade = world.random.nextFloat() * 0.25f;
        ParticleBuilder.create(EBParticles.SPARKLE).pos(x, y, z).time(20 + world.random.nextInt(8))
                .color(shade, shade + 0.1f, 0).spawn(world);
        ParticleBuilder.create(EBParticles.LEAF).pos(x, y, z).velocity(0, -0.01, 0).time(40 + world.random.nextInt(10)).spawn(world);
    }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) { return false; }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) { return false; }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.NOVICE, Elements.EARTH, SpellTypes.ATTACK, SpellAction.POINT, 10, 0, 10)
                .add(DefaultProperties.RANGE, 10f).add(DefaultProperties.DAMAGE, 6f)
                .add(DefaultProperties.EFFECT_DURATION, 100).add(DefaultProperties.EFFECT_STRENGTH, 2).build();
    }
}
