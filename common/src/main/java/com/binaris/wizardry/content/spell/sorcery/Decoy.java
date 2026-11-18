package com.binaris.wizardry.content.spell.sorcery;

import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.EntityCastContext;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.spell.properties.SpellProperty;
import com.binaris.wizardry.setup.registries.EBEntities;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class Decoy extends Spell {
    private static final SpellProperty<Integer> LIFETIME = SpellProperty.intProperty("decoy_lifetime", 600);
    private static final SpellProperty<Float> TRICK_CHANCE = SpellProperty.floatProperty("mob_trick_chance", 0.5f);

    public Decoy() { soundValues(1, 0.9f, 0.2f); }

    @Override
    public boolean requiresPacket() { return false; }

    @Override
    public boolean canCastByEntity() { return true; }

    @Override
    public boolean cast(PlayerCastContext context) {
        return split(context, context.caster().getId() % 2 == 0 ? 0.3 : -0.3);
    }

    @Override
    public boolean cast(EntityCastContext context) {
        return split(context, context.world().random.nextBoolean() ? 0.3 : -0.3);
    }

    private boolean split(CastContext context, double speed) {
        var level = context.world();
        var caster = context.caster();
        if (level.isClientSide) return true;
        var decoy = EBEntities.DECOY.get().create(level);
        if (decoy == null || property(LIFETIME) <= 0) return false;
        decoy.configure(caster, property(LIFETIME));
        decoy.moveTo(caster.getX(), caster.getY(), caster.getZ(), caster.getYRot(), caster.getXRot());
        var look = caster.getLookAngle();
        Vec3 separation = new Vec3(look.z * speed, 0, -look.x * speed);
        decoy.setDeltaMovement(separation.reverse());
        if (!level.addFreshEntity(decoy)) return false;
        caster.setDeltaMovement(caster.getDeltaMovement().add(separation));
        caster.hurtMarked = true;
        caster.hasImpulse = true;
        float probability = context.modifiers().get(SpellModifiers.POTENCY, property(TRICK_CHANCE));
        for (var mob : level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(16),
                mob -> mob.getTarget() == caster && mob.distanceToSqr(caster) <= 256)) {
            if (level.random.nextFloat() < probability) mob.setTarget(decoy);
        }
        playSound(level, caster, context.castingTicks(), -1);
        return true;
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.SORCERY, SpellTypes.UTILITY, SpellAction.SUMMON, 40, 10, 200)
                .add(LIFETIME).add(TRICK_CHANCE).build();
    }
}
