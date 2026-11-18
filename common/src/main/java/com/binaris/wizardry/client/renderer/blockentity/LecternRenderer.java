package com.binaris.wizardry.client.renderer.blockentity;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.blockentity.LecternBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public final class LecternRenderer implements BlockEntityRenderer<LecternBlockEntity> {
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(
            WizardryMainMod.location("textures/entity/lectern_book.png"));
    private final BookModel model;

    public LecternRenderer(BlockEntityRendererProvider.Context context) {
        model = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(LecternBlockEntity book, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float time = book.getLevel().getGameTime() + partialTick;
        float spread = Mth.lerp(partialTick, book.previousSpread, book.spread);
        float flip = Mth.lerp(partialTick, book.previousFlip, book.flip);
        pose.pushPose();
        pose.translate(0.5, 1.12 + (spread > 0.3f ? Mth.sin(time * 0.1f) * 0.01f : 0), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(90 - book.getBlockState().getValue(HorizontalDirectionalBlock.FACING).toYRot()));
        pose.mulPose(Axis.ZP.rotationDegrees(112.5f));
        pose.translate(0, 0.04 + (1 - spread) * 0.09, (1 - spread) * -0.1875);
        pose.mulPose(Axis.YP.rotationDegrees((1 - spread) * -90));
        model.setupAnim(time, page(flip + 0.25f), page(flip + 0.75f), spread);
        model.renderToBuffer(pose, buffers.getBuffer(RENDER_TYPE), light, overlay, -1);
        pose.popPose();
    }

    private static float page(float flip) {
        return Mth.clamp(Mth.frac(flip) * 1.6f - 0.3f, 0, 1);
    }
}
