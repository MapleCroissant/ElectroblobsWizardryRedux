package com.binaris.wizardry.content.effect;

import com.binaris.wizardry.api.content.effect.CurseMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class EnfeeblementCurse extends CurseMobEffect {
    public EnfeeblementCurse() {
        super(MobEffectCategory.HARMFUL, 0x36000b);
        this.addAttributeModifier(Attributes.MAX_HEALTH, com.binaris.wizardry.WizardryMainMod.location("modifier/2e8c378e-3d51-4ba1-b02c-591b5d968a05"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity livingEntity, int tick) {
        if (!(livingEntity instanceof Player player)) return true;
        // Prob should be 0.2
        if (player.getFoodData().getFoodLevel() > 17 && player.level().random.nextDouble() < 0.2) {
            player.getFoodData().setFoodLevel(0);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int $$0, int $$1) {
        // Same that the poison effect
        int $$3 = 25 >> $$1;
        return $$3 == 0 || $$0 % $$3 == 0;
    }
}
