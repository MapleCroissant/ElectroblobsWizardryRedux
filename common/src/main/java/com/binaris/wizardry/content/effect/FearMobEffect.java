package com.binaris.wizardry.content.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;

import java.util.UUID;

public final class FearMobEffect extends MobEffect {
    public interface SourceAccess {
        UUID ebwizardry$getFearSource();
        void ebwizardry$setFearSource(UUID source);
    }

    public FearMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xbd0100);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof PathfinderMob mob) || !(entity.level() instanceof ServerLevel level)
                || entity.tickCount % 50 != 0) return true;
        var id = ((SourceAccess) mob).ebwizardry$getFearSource();
        if (id == null || !(level.getEntity(id) instanceof LivingEntity source) || !source.isAlive()) return true;

        int distance = Math.clamp(16 + 4 * amplifier, 16, 64);
        if (mob.distanceToSqr(source) >= distance * distance) return true;
        mob.setTarget(null);
        var navigation = mob.getNavigation();
        var end = navigation.getTargetPos();
        if (!navigation.isDone() && end != null && end.distToCenterSqr(source.position()) >= distance * distance) return true;
        var escape = DefaultRandomPos.getPosAway(mob, distance, distance / 2, source.position());
        if (escape != null) navigation.moveTo(escape.x, escape.y, escape.z, 1.25);
        return true;
    }
}
