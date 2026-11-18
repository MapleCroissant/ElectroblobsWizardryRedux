package com.binaris.wizardry.content.spell.ice;

import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.EntityCastContext;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.Vec3;

public final class SnowballSpell extends Spell {
    private static final float LAUNCH_OFFSET = 0.1f;

    public SnowballSpell() {
        soundValues(0.5f, 0.4f, 0.2f);
    }

    @Override
    public boolean requiresPacket() { return false; }

    @Override
    public boolean canCastByEntity() { return true; }

    @Override
    public boolean cast(PlayerCastContext context) {
        var caster = context.caster();
        if (!context.world().isClientSide) {
            var snowball = new Snowball(context.world(), caster);
            snowball.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0,
                    velocity(context.modifiers(), caster.getEyeHeight() - LAUNCH_OFFSET), 1);
            if (!context.world().addFreshEntity(snowball)) return false;
        }
        playSound(context.world(), caster, context.castingTicks(), -1);
        caster.swing(context.hand());
        return true;
    }

    @Override
    public boolean cast(EntityCastContext context) {
        if (context.target() == null) return false;
        var caster = context.caster();
        if (!context.world().isClientSide) {
            var snowball = new Snowball(context.world(), caster);
            Vec3 horizontal = context.target().position().subtract(caster.position()).multiply(1, 0, 1);
            double y = caster.getEyeY() - LAUNCH_OFFSET;
            snowball.setPos(new Vec3(caster.getX(), y, caster.getZ()).add(horizontal.normalize()));
            snowball.shoot(horizontal.x,
                    context.target().getY() + context.target().getBbHeight() / 3 - y + horizontal.length() * 0.2,
                    horizontal.z, velocity(context.modifiers(), caster.getEyeHeight() - LAUNCH_OFFSET),
                    EntityUtil.getDefaultAimingError(context.world().getDifficulty()));
            if (!context.world().addFreshEntity(snowball)) return false;
        }
        playSound(context.world(), caster, context.castingTicks(), -1);
        caster.swing(context.hand());
        return true;
    }

    private float velocity(SpellModifiers modifiers, float height) {
        return (float) (modifiers.get(SpellModifiers.RANGE, property(DefaultProperties.RANGE))
                / Math.sqrt(2 * height / 0.03));
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.NOVICE, Elements.ICE, SpellTypes.PROJECTILE, SpellAction.POINT, 1, 0, 1)
                .add(DefaultProperties.RANGE, 15f).build();
    }
}
