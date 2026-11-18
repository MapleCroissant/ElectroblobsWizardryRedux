package com.binaris.wizardry.content.spell.sorcery;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.content.entity.construct.LevitatingBlock;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.RaySpell;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class GreaterTelekinesis extends RaySpell {
    private static final SpellProperty<Float> HOLD_RANGE = SpellProperty.floatProperty("hold_range", 5);
    private static final SpellProperty<Float> THROW_VELOCITY = SpellProperty.floatProperty("throw_velocity", 0.15f);

    public GreaterTelekinesis() {
        aimAssist(0.4f);
        soundValues(0.8f, 1, 0.2f);
    }

    @Override
    public boolean isInstantCast() { return false; }

    @Override
    public boolean canCastByEntity() { return false; }

    @Override
    public boolean canCastByLocation() { return false; }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) {
        if (!(context.caster() instanceof Player caster)) return false;
        Entity target = hit.getEntity();
        if (!(target instanceof LivingEntity || target instanceof LevitatingBlock || target instanceof PrimedTnt)) return false;
        if (target instanceof Player player && (!EBServerConfig.PLAYERS_MOVE_EACH_OTHER.get()
                || ArtifactChannel.isEquipped(player, EBItems.AMULET_ANCHORING.get()))) return false;

        if (context.world().isClientSide) {
            particles(context, caster, target, origin);
        } else {
            if (target instanceof LevitatingBlock block) block.suspend(caster);
            Vec3 direction = caster.getLookAngle();
            if (caster.isShiftKeyDown()) {
                target.push(direction.scale(context.modifiers().get(SpellModifiers.RANGE, property(THROW_VELOCITY))));
                caster.swing(caster.getUsedItemHand());
            } else {
                Vec3 hold = origin.add(direction.scale(property(HOLD_RANGE)));
                Vec3 velocity = target.getDeltaMovement();
                Vec3 correction = hold.subtract(target.getBoundingBox().getCenter()).subtract(velocity).scale(0.8);
                target.setDeltaMovement(velocity.add(correction).add(0, target.isNoGravity() ? 0 : target.getGravity(), 0));
                target.hasImpulse = true;
            }
            if (target instanceof ServerPlayer player) player.connection.send(new ClientboundSetEntityMotionPacket(target));
        }
        return true;
    }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) {
        if (!(context.caster() instanceof Player caster) || !EBServerConfig.PLAYER_BLOCK_DAMAGE.get() || !caster.mayBuild()) return false;
        var level = context.world();
        var pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        boolean barrel = state.is(Blocks.BARREL);
        boolean portable = barrel || state.getBlock() instanceof ShulkerBoxBlock;
        if ((!state.isSolid() && !portable) || state.getDestroySpeed(level, pos) < 0 || (state.hasBlockEntity() && !portable)
                || state.getBlock() instanceof DoorBlock || state.getBlock() instanceof BedBlock) return false;
        if (level.isClientSide) return true;
        if (!BlockUtil.canBreak(caster, level, pos, true) || level.getBlockState(pos) != state) return false;

        BaseContainerBlockEntity container = null;
        var block = new LevitatingBlock(level, pos, state, caster,
                context.modifiers().get(SpellModifiers.POTENCY, property(DefaultProperties.DAMAGE)));
        if (portable) {
            if (!(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity current) || !closedContainer(current, state)) return false;
            var lock = Services.OBJECT_DATA.getArcaneLockData(current);
            if (lock != null && lock.isArcaneLocked()) return false;
            block.blockData = current.saveWithId(level.registryAccess());
            if (block.blockData.contains("Lock") || (barrel && block.blockData.contains("LootTable"))) return false;
            container = current;
        }
        if (!level.addFreshEntity(block)) return false;

        if (level.getBlockState(pos) != state || (container != null
                && (level.getBlockEntity(pos) != container || !closedContainer(container, state)
                || !container.saveWithId(level.registryAccess()).equals(block.blockData)))) {
            block.discard();
            return false;
        }

        if (barrel) {
            // A barrel's removal drops its inventory if the block entity is still attached.
            level.getChunkAt(pos).removeBlockEntity(pos);
        }
        if (level.removeBlock(pos, false)) {
            if (barrel) level.updateNeighbourForOutputSignal(pos, state.getBlock());
            return true;
        }
        if (barrel) {
            if (level.getBlockState(pos) != state) return true;
            container.clearRemoved();
            level.setBlockEntity(container);
        }
        block.discard();
        return false;
    }

    private boolean closedContainer(BaseContainerBlockEntity container, BlockState state) {
        if (state.is(Blocks.BARREL)) {
            return container instanceof BarrelBlockEntity && !state.getValue(BarrelBlock.OPEN);
        }
        return container instanceof ShulkerBoxBlockEntity box && box.isClosed();
    }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) { return false; }

    private void particles(CastContext context, Player caster, Entity target, Vec3 origin) {
        Level level = context.world();
        ParticleBuilder.create(EBParticles.BEAM).entity(caster).pos(origin.subtract(caster.position())).target(target)
                .color(0.2f, 0.6f + 0.3f * level.random.nextFloat(), 1).time(0)
                .scale(Mth.sin(context.castingTicks() * 0.3f) * 0.1f + 0.9f).spawn(level);
        if (context.castingTicks() % 18 == 1) {
            ParticleBuilder.create(EBParticles.FLASH).entity(target).pos(0, target.getBbHeight() / 2, 0)
                    .scale(2.5f).time(30).color(0.2f, 0.8f, 1).fade(1, 1, 1).spawn(level);
        }
        ParticleBuilder.create(EBParticles.SPARKLE, target).velocity(0, 0.05, 0).time(15).scale(0.6f)
                .color(0.2f, 0.6f, 1).fade(1, 1, 1).spawn(level);
    }

    @Override
    protected void playSound(Level level, LivingEntity entity, int tick, int duration) {
        playSoundLoop(level, entity, tick);
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.SORCERY, SpellTypes.UTILITY, SpellAction.POINT, 10, 0, 0)
                .add(DefaultProperties.RANGE, 8f).add(HOLD_RANGE).add(THROW_VELOCITY).add(DefaultProperties.DAMAGE, 4f)
                .build();
    }
}
