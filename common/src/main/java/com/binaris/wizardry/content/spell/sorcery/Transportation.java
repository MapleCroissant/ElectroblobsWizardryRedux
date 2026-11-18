package com.binaris.wizardry.content.spell.sorcery;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.data.SpellVar;
import com.binaris.wizardry.api.content.data.StoredSpellVar;
import com.binaris.wizardry.api.content.data.VarPersistence;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.*;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class Transportation extends Spell {
    private static final SpellProperty<Integer> COUNTDOWN = SpellProperty.intProperty("teleport_countdown", 75);
    public static final StoredSpellVar<List<GlobalPos>> DESTINATIONS =
            new StoredSpellVar<>("transportationLocations", GlobalPos.CODEC.listOf(), VarPersistence.ALWAYS).setSynced();
    private static final SpellVar<Trip> PENDING = new SpellVar<Trip>(VarPersistence.NEVER).withTicker(Transportation::tick);
    private static final String SPELL = "spell.ebwizardry.transportation.";
    private static final String STONE = "block.ebwizardry.transportation_stone.";

    public Transportation() { Services.OBJECT_DATA.spellStoredVariables(DESTINATIONS); }

    @Override
    public boolean requiresPacket() { return false; }

    @Override
    public boolean cast(PlayerCastContext context) {
        var caster = context.caster();
        if (!(context.world() instanceof ServerLevel level)) return false;
        var data = Services.OBJECT_DATA.getSpellManagerData(caster);
        if (data.getVariable(PENDING) != null) return false;
        var locations = data.getVariable(DESTINATIONS);
        if (locations == null || locations.isEmpty()) return fail(caster, "undefined");

        boolean many = ArtifactChannel.isEquipped(caster, EBItems.CHARM_TRANSPORTATION.get());
        GlobalPos selected;
        if (many) {
            var inDimension = locations.stream().filter(point -> point.dimension().equals(level.dimension())).toList();
            if (inDimension.isEmpty()) return fail(caster, "wrongdimension");
            selected = inDimension.stream().filter(point -> deviation(caster, point.pos()) < iconAngle(caster, point.pos()))
                    .min(Comparator.comparingDouble(point -> deviation(caster, point.pos()))).orElse(null);
            if (selected == null) return false;
        } else {
            selected = locations.getLast();
            if (!selected.dimension().equals(level.dimension())) return fail(caster, "wrongdimension");
        }
        if (!circle(level, selected.pos())) return fail(caster, "missing");
        if (many) {
            var ordered = new ArrayList<>(locations);
            ordered.remove(selected);
            ordered.add(selected);
            data.setVariable(DESTINATIONS, ordered);
            data.sync();
        }

        caster.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 150, 0));
        data.setVariable(PENDING, new Trip(selected, Math.max(1, property(COUNTDOWN))));
        playSound(level, caster, context.castingTicks(), -1);
        return true;
    }

    private static Trip tick(Player player, Trip trip) {
        if (trip == null || !(player.level() instanceof ServerLevel level) || !player.isAlive()
                || !level.dimension().equals(trip.destination.dimension())) return null;
        if (trip.remaining > 1) return new Trip(trip.destination, trip.remaining - 1);
        var pos = trip.destination.pos();
        if (!circle(level, pos)) {
            fail(player, "missing");
            return null;
        }
        Vec3 destination = Vec3.atBottomCenterOf(pos);
        Vec3 displacement = destination.subtract(player.position());
        if (!level.noCollision(player, player.getBoundingBox().move(displacement))) {
            fail(player, "missing");
            return null;
        }
        var vehicle = player.getVehicle();
        player.stopRiding();
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.teleportTo(level, destination.x, destination.y, destination.z, Set.of(), player.getYRot(), player.getXRot());
        } else {
            player.teleportTo(destination.x, destination.y, destination.z);
        }
        if (vehicle != null && ArtifactChannel.isEquipped(player, EBItems.CHARM_MOUNT_TELEPORTING.get())
                && level.noCollision(vehicle, vehicle.getBoundingBox().move(destination.subtract(vehicle.position())))) {
            vehicle.teleportTo(destination.x, destination.y, destination.z);
            player.startRiding(vehicle, true);
        }
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
        for (int i = 0; i < 20; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            Vec3 sparkle = destination.add(Math.cos(angle), level.random.nextDouble() * 2, Math.sin(angle));
            ParticleBuilder.create(EBParticles.SPARKLE).pos(sparkle).velocity(0, 0.02, 0).color(0x99ff99)
                    .time(80 + level.random.nextInt(10)).allowServer(true).spawn(level);
            angle = level.random.nextDouble() * Math.PI * 2;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, destination.x + Math.cos(angle),
                    destination.y + level.random.nextDouble() * 2, destination.z + Math.sin(angle), 0, 0, 0.02, 0, 1);
        }
        level.playSound(null, destination.x, destination.y, destination.z, EBSounds.SPELL_TRANSPORTATION_TRAVEL.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
        return null;
    }

    public static void remember(Level level, BlockPos clicked, Player player) {
        BlockPos center = null;
        int best = -1;
        BlockPos preview = clicked;
        for (BlockPos candidate : BlockPos.betweenClosed(clicked.offset(-1, 0, -1), clicked.offset(1, 0, 1))) {
            if (candidate.equals(clicked)) continue;
            int stones = countStones(level, candidate);
            if (stones > best) {
                best = stones;
                preview = candidate.immutable();
            }
            if (stones == 8 && circle(level, candidate)) {
                center = candidate.immutable();
                break;
            }
        }
        if (center == null) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable(STONE + "invalid"), true);
            else for (BlockPos point : BlockPos.betweenClosed(preview.offset(-1, 0, -1), preview.offset(1, 0, 1))) {
                if (!point.equals(preview)) ParticleBuilder.create(EBParticles.PATH).pos(Vec3.atBottomCenterOf(point).add(0, 0.1875, 0))
                        .color(0x86ff65).time(200).scale(2).spawn(level);
            }
            return;
        }
        if (level.isClientSide) return;
        var data = Services.OBJECT_DATA.getSpellManagerData(player);
        var saved = data.getVariable(DESTINATIONS);
        var locations = saved == null ? new ArrayList<GlobalPos>() : new ArrayList<>(saved);
        var here = GlobalPos.of(level.dimension(), center);
        if (ArtifactChannel.isEquipped(player, EBItems.CHARM_TRANSPORTATION.get())) {
            if (locations.remove(here)) tell(player, STONE + "forget", here);
            else {
                locations.add(here);
                tell(player, STONE + "remember", here);
                if (locations.size() > 4) tell(player, STONE + "forget", locations.removeFirst());
            }
        } else {
            locations.remove(here);
            if (!locations.isEmpty()) locations.removeLast();
            locations.add(here);
            player.displayClientMessage(Component.translatable(STONE + "confirm", Component.translatable("spell.ebwizardry.transportation")), true);
        }
        data.setVariable(DESTINATIONS, locations);
        data.sync();
    }

    public static boolean circle(Level level, BlockPos center) {
        return level.getWorldBorder().isWithinBounds(center) && !level.getBlockState(center).blocksMotion()
                && !level.getBlockState(center.above()).blocksMotion() && countStones(level, center) == 8;
    }

    private static int countStones(Level level, BlockPos center) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            if (!pos.equals(center) && level.getBlockState(pos).is(EBBlocks.TRANSPORTATION_STONE.get())) count++;
        }
        return count;
    }

    public static double deviation(Player player, BlockPos point) {
        return Math.acos(Math.clamp(Vec3.atCenterOf(point).subtract(player.getEyePosition()).normalize().dot(player.getLookAngle()), -1, 1));
    }

    public static double iconAngle(Player player, BlockPos point) {
        return 0.05 + 2 / (Vec3.atCenterOf(point).distanceTo(player.getEyePosition()) + 5);
    }

    private static void tell(Player player, String key, GlobalPos point) {
        var pos = point.pos();
        player.displayClientMessage(Component.translatable(key, pos.getX(), pos.getY(), pos.getZ(), point.dimension().location()), true);
    }

    private static boolean fail(Player player, String reason) {
        player.displayClientMessage(Component.translatable(SPELL + reason), true);
        return false;
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.SORCERY, SpellTypes.UTILITY, SpellAction.POINT_UP, 100, 20, 100)
                .add(COUNTDOWN).build();
    }

    private record Trip(GlobalPos destination, int remaining) {}
}
