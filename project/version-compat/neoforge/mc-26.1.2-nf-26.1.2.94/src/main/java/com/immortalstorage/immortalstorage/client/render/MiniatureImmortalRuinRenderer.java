package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.custom.MiniatureImmortalRuinBlock;
import com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity;
import com.immortalstorage.immortalstorage.compat.mc2612.LegacyBlockEntityRenderer;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;

/** Target-compatible pure-color projected silhouette renderer. */
public final class MiniatureImmortalRuinRenderer extends LegacyBlockEntityRenderer<MiniatureImmortalRuinBlockEntity> {
    private static final RenderPipeline SILHOUETTE_RIM_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation("immortalstorage/pipeline/ruin_silhouette_rim")
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR,
                    com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS)
            .build();
    private static final RenderPipeline OPAQUE_CORE_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation("immortalstorage/pipeline/ruin_opaque_core")
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR,
                    com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS)
            .build();
    private static final RenderType SILHOUETTE_RIM_LAYER = RenderType.create(
            "immortalstorage_ruin_silhouette_rim", RenderSetup.builder(SILHOUETTE_RIM_PIPELINE).createRenderSetup());
    private static final RenderType OPAQUE_CORE_LAYER = RenderType.create(
            "immortalstorage_ruin_opaque_core", RenderSetup.builder(OPAQUE_CORE_PIPELINE).createRenderSetup());

    public MiniatureImmortalRuinRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void legacyRender(MiniatureImmortalRuinBlockEntity entity, float partialTick, PoseStack poses,
                             net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay,
                             net.minecraft.world.phys.Vec3 cameraPosition) {
        float time = (entity.getLevel() == null ? 0 : entity.getLevel().getGameTime()) + partialTick;
        float scale = 0.34F + Mth.sin(time * 0.12F) * 0.035F;
        boolean reversed = entity.getBlockState().getValue(MiniatureImmortalRuinBlock.REVERSED);
        poses.pushPose();
        poses.translate(0.5F, 0.5F, 0.5F);
        poses.mulPose(com.mojang.math.Axis.YP.rotation(time * 0.018F));
        poses.mulPose(com.mojang.math.Axis.XP.rotation(time * 0.011F));
        drawDisc(poses, buffers, scale, reversed);
        poses.popPose();
        RuinLinkRenderer.render(poses, buffers, entity.getBlockPos(), entity.linkedPos());
    }

    static void drawDisc(PoseStack poses, net.minecraft.client.renderer.MultiBufferSource buffers,
                         float radius, boolean reversed) {
        int coreColor = reversed ? 0xFFFFFF : 0x000000;
        int edgeColor = reversed ? 0x000000 : 0xFFFFFF;
        drawCube(buffers.getBuffer(SILHOUETTE_RIM_LAYER), poses, radius, edgeColor);
        drawCube(buffers.getBuffer(OPAQUE_CORE_LAYER), poses, radius * 0.78F, coreColor);
    }

    private static void drawCube(VertexConsumer consumer, PoseStack poses, float radius, int color) {
        face(consumer, poses, -radius,-radius,radius, radius,-radius,radius, radius,radius,radius,-radius,radius,radius,0,0,1,color);
        face(consumer, poses, radius,-radius,-radius, -radius,-radius,-radius, -radius,radius,-radius, radius,radius,-radius,0,0,-1,color);
        face(consumer, poses, radius,-radius,radius, radius,-radius,-radius, radius,radius,-radius, radius,radius,radius,1,0,0,color);
        face(consumer, poses, -radius,-radius,-radius, -radius,-radius,radius, -radius,radius,radius, -radius,radius,-radius,-1,0,0,color);
        face(consumer, poses, -radius,radius,radius, radius,radius,radius, radius,radius,-radius, -radius,radius,-radius,0,1,0,color);
        face(consumer, poses, -radius,-radius,-radius, radius,-radius,-radius, radius,-radius,radius, -radius,-radius,radius,0,-1,0,color);
    }

    private static void face(VertexConsumer c, PoseStack p, float x0,float y0,float z0,float x1,float y1,float z1,
                             float x2,float y2,float z2,float x3,float y3,float z3,float nx,float ny,float nz,int rgb) {
        vertex(c,p,x0,y0,z0,nx,ny,nz,rgb); vertex(c,p,x1,y1,z1,nx,ny,nz,rgb);
        vertex(c,p,x2,y2,z2,nx,ny,nz,rgb); vertex(c,p,x3,y3,z3,nx,ny,nz,rgb);
    }

    private static void vertex(VertexConsumer c, PoseStack p, float x,float y,float z,float nx,float ny,float nz,int rgb) {
        c.addVertex(p.last(),x,y,z).setColor(rgb>>16&255,rgb>>8&255,rgb&255,255);
    }
}
