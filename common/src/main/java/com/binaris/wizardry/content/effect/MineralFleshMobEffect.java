package com.binaris.wizardry.content.effect;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.core.config.EBServerConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;
import java.util.function.DoubleSupplier;

public final class MineralFleshMobEffect extends MobEffect {
    private final String name;
    private final List<Bonus> bonuses;

    public MineralFleshMobEffect(boolean diamond) {
        super(MobEffectCategory.BENEFICIAL, 0);
        name = diamond ? "diamondflesh" : "ironflesh";
        bonuses = List.of(
                new Bonus(Attributes.MOVEMENT_SPEED, "speed",
                        () -> EBServerConfig.MINERAL_FLESH_SLOWNESS.get() ? -0.1 : 0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                new Bonus(Attributes.ARMOR, "armor", () -> diamond ? EBServerConfig.DIAMOND_FLESH_ARMOR.get() : EBServerConfig.IRON_FLESH_ARMOR.get(), AttributeModifier.Operation.ADD_VALUE),
                diamond
                        ? new Bonus(Attributes.ARMOR_TOUGHNESS, "toughness",
                                () -> EBServerConfig.DIAMOND_FLESH_TOUGHNESS.get(), AttributeModifier.Operation.ADD_VALUE)
                        : new Bonus(Attributes.KNOCKBACK_RESISTANCE, "knockback_resistance",
                                () -> 0.3, AttributeModifier.Operation.ADD_VALUE));
    }

    @Override
    public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
        for (var bonus : bonuses) {
            var instance = attributes.getInstance(bonus.attribute);
            if (instance == null) continue;
            var id = WizardryMainMod.location(name + "/" + bonus.key);
            instance.removeModifier(id);
            double amount = bonus.amount.getAsDouble() * (amplifier + 1);
            if (amount != 0) instance.addPermanentModifier(new AttributeModifier(id, amount, bonus.operation));
        }
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributes) {
        for (var bonus : bonuses) {
            var instance = attributes.getInstance(bonus.attribute);
            if (instance != null) instance.removeModifier(WizardryMainMod.location(name + "/" + bonus.key));
        }
    }

    private record Bonus(Holder<Attribute> attribute, String key, DoubleSupplier amount, AttributeModifier.Operation operation) {}
}
