package com.binaris.wizardry.gametest;

import com.binaris.wizardry.core.gametest.EBTestCentral;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@SuppressWarnings("unused")
@net.neoforged.neoforge.gametest.GameTestHolder("ebwizardry")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public class WizardArmorTest {
    @GameTest(template = "empty_3x3x3")
    public static void armorNeverBreaks(GameTestHelper helper) {
        EBTestCentral.WizardArmor.armorNeverBreaks(helper);
    }

    @GameTest(template = "empty_3x3x3")
    public static void armorAttributesWithMana(GameTestHelper helper) {
        EBTestCentral.WizardArmor.armorAttributesWithMana(helper);
    }

    @GameTest(template = "empty_3x3x3")
    public static void armorNoAttributesWithoutMana(GameTestHelper helper) {
        EBTestCentral.WizardArmor.armorNoAttributesWithoutMana(helper);
    }
}
