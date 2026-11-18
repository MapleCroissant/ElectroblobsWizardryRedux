package com.binaris.wizardry.client.particle;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.client.particle.ParticleTargeted;
import com.binaris.wizardry.api.client.particle.ParticleWizardry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.stream.IntStream;

public final class ParticleVine extends ParticleTargeted {
    private static final ResourceLocation STEM = WizardryMainMod.location("textures/particle/vine.png");
    private static final ResourceLocation[] LEAVES = IntStream.range(0, 5)
            .mapToObj(index -> WizardryMainMod.location("textures/particle/vine_leaf_" + index + ".png"))
            .toArray(ResourceLocation[]::new);
    private static final float WIDTH = 0.02f;

    public ParticleVine(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites, false);
        setLifetime(0);
    }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override
    protected boolean shouldApplyOriginOffset() { return false; }

    @Override
    protected void draw(PoseStack poses, Tesselator tessellator, float length, float partialTick) {
        if (length < 0.01f) return;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        try {
            RenderSystem.setShaderTexture(0, STEM);
            var stem = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (float end = length; end > 0; end -= 1) {
                float start = Math.max(0, end - 1);
                for (int side = 0; side < 4; side++) {
                    poses.pushPose();
                    poses.mulPose(Axis.ZP.rotationDegrees(side * 90));
                    var matrix = poses.last().pose();
                    float bottom = side * 0.0625f;
                    float top = bottom + 0.0625f;
                    stem.addVertex(matrix, -WIDTH, WIDTH, end).setUv(0, bottom).setColor(51, 166, 0, 255);
                    stem.addVertex(matrix, -WIDTH, WIDTH, start).setUv(end - start, bottom).setColor(51, 166, 0, 255);
                    stem.addVertex(matrix, WIDTH, WIDTH, start).setUv(end - start, top).setColor(51, 166, 0, 255);
                    stem.addVertex(matrix, WIDTH, WIDTH, end).setUv(0, top).setColor(51, 166, 0, 255);
                    poses.popPose();
                }
            }
            BufferUploader.drawWithShader(stem.buildOrThrow());

            for (int texture = 0; texture < LEAVES.length; texture++) {
                random.setSeed(seed);
                BufferBuilder leaves = null;
                for (float depth = length; depth > 0; depth -= 0.5f) {
                    int quarter = random.nextInt(4);
                    int index = random.nextInt(5);
                    int shade = random.nextInt(77) - 38;
                    if (index != texture) continue;
                    if (leaves == null) {
                        RenderSystem.setShaderTexture(0, LEAVES[texture]);
                        leaves = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                    }
                    poses.pushPose();
                    poses.mulPose(Axis.ZP.rotationDegrees(quarter * 90));
                    var matrix = poses.last().pose();
                    float size = WIDTH * 16;
                    addLeaf(leaves, matrix, 0, 0, depth, 0, 0, shade);
                    addLeaf(leaves, matrix, size, 0, depth, 1, 0, shade);
                    addLeaf(leaves, matrix, size, size, depth, 1, 1, shade);
                    addLeaf(leaves, matrix, 0, size, depth, 0, 1, shade);
                    poses.popPose();
                }
                if (leaves != null) BufferUploader.drawWithShader(leaves.buildOrThrow());
            }
        } finally {
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static void addLeaf(BufferBuilder out, org.joml.Matrix4f pose, float x, float y, float z, float u, float v, int shade) {
        out.addVertex(pose, x, y, z).setUv(u, v).setColor(51 + shade, 166 + shade, Math.max(0, shade), 255);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private static SpriteSet sprites;

        public Provider(SpriteSet sprites) { Provider.sprites = sprites; }

        public static ParticleWizardry create(ClientLevel level, Vec3 pos) {
            return new ParticleVine(level, pos.x, pos.y, pos.z, sprites);
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new ParticleVine(level, x, y, z, sprites);
        }
    }
}
