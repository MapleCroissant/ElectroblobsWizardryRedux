package com.binaris.wizardry.client.renderer.entity;

import com.binaris.wizardry.content.entity.living.DecoyEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

public final class DecoyRenderer extends EntityRenderer<DecoyEntity> {
    private final Map<DecoyEntity, Appearance> appearances = new WeakHashMap<>();
    private final PlayerRenderer wide;
    private final PlayerRenderer slim;

    public DecoyRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.5f;
        wide = new NamelessPlayerRenderer(context, false);
        slim = new NamelessPlayerRenderer(context, true);
    }

    @Override
    public void render(DecoyEntity decoy, float yaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light) {
        var owner = decoy.getOwner();
        var cached = appearances.get(decoy);
        if (cached == null || (owner == null ? -1 : owner.getId()) != cached.ownerId) {
            var level = (ClientLevel) decoy.level();
            LivingEntity copy;
            if (owner instanceof AbstractClientPlayer player) copy = new RemotePlayer(level, player.getGameProfile());
            else if (owner != null && owner.getType().create(level) instanceof LivingEntity clone) copy = clone;
            else copy = new RemotePlayer(level, new GameProfile(decoy.getUUID(), "Decoy"));
            cached = new Appearance(owner == null ? -1 : owner.getId(), copy);
            appearances.put(decoy, cached);
        }

        var standIn = cached.figure;
        standIn.setPos(decoy.position());
        standIn.setYRot(decoy.getYRot());
        standIn.yRotO = decoy.yRotO;
        standIn.setXRot(decoy.getXRot());
        standIn.xRotO = decoy.xRotO;
        standIn.setYBodyRot(decoy.yBodyRot);
        standIn.yBodyRotO = decoy.yBodyRotO;
        standIn.setYHeadRot(decoy.getYHeadRot());
        standIn.yHeadRotO = decoy.yHeadRotO;
        standIn.tickCount = decoy.tickCount;
        var mirage = com.binaris.wizardry.setup.registries.EBMobEffects.MIRAGE.get();
        var displayedEffect = decoy.getEffect(mirage);
        if (displayedEffect == null) standIn.removeEffect(mirage);
        else if (!standIn.hasEffect(mirage)) standIn.addEffect(new net.minecraft.world.effect.MobEffectInstance(displayedEffect));
        standIn.walkAnimation.update(decoy.walkAnimation.speed(), 1);
        if (owner != null) {
            for (var slot : EquipmentSlot.values()) {
                var item = owner.getItemBySlot(slot);
                if (!net.minecraft.world.item.ItemStack.matches(item, standIn.getItemBySlot(slot))) {
                    standIn.setItemSlot(slot, item.copy());
                }
            }
        }
        if (standIn instanceof AbstractClientPlayer player) {
            (player.getSkin().model() == PlayerSkin.Model.SLIM ? slim : wide).render(player, yaw, partialTick, poses, buffers, light);
        } else {
            entityRenderDispatcher.getRenderer(standIn).render(standIn, yaw, partialTick, poses, buffers, light);
        }
        super.render(decoy, yaw, partialTick, poses, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DecoyEntity decoy) { return DefaultPlayerSkin.getDefaultTexture(); }

    private record Appearance(int ownerId, LivingEntity figure) {}

    private static final class NamelessPlayerRenderer extends PlayerRenderer {
        private NamelessPlayerRenderer(EntityRendererProvider.Context context, boolean slim) { super(context, slim); }

        @Override
        protected boolean shouldShowName(AbstractClientPlayer player) { return false; }
    }
}
