package com.binaris.wizardry.content.spell.earth;

import com.binaris.wizardry.api.content.data.StoredSpellVar;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.util.BlockUtil;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.core.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.function.Supplier;

abstract class SpiritCompanionSpell<T extends Mob & OwnableEntity> extends Spell {
    private final StoredSpellVar<UUID> companionKey;
    private final Supplier<EntityType<T>> type;

    SpiritCompanionSpell(StoredSpellVar<UUID> companionKey, Supplier<EntityType<T>> type) {
        this.companionKey = companionKey;
        this.type = type;
        soundValues(0.7f, 1.2f, 0.4f);
        Services.OBJECT_DATA.spellStoredVariables(companionKey);
    }

    @Override
    public boolean requiresPacket() { return false; }

    @Override
    public final boolean cast(PlayerCastContext context) {
        var caster = context.caster();
        if (context.world() instanceof ServerLevel level) {
            int radius = Math.clamp(property(DefaultProperties.SUMMON_RADIUS), 1, 8);
            BlockPos pos = BlockUtil.findNearbyFloorSpace(caster, radius, 4);
            if (pos == null || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return false;
            var companion = type.get().create(level);
            if (companion == null) return false;
            companion.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (level.getBlockCollisions(companion, companion.getBoundingBox()).iterator().hasNext()) return false;
            configure(companion, caster, context.modifiers().getFactor(SpellModifiers.POTENCY));
            companion.setPersistenceRequired();
            companion.setHealth(companion.getMaxHealth());
            if (!level.addFreshEntity(companion)) return false;

            var data = Services.OBJECT_DATA.getSpellManagerData(caster);
            UUID previous = data.getVariable(companionKey);
            data.setVariable(companionKey, companion.getUUID());
            if (previous != null) {
                for (ServerLevel dimension : level.getServer().getAllLevels()) {
                    var entity = dimension.getEntity(previous);
                    if (entity instanceof OwnableEntity old && entity.getType() == type.get()
                            && caster.getUUID().equals(old.getOwnerUUID())) {
                        entity.discard();
                        break;
                    }
                }
            }
        }
        playSound(context.world(), caster, context.castingTicks(), -1);
        return true;
    }

    protected abstract void configure(T companion, Player caster, float potency);
}
