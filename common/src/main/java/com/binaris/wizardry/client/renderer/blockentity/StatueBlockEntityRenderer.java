package com.binaris.wizardry.client.renderer.blockentity;

import com.binaris.wizardry.content.block.StatueBlock;
import com.binaris.wizardry.content.blockentity.StatueBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.Map;
import java.util.WeakHashMap;

public final class StatueBlockEntityRenderer implements BlockEntityRenderer<StatueBlockEntity> {
    private static final RenderType STONE = RenderType.entityCutoutNoCull(ResourceLocation.withDefaultNamespace("textures/block/stone.png"));
    private final Map<StatueBlockEntity, Figure> figures = new WeakHashMap<>();

    public StatueBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(StatueBlockEntity statue, float delta, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        var display = statue.display();
        if (display == null || statue.getLevel() == null) return;
        var cached = figures.get(statue);
        if (cached == null || cached.data != display) {
            if (!(EntityType.create(display.copy(), statue.getLevel()).orElse(null) instanceof Mob mob)) return;
            mob.setYBodyRot(mob.getYRot());
            mob.yBodyRotO = mob.getYRot();
            mob.setYHeadRot(mob.getYRot());
            mob.yHeadRotO = mob.getYRot();
            mob.xRotO = mob.getXRot();
            cached = new Figure(display, mob);
            figures.put(statue, cached);
        }
        var figure = cached.mob;
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(figure);
        var shape = (StatueBlock) statue.getBlockState().getBlock();
        poses.pushPose();
        poses.translate(0.5, 0, 0.5);
        renderer.render(figure, figure.getYRot(), 0, poses, shape.icy() ? buffers : type -> buffers.getBuffer(STONE), light);
        poses.popPose();
    }

    private record Figure(CompoundTag data, Mob mob) {}
}
