package com.immortalstorage.immortalstorage.client.render;
import com.immortalstorage.immortalstorage.compat.mc2612.LegacyBlockEntityRenderer;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatRenderTypes;

import com.immortalstorage.immortalstorage.block.custom.MiniatureImmortalRuinBlock;
import com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** View-independent black sphere silhouette wrapped by a solid white emissive edge and glow. */
public final class MiniatureImmortalRuinRenderer extends LegacyBlockEntityRenderer<MiniatureImmortalRuinBlockEntity> {
    private static final RenderType OPAQUE_LAYER = CompatRenderTypes.entityCutoutNoCull(
            Identifier.withDefaultNamespace("textures/misc/white.png"));
    private static final RenderType OUTLINE_LAYER = CompatRenderTypes.entityTranslucentEmissive(
            Identifier.withDefaultNamespace("textures/misc/white.png"));
    public MiniatureImmortalRuinRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void legacyRender(MiniatureImmortalRuinBlockEntity entity, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay, net.minecraft.world.phys.Vec3 cameraPosition) {
        float time = (entity.getLevel() == null ? 0 : entity.getLevel().getGameTime()) + partialTick;
        float scale = 0.34F + Mth.sin(time * 0.12F) * 0.035F;
        boolean reversed = entity.getBlockState().getValue(MiniatureImmortalRuinBlock.REVERSED);
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        poses.pushPose();
        poses.translate(0.5F, 0.5F, 0.5F);
        poses.mulPose(camera.rotation());
        drawDisc(poses, buffers, scale, reversed);
        poses.popPose();
        RuinLinkRenderer.render(poses, buffers, entity.getBlockPos(), entity.linkedPos());
    }

    static void drawDisc(PoseStack poses, MultiBufferSource buffers, float radius, boolean reversed) {
        int segments = 32;
        int coreColor = reversed ? 0xFFFFFF : 0x000000;
        int edgeColor = reversed ? 0x000000 : 0xFFFFFF;

        // A thick full-bright silhouette edge mimics the glowing entity outline
        // without the through-wall outline framebuffer. The ordinary entity
        // cutout layer keeps depth testing, so opaque blocks still occlude it.
        drawRing(buffers.getBuffer(OUTLINE_LAYER), poses, segments,
                radius * 0.78F, radius, edgeColor, 150, -0.002F);
        drawRing(buffers.getBuffer(OPAQUE_LAYER), poses, segments,
                0.0F, radius * 0.76F, coreColor, 255, 0.0F);
    }

    private static void drawRing(VertexConsumer consumer, PoseStack poses, int segments,
                                 float inner, float outer, int color, int alpha, float z) {
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (Math.PI * 2.0D * i / segments);
            float a1 = (float) (Math.PI * 2.0D * (i + 1) / segments);
            vertex(consumer, poses, inner * Mth.cos(a0), inner * Mth.sin(a0), z, color, alpha);
            vertex(consumer, poses, outer * Mth.cos(a0), outer * Mth.sin(a0), z, color, alpha);
            vertex(consumer, poses, outer * Mth.cos(a1), outer * Mth.sin(a1), z, color, alpha);
            vertex(consumer, poses, inner * Mth.cos(a1), inner * Mth.sin(a1), z, color, alpha);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack poses, float x, float y, float z,
                               int rgb, int alpha) {
        consumer.addVertex(poses.last(), x, y, z).setColor(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, alpha)
                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0x00F000F0).setNormal(poses.last(), 0, 0, 1);
    }
}
