package com.binaris.wizardry.content.spell.sorcery;

import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.EntityCastContext;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.RayTracer;
import com.binaris.wizardry.api.content.util.VecUtils;
import com.binaris.wizardry.client.effect.BlinkOverlay;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Blink extends Spell {
    @Override
    public boolean cast(PlayerCastContext context) {
        var player = context.caster();
        Level level = context.world();
        if (level.isClientSide) {
            departureParticles(player);
            BlinkOverlay.start(player);
            return true;
        }

        boolean mount = player.isPassenger() && ArtifactChannel.isEquipped(player, EBItems.CHARM_MOUNT_TELEPORTING.get());
        var vehicle = mount ? player.getVehicle() : player;
        double range = context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE));
        var hit = RayTracer.standardBlockRayTrace(level, player, range, vehicle instanceof Boat, true, false);
        if (!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return false;

        BlockPos destination = blockHit.getBlockPos().relative(blockHit.getDirection());
        if (!level.hasChunkAt(destination) || !level.getWorldBorder().isWithinBounds(destination)) return false;
        Vec3 space = EntityUtil.findSpaceForTeleport(vehicle, VecUtils.getFaceCentre(destination, Direction.DOWN), mount);
        if (space == null) return false;

        playSound(level, player, 0, -1);
        if (!mount && player.isPassenger()) player.stopRiding();
        vehicle.teleportTo(space.x, space.y, space.z);
        playSound(level, player, 0, -1);
        return true;
    }

    @Override
    public boolean canCastByEntity() {
        return true;
    }

    @Override
    public boolean cast(EntityCastContext context) {
        var caster = context.caster();
        var target = context.target();
        var level = context.world();
        if (target == null) return false;
        if (level.isClientSide) {
            departureParticles(caster);
            return true;
        }

        double angle = Mth.atan2(target.getZ() - caster.getZ(), target.getX() - caster.getX()) + level.random.nextDouble() * Math.PI;
        double distance = caster.distanceTo(target) + level.random.nextDouble() * 3;
        int x = Mth.floor(target.getX() + Math.sin(angle) * distance);
        int z = Mth.floor(target.getZ() - Math.cos(angle) * distance);
        var column = new BlockPos(x, caster.getBlockY(), z);
        if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) return false;
        Integer y = BlockUtil.getNearestFloor(level, column, Mth.ceil(distance));
        if (y == null) return false;
        var floor = new BlockPos(x, y, z);
        if (!level.getBlockState(floor).blocksMotion()) floor = floor.below();
        if (level.getBlockState(floor.above()).blocksMotion() || level.getBlockState(floor.above(2)).blocksMotion()) return false;

        playSound(level, caster, 0, -1);
        caster.teleportTo(x + 0.5, floor.getY() + 1, z + 0.5);
        playSound(level, caster, 0, -1);
        caster.swing(context.hand());
        return true;
    }

    private static void departureParticles(LivingEntity entity) {
        Level level = entity.level();
        for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.PORTAL, entity.getX(), entity.getY() + entity.getBbHeight() * level.random.nextFloat(),
                    entity.getZ(), level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5);
        }
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.APPRENTICE, Elements.SORCERY, SpellTypes.UTILITY, SpellAction.POINT, 15, 0, 25)
                .add(DefaultProperties.RANGE, 25f).build();
    }
}
