package com.binaris.wizardry.client.effect;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.item.ICastItem;
import com.binaris.wizardry.content.spell.sorcery.Transportation;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

public final class TransportationOverlay {
    private static final net.minecraft.resources.ResourceLocation MARKER =
            WizardryMainMod.location("textures/gui/transportation_marker.png");

    private TransportationOverlay() {}

    public static void render(GuiGraphics graphics) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || !minecraft.options.getCameraType().isFirstPerson()
                || !ArtifactChannel.isEquipped(player, EBItems.CHARM_TRANSPORTATION.get())) return;
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ICastItem)) stack = player.getOffhandItem();
        if (!(stack.getItem() instanceof ICastItem item) || item.getCurrentSpell(stack) != Spells.TRANSPORTATION) return;
        var destinations = Services.OBJECT_DATA.getSpellManagerData(player).getVariable(Transportation.DESTINATIONS);
        if (destinations == null) return;

        Vec3 forward = player.getLookAngle();
        Vec3 right = new Vec3(forward.z, 0, -forward.x).normalize().scale(-1);
        Vec3 up = right.cross(forward);
        double focal = graphics.guiHeight() / (2 * Math.tan(Math.toRadians(minecraft.options.fov().get()) / 2));
        for (var destination : destinations) {
            if (!destination.dimension().equals(player.level().dimension())) continue;
            Vec3 offset = Vec3.atCenterOf(destination.pos()).subtract(player.getEyePosition());
            double depth = offset.dot(forward);
            if (depth <= 0) continue;
            double angle = Transportation.iconAngle(player, destination.pos());
            double aim = Transportation.deviation(player, destination.pos()) / angle;
            double proximity = Math.pow(Math.max(0, 1 - aim * aim), 3);
            int radius = (int) Math.max(1, focal * angle * (1 + 0.3 * proximity));
            int x = (int) (graphics.guiWidth() / 2.0 + focal * offset.dot(right) / depth);
            int y = (int) (graphics.guiHeight() / 2.0 - focal * offset.dot(up) / depth);
            if (x + radius < 0 || y + radius < 0 || x - radius > graphics.guiWidth() || y - radius > graphics.guiHeight()) continue;
            float intensity = aim < 1 ? 1 : 0.5f;
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            graphics.setColor(intensity, 1, intensity, intensity);
            graphics.blit(MARKER, x - radius, y - radius, 0, 0, radius * 2, radius * 2, radius * 2, radius * 2);
            graphics.setColor(1, 1, 1, 1);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }
}
