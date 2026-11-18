package com.binaris.wizardry.content.item.artifact;

import com.binaris.wizardry.content.spell.fire.Disintegration;
import com.binaris.wizardry.core.ArtifactEffectContext;
import com.binaris.wizardry.core.IArtifactEffect;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class DisintegrationRingEffect implements IArtifactEffect {
    @Override
    public void onKillEntity(LivingEntity user, LivingEntity victim, DamageSource source, ArtifactEffectContext context) {
        if (source.is(EBDamageSources.FIRE)) Disintegration.spawnEmbers(victim, user);
    }
}
