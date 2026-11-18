package com.binaris.wizardry.content.item;

import com.binaris.wizardry.api.content.event.SpellCastEvent;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellContexts;
import com.binaris.wizardry.api.content.spell.internal.LocationCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.util.RegistryUtils;
import com.binaris.wizardry.core.event.WizardryEventBus;
import com.binaris.wizardry.core.networking.s2c.DispenserCastS2C;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class ScrollDispenseBehavior extends OptionalDispenseItemBehavior {
    private static final SpellCastEvent.Sources SOURCE = SpellCastEvent.Sources.DISPENSER;
    // Server-thread state. The values hold no reference to the level.
    private static final Map<ServerLevel, Map<BlockPos, Channel>> CHANNELS = new WeakHashMap<>();

    public static void register() {
        DispenserBlock.registerBehavior(EBItems.SCROLL.get(), new ScrollDispenseBehavior());
    }

    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        setSuccess(false);
        ServerLevel level = source.level();
        BlockPos pos = source.pos();
        Direction direction = source.state().getValue(DispenserBlock.FACING);
        if (blocked(level, pos, direction) || CHANNELS.getOrDefault(level, Map.of()).containsKey(pos)) return stack;

        Spell spell = RegistryUtils.getSpell(stack);
        if (spell == Spells.NONE || !spell.canCastByLocation() || !spell.isEnabled(SpellContexts.DISPENSERS)) {
            setSuccess(true);
            return super.execute(source, stack);
        }

        int duration = spell.isInstantCast() ? 0 : ScrollItem.CASTING_TIME;
        var modifiers = new SpellModifiers();
        var context = context(level, pos, direction, modifiers, 0, duration);
        if (WizardryEventBus.fireEvent(new SpellCastEvent.Pre(SOURCE, spell, context)) || !spell.cast(context)) return stack;

        stack.shrink(1);
        setSuccess(true);
        WizardryEventBus.fireEvent(new SpellCastEvent.Post(SOURCE, spell, context));
        broadcast(level, pos, direction, spell, context, false);
        if (!spell.isInstantCast()) {
            CHANNELS.computeIfAbsent(level, ignored -> new HashMap<>())
                    .put(pos.immutable(), new Channel(spell, direction, modifiers, level.getGameTime(), duration, 0));
        }
        return stack;
    }

    public static void tick(ServerLevel level) {
        var channels = CHANNELS.get(level);
        if (channels == null) return;
        var iterator = channels.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            BlockPos pos = entry.getKey();
            Channel cast = entry.getValue();
            if (cast.startedAt == level.getGameTime()) continue;
            int tick = cast.tick + 1;
            int duration = cast.duration;
            var dispenser = level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.DISPENSER)
                    && level.getBlockState(pos).getValue(DispenserBlock.FACING) == cast.direction
                    && level.getBlockEntity(pos) instanceof DispenserBlockEntity entity ? entity : null;
            boolean ready = dispenser != null && (level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above()))
                    && !blocked(level, pos, cast.direction);
            if (ready && tick >= duration && replenish(dispenser, cast.spell)) duration += ScrollItem.CASTING_TIME;

            var context = context(level, pos, cast.direction, cast.modifiers, tick, duration);
            if (ready && tick < duration && !WizardryEventBus.fireEvent(new SpellCastEvent.Tick(SOURCE, cast.spell, context))
                    && cast.spell.cast(context)) {
                entry.setValue(new Channel(cast.spell, cast.direction, cast.modifiers, cast.startedAt, duration, tick));
                broadcast(level, pos, cast.direction, cast.spell, context, false);
            } else {
                iterator.remove();
                WizardryEventBus.fireEvent(new SpellCastEvent.Finish(SOURCE, cast.spell, context));
                cast.spell.endCast(context);
                if (level.hasChunkAt(pos)) broadcast(level, pos, cast.direction, cast.spell, context, true);
            }
        }
        if (channels.isEmpty()) CHANNELS.remove(level);
    }

    private static boolean replenish(DispenserBlockEntity dispenser, Spell spell) {
        var slots = new ArrayList<Integer>();
        for (int slot = 0; slot < dispenser.getContainerSize(); slot++) {
            ItemStack stack = dispenser.getItem(slot);
            if (stack.getItem() instanceof ScrollItem scroll && scroll.getCurrentSpell(stack) == spell) slots.add(slot);
        }
        if (slots.isEmpty()) return false;
        dispenser.removeItem(slots.get(dispenser.getLevel().random.nextInt(slots.size())), 1);
        dispenser.setChanged();
        return true;
    }

    private static boolean blocked(Level level, BlockPos pos, Direction direction) {
        var front = pos.relative(direction);
        return level.getBlockState(front).isFaceSturdy(level, front, direction.getOpposite());
    }

    private static void broadcast(ServerLevel level, BlockPos pos, Direction direction, Spell spell,
                                  LocationCastContext context, boolean finished) {
        if (spell.requiresPacket() || !spell.isInstantCast()) {
            Services.NETWORK_HELPER.sendToTracking(level, pos, new DispenserCastS2C(pos, direction, spell.getLocation(),
                    context.castingTicks(), context.duration(), context.modifiers(), finished));
        }
    }

    public static LocationCastContext context(Level level, BlockPos pos, Direction direction, SpellModifiers modifiers,
                                              int tick, int duration) {
        Vec3 origin = Vec3.atCenterOf(pos).relative(direction, 0.7);
        if (direction.getAxis().isHorizontal()) origin = origin.subtract(0, 0.125, 0);
        return new LocationCastContext(level, origin.x, origin.y, origin.z, direction, tick, duration, modifiers);
    }

    private record Channel(Spell spell, Direction direction, SpellModifiers modifiers, long startedAt, int duration, int tick) {}
}
