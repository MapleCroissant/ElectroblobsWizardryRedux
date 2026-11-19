package com.binaris.wizardry.content.spell.healing;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.item.ICastItem;
import com.binaris.wizardry.api.content.spell.*;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.api.content.util.CastItemDataHelper;
import com.binaris.wizardry.content.item.ScrollItem;
import com.binaris.wizardry.content.item.WandItem;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.*;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;

public final class Resurrection extends Spell {
    private static final SpellProperty<Integer> WAIT_TIME = SpellProperty.intProperty("wait_time", 200);
    private static final String KEY = "ebwizardry_resurrection_death";
    private static final Map<ServerPlayer, Death> DEATHS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Integer> UNRELEASED = new WeakHashMap<>();
    private static final Map<ServerPlayer, Vec3> REVIVING = new WeakHashMap<>();

    @Override
    public boolean requiresPacket() { return false; }

    @Override
    public boolean cast(PlayerCastContext context) {
        if (!(context.caster() instanceof ServerPlayer caster)) return false;
        if (!caster.isAlive()) {
            // Only the authenticated death-screen request can authorize a cast from a dead body.
            return REVIVING.containsKey(caster);
        }
        double radius = context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.EFFECT_RADIUS));
        int wait = (int)(property(WAIT_TIME) / Math.max(0.01f, context.modifiers().getFactor(SpellModifiers.POTENCY)));
        var allies = Services.OBJECT_DATA.getWizardData(caster);
        var target = caster.server.getPlayerList().getPlayers().stream()
                .filter(player -> player != caster && player.serverLevel() == caster.serverLevel())
                .filter(player -> caster.distanceToSqr(player) < radius * radius && allies.isPlayerAlly(player))
                .filter(player -> ready(player, wait))
                .min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);
        if (target == null) return false;
        Vec3 landing = landing(target);
        if (landing == null) return false;
        if (com.binaris.wizardry.core.event.WizardryEventBus.fireEvent(new com.binaris.wizardry.api.content.event.EBPlayerResurrectionEvent(target, caster))) return false;
        return revive(target, caster, landing) != null;
    }

    public static int findCastingSlot(ServerPlayer player) {
        var context = selfContext(player);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!supports(stack)) continue;
            if (stack.getItem() instanceof WandItem wand) {
                Spell selected = wand.getCurrentSpell(stack);
                CastItemDataHelper.setCurrentSpell(stack, Spells.RESURRECTION);
                try {
                    if (wand.canCastRequirements(stack, Spells.RESURRECTION, context)) return i;
                } finally {
                    CastItemDataHelper.setCurrentSpell(stack, selected);
                }
            } else if (!player.getCooldowns().isOnCooldown(stack.getItem())) return i;
        }
        return -1;
    }

    public static boolean supports(ItemStack stack) {
        return (stack.getItem() instanceof WandItem && Spells.RESURRECTION.isEnabled(SpellContexts.WANDS)
                || stack.getItem() instanceof ScrollItem && Spells.RESURRECTION.isEnabled(SpellContexts.SCROLL))
                && stack.getItem() instanceof ICastItem item
                && Arrays.asList(item.getSpells(stack)).contains(Spells.RESURRECTION);
    }

    private static PlayerCastContext selfContext(ServerPlayer player) {
        return new PlayerCastContext(player.serverLevel(), player, InteractionHand.MAIN_HAND, 0, new SpellModifiers());
    }

    public static int waitTicks() { return ((Resurrection) Spells.RESURRECTION).property(WAIT_TIME); }

    public static void markDeath(ServerPlayer player, boolean amulet, int reservedSlot) {
        DEATHS.put(player, new Death(player.serverLevel().getGameTime(), player.serverLevel().dimension().location().toString(), amulet, reservedSlot));
    }

    private static boolean ready(ServerPlayer player, int wait) {
        Death death = DEATHS.get(player);
        return death != null && player.isDeadOrDying() && !player.isSpectator()
                && death.dimension.equals(player.serverLevel().dimension().location().toString())
                && player.server.getPlayerList().getPlayer(player.getUUID()) == player
                && !REVIVING.containsKey(player) && player.serverLevel().getGameTime() - death.tick >= wait;
    }

    public static void requestSelf(ServerPlayer player) {
        if (!ready(player, waitTicks())) return;
        Death death = DEATHS.get(player);
        if (!death.amulet) return;
        if (death.reservedSlot < 0 && !player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        int slot = death.reservedSlot >= 0 ? death.reservedSlot : findCastingSlot(player);
        if (slot < 0) return;
        ItemStack stack = player.getInventory().getItem(slot);
        if (!supports(stack)) return;
        Vec3 landing = landing(player);
        if (landing == null) return;
        if (stack.getItem() instanceof ScrollItem && player.getCooldowns().isOnCooldown(stack.getItem())) return;
        if (com.binaris.wizardry.core.event.WizardryEventBus.fireEvent(new com.binaris.wizardry.api.content.event.EBPlayerResurrectionEvent(player, player))) return;
        var context = selfContext(player);
        var item = (ICastItem) stack.getItem();
        Spell selected = item.getCurrentSpell(stack);
        if (item instanceof WandItem) CastItemDataHelper.setCurrentSpell(stack, Spells.RESURRECTION);
        REVIVING.put(player, landing);
        try {
            if (!item.canCast(stack, Spells.RESURRECTION, context)) return;
            if (item instanceof ScrollItem scroll) {
                var cooldown = scroll.castAndGetCooldown(stack, Spells.RESURRECTION, context);
                if (cooldown.isEmpty()) return;
                var revived = revive(player, player, landing);
                if (revived != null && cooldown.getAsInt() > 0) revived.getCooldowns().addCooldown(scroll, cooldown.getAsInt());
                return;
            }
            if (!item.cast(stack, Spells.RESURRECTION, context)) return;
            if (item instanceof WandItem wand) {
                wand.consumeManaAndSetCooldown(stack, Spells.RESURRECTION, context);
                CastItemDataHelper.setCurrentSpell(stack, selected);
            }
            revive(player, player, landing);
        } finally {
            if (item instanceof WandItem && item.getCurrentSpell(stack) != selected) CastItemDataHelper.setCurrentSpell(stack, selected);
            REVIVING.remove(player);
        }
    }

    public static boolean amuletActive(ServerPlayer player) {
        return ArtifactChannel.isEquipped(player, EBItems.AMULET_RESURRECTION.get());
    }

    public static void saveDeath(ServerPlayer player, CompoundTag tag) {
        Death death = DEATHS.get(player);
        if (death == null || !player.isDeadOrDying()) return;
        CompoundTag data = new CompoundTag();
        data.putLong("tick", death.tick);
        data.putString("dimension", death.dimension);
        data.putBoolean("amulet", death.amulet);
        data.putInt("slot", death.reservedSlot);
        tag.put(KEY, data);
    }

    public static void loadDeath(ServerPlayer player, CompoundTag tag) {
        if (!tag.contains(KEY, Tag.TAG_COMPOUND)) return;
        CompoundTag data = tag.getCompound(KEY);
        int slot = data.getInt("slot");
        if (slot >= -1 && slot < 9) DEATHS.put(player, new Death(data.getLong("tick"), data.getString("dimension"), data.getBoolean("amulet"), slot));
    }

    public static void ordinaryRespawn(ServerPlayer player) {
        if (REVIVING.containsKey(player)) return;
        Death death = DEATHS.remove(player);
        if (death == null || death.reservedSlot < 0 || !player.isDeadOrDying()
                || player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        ItemStack stack = player.getInventory().getItem(death.reservedSlot);
        if (!supports(stack)) return;
        var drop = new net.minecraft.world.entity.item.ItemEntity(player.serverLevel(), player.getX(), player.getY(), player.getZ(), stack);
        if (player.serverLevel().addFreshEntity(drop)) player.getInventory().removeItemNoUpdate(death.reservedSlot);
        else UNRELEASED.put(player, death.reservedSlot);
    }

    public static void finishOrdinaryRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        Integer slot = UNRELEASED.remove(oldPlayer);
        if (slot != null) transferReserved(oldPlayer, newPlayer, slot);
    }

    private static void transferReserved(ServerPlayer oldPlayer, ServerPlayer newPlayer, int slot) {
        ItemStack stack = oldPlayer.getInventory().getItem(slot);
        if (stack.isEmpty() || ItemStack.matches(stack, newPlayer.getInventory().getItem(slot))) return;
        if (newPlayer.getInventory().getItem(slot).isEmpty()) {
            newPlayer.getInventory().setItem(slot, oldPlayer.getInventory().removeItemNoUpdate(slot));
        } else {
            ItemStack remainder = oldPlayer.getInventory().removeItemNoUpdate(slot);
            if (!newPlayer.getInventory().add(remainder)) newPlayer.spawnAtLocation(remainder);
        }
        newPlayer.inventoryMenu.broadcastChanges();
    }

    public static DimensionTransition transition(ServerPlayer player, DimensionTransition.PostDimensionTransition post) {
        Vec3 point = REVIVING.get(player);
        return point == null ? null : new DimensionTransition(player.serverLevel(), point, Vec3.ZERO,
                player.getYRot(), player.getXRot(), post);
    }

    private static Vec3 landing(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        for (int dy = 0; dy <= 2; dy++) {
            for (BlockPos point : BlockPos.withinManhattan(origin.above(dy), 1, 0, 1)) {
                if (!level.hasChunkAt(point) || !level.getWorldBorder().isWithinBounds(point)
                        || !level.getFluidState(point).isEmpty()
                        || !level.getBlockState(point.below()).isFaceSturdy(level, point.below(), net.minecraft.core.Direction.UP)) continue;
                Vec3 position = Vec3.atBottomCenterOf(point);
                if (level.noCollision(player, player.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(position))) return position;
            }
        }
        return null;
    }

    private static ServerPlayer revive(ServerPlayer target, ServerPlayer caster, Vec3 landing) {
        boolean self = target == caster;
        Death death = DEATHS.remove(target);
        if (death == null) return null;
        REVIVING.put(target, landing);
        try {
            var revived = target.server.getPlayerList().respawn(target, false, Entity.RemovalReason.KILLED);
            if (death.reservedSlot >= 0) transferReserved(target, revived, death.reservedSlot);
            revived.setHealth(revived.getMaxHealth() / 2);
            revived.experienceProgress = 0;
            revived.experienceLevel = 0;
            revived.totalExperience = 0;
            revived.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(0, 0, 0));
            var level = revived.serverLevel();
            level.playSound(null, revived, EBSounds.SPELL_RESURRECTION.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
            for (int i = 0; i < 20; i++) {
                ParticleBuilder.create(EBParticles.SPARKLE).pos(revived.getX(), revived.getY() + 0.6, revived.getZ())
                        .color(0xffff65).velocity(level.random.nextGaussian() * .08, .08, level.random.nextGaussian() * .08)
                        .allowServer(true).spawn(level);
            }
            target.server.getPlayerList().broadcastSystemMessage(Component.translatable(
                    "spell.ebwizardry.resurrection." + (self ? "resurrect_self" : "resurrect_ally"),
                    revived.getDisplayName(), caster.getDisplayName()), false);
            return revived;
        } finally {
            REVIVING.remove(target);
        }
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder().assignBaseProperties(SpellTiers.MASTER, Elements.HEALING,
                SpellTypes.ALTERATION, SpellAction.SUMMON, 150, 25, 400)
                .add(DefaultProperties.EFFECT_RADIUS, 8).add(WAIT_TIME).build();
    }

    private record Death(long tick, String dimension, boolean amulet, int reservedSlot) {}
}
