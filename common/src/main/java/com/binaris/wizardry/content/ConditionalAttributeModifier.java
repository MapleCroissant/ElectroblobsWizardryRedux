package com.binaris.wizardry.content;

import com.binaris.wizardry.api.content.spell.SpellCondition;
import org.jetbrains.annotations.Nullable;

/** Implemented on vanilla modifiers by the persistence mixin. */
public interface ConditionalAttributeModifier {
    @Nullable SpellCondition ebwizardry$getCondition();
    void ebwizardry$setCondition(@Nullable SpellCondition condition);
}
