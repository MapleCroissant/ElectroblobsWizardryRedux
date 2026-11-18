package com.binaris.wizardry.content.spell.earth;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.api.content.util.EnchantmentUtil;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.RaySpell;
import com.binaris.wizardry.core.config.EBServerConfig;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Mine extends RaySpell {
    public Mine() {
        ignoreLivingEntities(true);
        particleSpacing(0.5);
    }

    @Override
    public boolean canCastByLocation() { return false; }

    @Override
    protected boolean shootSpell(CastContext context, Vec3 origin, Vec3 direction) {
        double range = context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE));
        var hit = context.world().clip(new ClipContext(origin, origin.add(direction.scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context.caster()));
        if (hit.getType() != HitResult.Type.BLOCK || !onBlockHit(context, hit, origin)) return false;
        if (context.world().isClientSide) spawnParticleRay(context, origin, direction, origin.distanceTo(hit.getLocation()));
        return true;
    }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) { return false; }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) { return false; }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) {
        var caster = context.caster();
        if (caster == null) return false;
        if (context.world().isClientSide) return true;
        if (!(context.world() instanceof ServerLevel level) || !EntityUtil.canDamageBlocks(caster, level)
                || (caster instanceof Player && !EBServerConfig.PLAYER_BLOCK_DAMAGE.get())) return false;

        float tierStep = EBServerConfig.POTENCY_INCREASE_PER_TIER.get();
        int tier = tierStep > 0 ? Math.clamp(Math.round((context.modifiers().getFactor(SpellModifiers.POTENCY) - 1) / tierStep) - 1, 0, 3) : 0;
        ItemStack tool = new ItemStack(switch (tier) {
            case 1 -> Items.STONE_PICKAXE;
            case 2 -> Items.IRON_PICKAXE;
            case 3 -> Items.DIAMOND_PICKAXE;
            default -> Items.WOODEN_PICKAXE;
        });
        var center = hit.getBlockPos();
        var centerState = level.getBlockState(center);
        if (centerState.getDestroySpeed(level, center) < 0
                || (centerState.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(centerState))) return false;
        if (caster instanceof Player player && ArtifactChannel.isEquipped(player, EBItems.CHARM_SILK_TOUCH.get())) {
            tool.enchant(EnchantmentUtil.get(level, Enchantments.SILK_TOUCH), 1);
        }

        float blastStep = EBServerConfig.BLAST_RADIUS_INCREASE_PER_LEVEL.get();
        int upgrades = blastStep > 0 ? Math.clamp(Math.round((context.modifiers().getFactor(SpellModifiers.BLAST) - 1) / blastStep), 0, 8) : 0;
        double radius = 0.5 + 0.73 * upgrades;
        int reach = (int) radius;
        boolean broken = false;
        for (var pos : BlockPos.betweenClosed(center.offset(-reach, -reach, -reach), center.offset(reach, reach, reach))) {
            if (pos.distSqr(center) > radius * radius || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) continue;
            var state = level.getBlockState(pos);
            if (state.isAir() || state.liquid() || state.getDestroySpeed(level, pos) < 0
                    || (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state))) continue;
            boolean allowed = caster instanceof Player player ? BlockUtil.canBreak(player, level, pos, true)
                    : caster instanceof Mob mob && BlockUtil.canBreak(mob, level, pos);
            if (!allowed || level.getBlockState(pos) != state) continue;
            var blockEntity = level.getBlockEntity(pos);
            if (level.destroyBlock(pos, false, caster)) {
                Block.dropResources(state, level, pos, blockEntity, caster, tool);
                broken = true;
            }
        }
        return broken;
    }

    @Override
    protected void spawnParticle(CastContext context, double x, double y, double z, double vx, double vy, double vz) {
        ParticleBuilder.create(EBParticles.DUST).pos(x, y, z).time(20 + context.world().random.nextInt(5))
                .color(0.9f, 0.95f, 1).shaded(false).spawn(context.world());
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.NOVICE, Elements.EARTH, SpellTypes.UTILITY, SpellAction.POINT, 5, 0, 5)
                .add(DefaultProperties.RANGE, 8f).build();
    }
}
