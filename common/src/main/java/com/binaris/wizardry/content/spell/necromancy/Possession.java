package com.binaris.wizardry.content.spell.necromancy;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.*;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.content.entity.living.*;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.RaySpell;
import com.binaris.wizardry.core.AllyDesignation;
import com.binaris.wizardry.core.platform.Services;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import com.binaris.wizardry.setup.registries.*;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class Possession extends RaySpell {
    private static final SpellProperty<Integer> CRITICAL_HEALTH = SpellProperty.intProperty("critical_health", 1);
    private static final ResourceLocation SPEED = ResourceLocation.fromNamespaceAndPath("ebwizardry", "possession_speed");
    private static final Map<Player, Session> ACTIVE = new WeakHashMap<>();

    public interface HostData {
        int ebwizardry$hostId();
        void ebwizardry$setHostId(int id);
    }

    @Override
    public boolean requiresPacket() { return false; }

    @Override
    public boolean canCastByEntity() { return false; }

    @Override
    public boolean canCastByLocation() { return false; }

    @Override
    public boolean cast(PlayerCastContext context) {
        Player player = context.caster();
        return !context.world().isClientSide && !active(player)
                && shootSpell(context, player.getEyePosition().add(0, -Y_OFFSET, 0), player.getLookAngle());
    }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) {
        if (!(context.caster() instanceof Player player) || context.world().isClientSide
                || !(hit.getEntity() instanceof Mob host)) return false;
        int ticks = (int) context.modifiers().get(SpellModifiers.DURATION, property(DefaultProperties.EFFECT_DURATION));
        return start(player, host, ticks);
    }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) { return false; }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) { return false; }

    public boolean start(Player player, Mob host, int ticks) {
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || player.isSpectator()
                || player.isShiftKeyDown() || !host.isAlive() || host.isRemoved() || host.level() != level
                || host instanceof DecoyEntity || host.getType() == EntityType.ENDER_DRAGON
                || host.getType() == EntityType.WITHER || host.isVehicle() || host.getVehicle() != null
                || ACTIVE.containsKey(player) || controller(host) != null || ticks <= 0
                || !level.getWorldBorder().isWithinBounds(host.blockPosition())
                || level.getBlockCollisions(player, player.getBoundingBox().move(host.position().subtract(player.position())))
                        .iterator().hasNext()) return false;
        int critical = property(CRITICAL_HEALTH);
        if (!player.isCreative() && player.getHealth() <= critical) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("spell.ebwizardry.possession.insufficienthealth"), true);
            return false;
        }
        boolean canFly = host instanceof FlyingMob || host instanceof Blaze;
        ACTIVE.put(player, new Session(host, level.getGameTime() + ticks, critical,
                player.getAbilities().mayfly, player.getAbilities().flying, canFly));
        ((HostData) player).ebwizardry$setHostId(host.getId());
        player.stopRiding();
        player.stopUsingItem();
        host.getNavigation().stop();
        host.setTarget(null);
        player.setYRot(host.getYRot());
        player.setXRot(host.getXRot());
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.teleportTo(level, host.getX(), host.getY(), host.getZ(), java.util.Set.of(), host.getYRot(), host.getXRot());
            serverPlayer.closeContainer();
        } else player.teleportTo(host.getX(), host.getY(), host.getZ());
        if (canFly) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
        var movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        double hostSpeed = host.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (movement != null && hostSpeed > 0 && movement.getValue() > 0) {
            movement.addTransientModifier(new AttributeModifier(SPEED, hostSpeed / movement.getValue() - 1,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        level.playSound(null, player, EBSounds.SPELL_POSSESSION_POSSESS.get(), SoundSource.PLAYERS, 1, 1);
        return true;
    }

    public static boolean active(Player player) { return ((HostData) player).ebwizardry$hostId() >= 0; }

    public static boolean controls(Player player, Entity entity) {
        int host = ((HostData) player).ebwizardry$hostId();
        return host >= 0 && host == entity.getId();
    }

    public static float eyeHeight(Player player) {
        int id = ((HostData) player).ebwizardry$hostId();
        return id >= 0 && player.level().getEntity(id) instanceof Mob host ? host.getEyeHeight() : player.getEyeHeight();
    }

    public static Player controller(Mob host) {
        return ACTIVE.entrySet().stream().filter(e -> e.getValue().host == host).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public static void tick(Player player) {
        Session session = ACTIVE.get(player);
        if (session == null) return;
        Mob host = session.host;
        if (!player.isAlive() || player.isRemoved() || player.isSpectator() || player.isShiftKeyDown()
                || host.isRemoved() || !host.isAlive() || host.level() != player.level()
                || player.level().getGameTime() >= session.end || player.getHealth() <= session.critical && !player.isCreative()) {
            end(player);
            return;
        }
        Vec3 offset = player.position().subtract(host.position());
        if (offset.lengthSqr() > 16 || !player.level().getWorldBorder().isWithinBounds(player.blockPosition())
                || player.level().getBlockCollisions(host, host.getBoundingBox().move(offset)).iterator().hasNext()) {
            end(player);
            return;
        }
        host.setDeltaMovement(Vec3.ZERO);
        host.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        host.setYHeadRot(player.getYHeadRot());
        host.setYBodyRot(player.getYRot());
        if (host instanceof Spider && player.horizontalCollision) {
            player.setDeltaMovement(player.getDeltaMovement().with(net.minecraft.core.Direction.Axis.Y, 0.2));
        } else if (host instanceof Chicken && !player.onGround() && player.getDeltaMovement().y < 0) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0.6, 1));
        }
        if (host.fireImmune() && player.isOnFire()) player.clearFire();
        if ((host.tickCount & 3) == 0) {
            ParticleBuilder.create(EBParticles.DARK_MAGIC).pos(host.getEyePosition()).color(0x1a004d)
                    .allowServer(true).spawn(host.level());
        }
    }

    public static void saveAbilities(Player player, net.minecraft.nbt.CompoundTag tag) {
        Session session = ACTIVE.get(player);
        if (session == null || !session.flight) return;
        var abilities = tag.getCompound("abilities");
        abilities.putBoolean("mayfly", session.mayFly);
        abilities.putBoolean("flying", session.flying);
        tag.put("abilities", abilities);
    }

    public static void end(Player player) {
        Session session = ACTIVE.remove(player);
        if (session == null) return;
        ((HostData) player).ebwizardry$setHostId(-1);
        var movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) movement.removeModifier(SPEED);
        if (session.flight && !player.isSpectator()) {
            player.getAbilities().mayfly = session.mayFly;
            player.getAbilities().flying = session.flying;
            player.onUpdateAbilities();
        }
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player, EBSounds.SPELL_POSSESSION_END.get(), SoundSource.PLAYERS, 1, 1);
        }
    }

    public static boolean melee(Player player, Entity target) {
        Session session = ACTIVE.get(player);
        if (session == null) return false;
        if (session.host instanceof Creeper || target == session.host || !target.isAlive() || target.level() != player.level()
                || !player.canInteractWithEntity(target, 0) || player.getAttackStrengthScale(0.5f) < 0.9f
                || target instanceof LivingEntity living && !AllyDesignation.isValidTarget(player, living)) return true;
        player.resetAttackStrengthTicker();
        session.host.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (session.host.getAttribute(Attributes.ATTACK_DAMAGE) != null) session.host.doHurtTarget(target);
        else target.hurt(player.damageSources().mobAttack(session.host), 1);
        return true;
    }

    public static boolean divertDamage(Player player, DamageSource source, float amount) {
        Session session = ACTIVE.get(player);
        if (session == null || source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL)) return false;
        session.host.hurt(source, amount);
        return true;
    }

    public static void hostDamage(Mob host, float healthBefore) {
        Player player = controller(host);
        if (player == null) return;
        Session session = ACTIVE.get(player);
        if (!player.isCreative()) {
            player.setHealth(Math.max(session.critical, player.getHealth() - Math.max(0, healthBefore - host.getHealth()) / 2));
        }
        if (!host.isAlive() || !player.isCreative() && player.getHealth() <= session.critical) end(player);
    }

    public static void useAbility(Player player) {
        Session session = ACTIVE.get(player);
        if (session == null || !(player.level() instanceof ServerLevel level) || player.level().getGameTime() < session.nextShot
                || !session.host.isAlive() || session.host.level() != level) return;
        Mob host = session.host;
        session.nextShot = level.getGameTime() + 30;
        if (host instanceof EnderMan enderman) {
            placeCarriedBlock(player, level, enderman);
            return;
        }
        if (host instanceof Creeper creeper) {
            end(player);
            creeper.ignite();
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 position = host.getEyePosition().add(look);
        Projectile projectile;
        if (host instanceof IceWraith) projectile = EBEntities.ICE_SHARD.get().create(level);
        else if (host instanceof ShadowWraith) projectile = EBEntities.DARKNESS_ORB.get().create(level);
        else if (host instanceof StormElemental) projectile = EBEntities.LIGHTNING_DISC.get().create(level);
        else if (host instanceof LightningWraith) {
            Spells.ARC.cast(new com.binaris.wizardry.api.content.spell.internal.PlayerCastContext(level, player,
                    net.minecraft.world.InteractionHand.MAIN_HAND, 0, new SpellModifiers()));
            return;
        } else if (host instanceof Blaze) projectile = EBEntities.MAGIC_FIREBALL.get().create(level);
        else if (host instanceof Ghast) projectile = EBEntities.LARGE_MAGIC_FIREBALL.get().create(level);
        else if (host instanceof SnowGolem) projectile = new Snowball(level, host);
        else if (host instanceof Witch) {
            var potion = new ThrownPotion(level, host);
            var item = new ItemStack(Items.SPLASH_POTION);
            item.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                    new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.HARMING));
            potion.setItem(item);
            projectile = potion;
        } else if (host.getMainHandItem().is(Items.BOW)) {
            var arrow = new Arrow(EntityType.ARROW, level);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            projectile = arrow;
        } else return;
        if (projectile == null) return;
        projectile.setOwner(player);
        projectile.setPos(position);
        projectile.shoot(look.x, look.y, look.z, 1.6f, 1);
        level.addFreshEntity(projectile);
    }

    public static void takeBlock(Player player) {
        Session session = ACTIVE.get(player);
        if (session == null || !(player.level() instanceof ServerLevel level)
                || !(session.host instanceof EnderMan host) || host.getCarriedBlock() != null
                || !host.isAlive() || host.level() != level || !player.mayBuild()) return;
        if (session.nextBlockAction > level.getGameTime()) return;
        session.nextBlockAction = level.getGameTime() + 5;
        var hit = aimBlock(player, level);
        if (hit.getType() != HitResult.Type.BLOCK) return;
        var pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        if (!player.canInteractWithBlock(pos, 0) || !level.mayInteract(player, pos) || state.hasBlockEntity()
                || state.getDestroySpeed(level, pos) < 0 || !(state.getBlock().asItem() instanceof BlockItem)
                || Services.PLATFORM.firePlayerBlockBreakEvent(level, pos, player) || level.getBlockState(pos) != state) return;
        if (level.removeBlock(pos, false)) host.setCarriedBlock(state);
    }

    private static void placeCarriedBlock(Player player, ServerLevel level, EnderMan host) {
        var carried = host.getCarriedBlock();
        if (carried == null || !player.mayBuild()) return;
        var hit = aimBlock(player, level);
        if (hit.getType() != HitResult.Type.BLOCK || !player.canInteractWithBlock(hit.getBlockPos(), 0)
                || !level.mayInteract(player, hit.getBlockPos())) return;
        var stack = new ItemStack(carried.getBlock());
        var context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit);
        if (!level.mayInteract(player, context.getClickedPos())) return;
        if (stack.useOn(context).consumesAction()) host.setCarriedBlock(null);
    }

    private static BlockHitResult aimBlock(Player player, ServerLevel level) {
        Vec3 eye = player.position().add(0, eyeHeight(player), 0);
        Vec3 end = eye.add(player.getLookAngle().scale(player.blockInteractionRange()));
        return level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder().assignBaseProperties(SpellTiers.MASTER, Elements.NECROMANCY,
                SpellTypes.ALTERATION, SpellAction.POINT, 100, 15, 300)
                .add(DefaultProperties.RANGE, 8f).add(DefaultProperties.EFFECT_DURATION, 600).add(CRITICAL_HEALTH).build();
    }

    private static final class Session {
        final Mob host;
        final long end;
        final int critical;
        final boolean mayFly;
        final boolean flying;
        final boolean flight;
        long nextShot;
        long nextBlockAction;
        Session(Mob host, long end, int critical, boolean mayFly, boolean flying, boolean flight) {
            this.host = host;
            this.end = end;
            this.critical = critical;
            this.mayFly = mayFly;
            this.flying = flying;
            this.flight = flight;
        }
    }
}
