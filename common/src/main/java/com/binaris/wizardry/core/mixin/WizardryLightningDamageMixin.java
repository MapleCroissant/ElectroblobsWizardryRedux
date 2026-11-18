package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.api.content.util.MagicDamageSource;
import com.binaris.wizardry.content.entity.living.LightningWraith;
import com.binaris.wizardry.content.entity.living.StormElemental;
import com.binaris.wizardry.content.spell.lightning.LightningBoltSpell;
import com.binaris.wizardry.setup.registries.EBDamageSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class WizardryLightningDamageMixin {
    @Redirect(method = "thunderHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean ebwizardry$applyStrikeDamage(Entity victim, DamageSource vanilla, float amount, ServerLevel level, LightningBolt bolt) {
        var data = (LightningBoltSpell.StrikeData) (Object) bolt;
        if (!data.ebwizardry$isSpellStrike()) return victim.hurt(vanilla, amount);
        if (victim instanceof LightningWraith || victim instanceof StormElemental) return false;
        var owner = data.ebwizardry$strikeOwner() == null ? null : level.getEntity(data.ebwizardry$strikeOwner());
        var source = owner == null ? vanilla : MagicDamageSource.causeIndirectMagicDamage(bolt, owner, EBDamageSources.SHOCK);
        return EntityUtil.attackEntityWithoutKnockback(victim, source, data.ebwizardry$strikeDamage());
    }
}
