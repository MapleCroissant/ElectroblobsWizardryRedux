package com.binaris.wizardry.content.spell.lightning;

import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.RaySpell;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class LightningBoltSpell extends RaySpell {
    public interface StrikeData {
        void ebwizardry$configureStrike(@Nullable UUID owner, float damage);
        boolean ebwizardry$isSpellStrike();
        @Nullable UUID ebwizardry$strikeOwner();
        float ebwizardry$strikeDamage();
    }

    public LightningBoltSpell() {
        ignoreLivingEntities(true);
    }

    @Override
    public boolean requiresPacket() { return false; }

    @Override
    protected boolean shootSpell(CastContext context, Vec3 origin, Vec3 direction) {
        var end = origin.add(direction.scale(context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE))));
        var hit = context.world().clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context.caster()));
        return hit.getType() == HitResult.Type.BLOCK && onBlockHit(context, hit, origin);
    }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) {
        var top = hit.getBlockPos().above();
        if (!context.world().hasChunkAt(top) || !context.world().getWorldBorder().isWithinBounds(top)
                || !isOpenToSky(context.world(), top)) return false;
        if (!(context.world() instanceof ServerLevel level)) return context.world().isClientSide;
        return summon(level, Vec3.atBottomCenterOf(top), context.caster(),
                context.modifiers().get(SpellModifiers.POTENCY, property(DefaultProperties.DAMAGE)));
    }

    static boolean isOpenToSky(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        return level.dimensionType().hasSkyLight() && level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY();
    }

    public static boolean summon(ServerLevel level, Vec3 position, @Nullable LivingEntity caster, float damage) {
        var bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return false;
        bolt.setPos(position);
        if (caster instanceof ServerPlayer player) bolt.setCause(player);
        ((StrikeData) (Object) bolt).ebwizardry$configureStrike(caster == null ? null : caster.getUUID(), damage);
        return level.addFreshEntity(bolt);
    }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) { return false; }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) { return false; }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.LIGHTNING, SpellTypes.ATTACK, SpellAction.POINT, 40, 10, 80)
                .add(DefaultProperties.RANGE, 40f).add(DefaultProperties.DAMAGE, 5f).build();
    }
}
