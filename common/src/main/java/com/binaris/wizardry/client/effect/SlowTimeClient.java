package com.binaris.wizardry.client.effect;

import com.binaris.wizardry.content.effect.SlowTimeMobEffect;
import net.minecraft.client.Minecraft;

public final class SlowTimeClient {
    private SlowTimeClient() {}

    public static void setBlocked(boolean blocked) {
        var level = Minecraft.getInstance().level;
        if (level != null) SlowTimeMobEffect.setLocalBlocked(level, blocked);
    }
}
