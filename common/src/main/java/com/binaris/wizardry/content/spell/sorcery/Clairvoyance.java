package com.binaris.wizardry.content.spell.sorcery;

import com.binaris.wizardry.api.content.data.StoredSpellVar;
import com.binaris.wizardry.api.content.data.VarPersistence;
import com.binaris.wizardry.api.content.item.ICastItem;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.networking.s2c.ClairvoyancePathS2C;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Clairvoyance extends Spell {
    public static final StoredSpellVar<GlobalPos> DESTINATION =
            new StoredSpellVar<>("clairvoyanceDestination", GlobalPos.CODEC, VarPersistence.ALWAYS);
    public static final int MAX_PATH_POINTS = 512;
    private static final String MESSAGE = "spell.ebwizardry.clairvoyance.";

    public Clairvoyance() { Services.OBJECT_DATA.spellStoredVariables(DESTINATION); }

    @Override
    public boolean requiresPacket() { return false; }

    public static InteractionResult remember(UseOnContext context) {
        var player = context.getPlayer();
        var stack = context.getItemInHand();
        if (player == null || !player.isShiftKeyDown() || !(stack.getItem() instanceof ICastItem item)
                || item.getCurrentSpell(stack) != Spells.CLAIRVOYANCE) return InteractionResult.PASS;
        var level = context.getLevel();
        var point = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getWorldBorder().isWithinBounds(point) || level.isOutsideBuildHeight(point)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            Services.OBJECT_DATA.getSpellManagerData(player).setVariable(DESTINATION, GlobalPos.of(level.dimension(), point));
            player.displayClientMessage(Component.translatable(MESSAGE + "confirm", Component.translatable("spell.ebwizardry.clairvoyance")), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean cast(PlayerCastContext context) {
        var caster = context.caster();
        if (!(context.world() instanceof ServerLevel level) || caster.isShiftKeyDown()) return false;
        var saved = Services.OBJECT_DATA.getSpellManagerData(caster).getVariable(DESTINATION);
        if (saved == null) {
            caster.displayClientMessage(Component.translatable(MESSAGE + "undefined"), true);
            return false;
        }
        if (!saved.dimension().equals(level.dimension())) {
            caster.displayClientMessage(Component.translatable(MESSAGE + "wrongdimension"), true);
            return false;
        }

        caster.displayClientMessage(Component.translatable(MESSAGE + "searching"), true);
        float range = context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE));
        List<BlockPos> points = findPath(level, caster.blockPosition(), saved.pos(), range);
        if (points.isEmpty()) {
            caster.displayClientMessage(Component.translatable(MESSAGE + "outofrange"), true);
            return false;
        }
        if (caster instanceof ServerPlayer player) {
            int duration = (int) context.modifiers().get(SpellModifiers.DURATION, property(DefaultProperties.DURATION));
            Services.NETWORK_HELPER.sendTo(player, new ClairvoyancePathS2C(points, duration));
        }
        playSound(level, caster, context.castingTicks(), -1);
        return true;
    }

    public static List<BlockPos> findPath(ServerLevel level, BlockPos start, BlockPos goal, float range) {
        range = Math.clamp(range, 0, MAX_PATH_POINTS);
        if (range == 0 || start.distSqr(goal) > range * range || !level.hasChunkAt(goal)
                || !level.getWorldBorder().isWithinBounds(goal)) return List.of();

        var probe = EntityType.ZOMBIE.create(level);
        if (probe == null) return List.of();
        probe.setPos(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
        probe.setOnGround(true);
        probe.setPathfindingMalus(PathType.WATER, 0);
        var evaluator = new WalkNodeEvaluator();
        evaluator.setCanPassDoors(true);
        evaluator.setCanOpenDoors(true);
        evaluator.setCanFloat(true);
        int extent = (int) Math.ceil(range) + 2;
        var region = new PathNavigationRegion(level, start.offset(-extent, -extent, -extent), start.offset(extent, extent, extent));
        var path = new PathFinder(evaluator, 5000).findPath(region, probe, Set.of(goal), range, 0, 1);
        if (path == null || !path.canReach() || path.getNodeCount() > MAX_PATH_POINTS) return List.of();

        var points = new ArrayList<BlockPos>(path.getNodeCount());
        for (int i = 0; i < path.getNodeCount(); i++) {
            var point = path.getNodePos(i);
            if (!level.hasChunkAt(point) || !level.getWorldBorder().isWithinBounds(point)) return List.of();
            points.add(point);
        }
        return points;
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.APPRENTICE, Elements.SORCERY, SpellTypes.UTILITY, SpellAction.POINT_UP, 20, 10, 100)
                .add(DefaultProperties.RANGE, 256f).add(DefaultProperties.DURATION, 1800).build();
    }
}
