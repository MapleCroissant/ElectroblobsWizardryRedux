package com.binaris.wizardry.client.effect;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.spell.earth.SixthSense;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public final class SixthSenseMarkers {
    public static final ResourceLocation OVERLAY = WizardryMainMod.location("textures/gui/sixth_sense_overlay.png");
    private static final RenderType PASSIVE = marker("passive");
    private static final RenderType HOSTILE = marker("hostile");
    private static final RenderType PLAYER = marker("player");

    private SixthSenseMarkers() {}

    public static void render(LivingEntity target, PoseStack poses, MultiBufferSource buffers) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || player == target || target instanceof ArmorStand || !target.isAlive()) return;
        var effect = player.getEffect(EBMobEffects.SIXTH_SENSE.get());
        if (effect == null) return;
        float radius = SixthSense.radius(effect.getAmplifier());
        float distance = player.distanceTo(target);
        if (distance >= radius) return;
        int alpha = (int) (255 * Math.clamp(5 * (1 - distance / radius), 0, 1));
        var type = PASSIVE;
        if (ArtifactChannel.isEquipped(player, EBItems.CHARM_SIXTH_SENSE.get())) {
            if (target instanceof Enemy) type = HOSTILE;
            else if (target instanceof Player) type = PLAYER;
        }

        poses.pushPose();
        poses.translate(0, target.getBbHeight() * 0.6, 0);
        poses.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        var vertex = buffers.getBuffer(type);
        var pose = poses.last();
        vertex(vertex, pose, -0.6f, -0.6f, 0, 1, alpha);
        vertex(vertex, pose, 0.6f, -0.6f, 1, 1, alpha);
        vertex(vertex, pose, 0.6f, 0.6f, 1, 0, alpha);
        vertex(vertex, pose, -0.6f, 0.6f, 0, 0, alpha);
        poses.popPose();
    }

    private static void vertex(VertexConsumer vertex, PoseStack.Pose pose, float x, float y, float u, float v, int alpha) {
        vertex.addVertex(pose, x, y, 0).setColor(255, 255, 255, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
    }

    private static RenderType marker(String type) {
        return RenderType.create("ebwizardry_sixth_sense_" + type, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                256, false, true, RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(
                                WizardryMainMod.location("textures/gui/sixth_sense_marker_" + type + ".png"), false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST).createCompositeState(false));
    }
}
