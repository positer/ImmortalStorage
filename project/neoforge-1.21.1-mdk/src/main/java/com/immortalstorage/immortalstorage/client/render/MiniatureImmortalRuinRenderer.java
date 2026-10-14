package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.custom.MiniatureImmortalRuinBlock;
import com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

/** Breathing cube rendered as a flat projected silhouette with no internal face seams. */
public final class MiniatureImmortalRuinRenderer implements BlockEntityRenderer<MiniatureImmortalRuinBlockEntity> {
    private static final RenderType POLARITY_LAYER = RenderType.create(
            "immortalstorage_ruin_negative_texture", DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, false, RenderType.CompositeState.builder()
                    .setShaderState(new net.minecraft.client.renderer.RenderStateShard.ShaderStateShard(
                            net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
                    .setTextureState(new net.minecraft.client.renderer.RenderStateShard.TextureStateShard(
                            net.minecraft.resources.ResourceLocation.parse("immortalstorage:textures/block/ruin_polarity.png"), false, false))
                    .setTransparencyState(net.minecraft.client.renderer.RenderStateShard.NO_TRANSPARENCY)
                    .setDepthTestState(net.minecraft.client.renderer.RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(net.minecraft.client.renderer.RenderStateShard.CULL)
                    .setWriteMaskState(net.minecraft.client.renderer.RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(false));
    public MiniatureImmortalRuinRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MiniatureImmortalRuinBlockEntity entity, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        float time = (entity.getLevel() == null ? 0 : entity.getLevel().getGameTime()) + partialTick;
        drawAnimated(poses, buffers, time, entity.getBlockState().getValue(MiniatureImmortalRuinBlock.REVERSED));
        RuinLinkRenderer.render(poses, buffers, entity.getBlockPos(), entity.linkedPos());
    }

    public static void drawAnimated(PoseStack poses, MultiBufferSource buffers, float time, boolean reversed) {
        float scale = 0.34F + Mth.sin(time * 0.12F) * 0.035F;
        poses.pushPose(); poses.translate(0.5F, 0.5F, 0.5F);
        poses.mulPose(com.mojang.math.Axis.YP.rotation(time * 0.018F));
        poses.mulPose(com.mojang.math.Axis.XP.rotation(time * 0.011F));
        drawDisc(poses, buffers, scale, reversed); poses.popPose();
    }

    static void drawDisc(PoseStack poses, MultiBufferSource buffers, float radius, boolean reversed) {
        int coreColor = reversed ? 0xFFFFFF : 0x000000;
        int edgeColor = reversed ? 0x000000 : 0xFFFFFF;
        // Inverted outer hull first, smaller outward core second. Back-face
        // culling exposes the far hull, so the nearer core survives depth testing.
        // Both write depth and remain fully opaque, including against particles.
        drawCube(buffers.getBuffer(POLARITY_LAYER), poses, radius, edgeColor, 255, true);
        drawCube(buffers.getBuffer(POLARITY_LAYER), poses, radius * 0.78F, coreColor, 255, false);
    }

    private static void drawCube(VertexConsumer consumer, PoseStack poses, float radius, int color, int alpha, boolean inverted) {
        face(consumer, poses, -radius, -radius, radius, radius, -radius, radius,
                radius, radius, radius, -radius, radius, radius, 0, 0, 1, color, alpha, inverted);
        face(consumer, poses, radius, -radius, -radius, -radius, -radius, -radius,
                -radius, radius, -radius, radius, radius, -radius, 0, 0, -1, color, alpha, inverted);
        face(consumer, poses, radius, -radius, radius, radius, -radius, -radius,
                radius, radius, -radius, radius, radius, radius, 1, 0, 0, color, alpha, inverted);
        face(consumer, poses, -radius, -radius, -radius, -radius, -radius, radius,
                -radius, radius, radius, -radius, radius, -radius, -1, 0, 0, color, alpha, inverted);
        face(consumer, poses, -radius, radius, radius, radius, radius, radius,
                radius, radius, -radius, -radius, radius, -radius, 0, 1, 0, color, alpha, inverted);
        face(consumer, poses, -radius, -radius, -radius, radius, -radius, -radius,
                radius, -radius, radius, -radius, -radius, radius, 0, -1, 0, color, alpha, inverted);
    }

    private static void face(VertexConsumer consumer, PoseStack poses,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float nx, float ny, float nz, int rgb, int alpha, boolean inverted) {
        vertex(consumer, poses, x0, y0, z0, nx, ny, nz, rgb, alpha);
        if (inverted) {
            vertex(consumer, poses, x3, y3, z3, nx, ny, nz, rgb, alpha);
            vertex(consumer, poses, x2, y2, z2, nx, ny, nz, rgb, alpha);
            vertex(consumer, poses, x1, y1, z1, nx, ny, nz, rgb, alpha);
        } else {
            vertex(consumer, poses, x1, y1, z1, nx, ny, nz, rgb, alpha);
            vertex(consumer, poses, x2, y2, z2, nx, ny, nz, rgb, alpha);
            vertex(consumer, poses, x3, y3, z3, nx, ny, nz, rgb, alpha);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack poses, float x, float y, float z,
                               float nx, float ny, float nz, int rgb, int alpha) {
        consumer.addVertex(poses.last(), x, y, z)
                .setUv(rgb == 0 ? 0.75F : 0.25F, 0.5F).setColor(255, 255, 255, alpha);
    }
}
