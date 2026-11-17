package com.binaris.wizardry.core.mixin;

import com.binaris.wizardry.api.content.spell.*;
import com.binaris.wizardry.content.ConditionalAttributeModifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AttributeModifier.class)
public abstract class AttributeModifierMixin implements ConditionalAttributeModifier {
    @Unique private SpellCondition ebwizardry$condition;

    @Override
    public SpellCondition ebwizardry$getCondition() {
        return ebwizardry$condition;
    }

    @Override
    public void ebwizardry$setCondition(SpellCondition condition) {
        ebwizardry$condition = condition;
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void ebwizardry$saveCondition(CallbackInfoReturnable<CompoundTag> cir) {
        if (ebwizardry$condition != null) ebwizardry$condition.save(cir.getReturnValue());
    }

    @Inject(method = "load", at = @At("RETURN"))
    private static void EBWIZARDRY$attribute_load(CompoundTag nbt, CallbackInfoReturnable<AttributeModifier> cir) {
        AttributeModifier modifier = cir.getReturnValue();
        if (modifier != null) {
            ((ConditionalAttributeModifier) (Object) modifier).ebwizardry$setCondition(SpellCondition.load(nbt));
        }
    }
}
