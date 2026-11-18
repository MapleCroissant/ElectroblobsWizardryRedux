package com.binaris.wizardry.client.effect;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.core.config.EBClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.lang.ref.WeakReference;

public final class BlinkOverlay {
    public static final ResourceLocation TEXTURE = WizardryMainMod.location("textures/gui/blink_overlay.png");
    private static WeakReference<Player> player = new WeakReference<>(null);
    private static int startTick;

    private BlinkOverlay() {}

    public static void start(Player caster) {
        if (caster == Minecraft.getInstance().player && EBClientConfig.BLINK_OVERLAY.get()) {
            player = new WeakReference<>(caster);
            startTick = caster.tickCount;
        }
    }

    public static float strength(float partialTick) {
        var client = Minecraft.getInstance();
        var caster = player.get();
        if (caster == null || caster != client.player || !client.options.getCameraType().isFirstPerson()
                || !EBClientConfig.BLINK_OVERLAY.get()) return 0;
        return Math.clamp((8 - (caster.tickCount - startTick) - partialTick) / 8f, 0, 1);
    }
}
