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
    private static final RenderPipeline PROJECTED_SILHOUETTE_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexShader("core/position_tex_color").withFragmentShader("core/position_tex_color")
            .withSampler("Sampler0")
            .withLocation("immortalstorage/pipeline/ruin_projected_silhouette")
            .withCull(true)
            .withColorTargetState(com.mojang.blaze3d.pipeline.ColorTargetState.DEFAULT)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR,
                    com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS)
            .build();
    private static final RenderType PROJECTED_SILHOUETTE_LAYER = RenderType.create(
            "immortalstorage_ruin_projected_silhouette",
            RenderSetup.builder(PROJECTED_SILHOUETTE_PIPELINE)
                    .withTexture("Sampler0", net.minecraft.resources.Identifier.parse("immortalstorage:textures/block/ruin_polarity.png"))
                    .createRenderSetup());
    private static final RenderPipeline HANDBOOK_PROJECTION_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexShader("core/position_tex_color").withFragmentShader("core/position_tex_color")
            .withSampler("Sampler0").withLocation("immortalstorage/pipeline/ruin_handbook_projection")
            .withCull(true)
            .withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS).build();
    private static final RenderType HANDBOOK_PROJECTION_LAYER = RenderType.create("immortalstorage_ruin_handbook_projection",
            RenderSetup.builder(HANDBOOK_PROJECTION_PIPELINE)
                    .withTexture("Sampler0", net.minecraft.resources.Identifier.parse("immortalstorage:textures/block/ruin_polarity.png"))
                    .createRenderSetup());

    public MiniatureImmortalRuinRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void legacyRender(MiniatureImmortalRuinBlockEntity entity, float partialTick, PoseStack poses,
                             net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay,
                             net.minecraft.world.phys.Vec3 cameraPosition) {
        float time = (entity.getLevel() == null ? 0 : entity.getLevel().getGameTime()) + partialTick;
        drawAnimated(poses, buffers, time, entity.getBlockState().getValue(MiniatureImmortalRuinBlock.REVERSED));
        RuinLinkRenderer.render(poses, buffers, entity.getBlockPos(), entity.linkedPos());
    }

    public static void drawAnimated(PoseStack poses, net.minecraft.client.renderer.MultiBufferSource buffers, float time, boolean reversed) {
        float scale = 0.34F + Mth.sin(time * 0.12F) * 0.035F;
        poses.pushPose(); poses.translate(0.5F, 0.5F, 0.5F);
        poses.mulPose(com.mojang.math.Axis.YP.rotation(time * 0.018F));
        poses.mulPose(com.mojang.math.Axis.XP.rotation(time * 0.011F));
        drawDisc(poses, buffers, scale, reversed); poses.popPose();
    }

    /** Only the handbook's ghost uses blending; placed and held ruins keep the opaque pipeline. */
    public static void drawProjection(PoseStack poses, net.minecraft.client.renderer.MultiBufferSource buffers,
                                       float time, boolean reversed, float opacity) {
        int alpha = Math.round(Mth.clamp(opacity, 0F, 1F) * 255F);
        drawAnimated(poses, ignored -> {
            VertexConsumer target = buffers.getBuffer(HANDBOOK_PROJECTION_LAYER);
            return new VertexConsumer() {
                @Override public VertexConsumer addVertex(float x,float y,float z) { target.addVertex(x,y,z); return this; }
                @Override public VertexConsumer setColor(int r,int g,int b,int a) { target.setColor(r,g,b,alpha); return this; }
                @Override public VertexConsumer setColor(int color) { target.setColor((color & 0x00FFFFFF) | (alpha << 24)); return this; }
                @Override public VertexConsumer setUv(float u,float v) { target.setUv(u,v); return this; }
                @Override public VertexConsumer setUv1(int u,int v) { target.setUv1(u,v); return this; }
                @Override public VertexConsumer setUv2(int u,int v) { target.setUv2(u,v); return this; }
                @Override public VertexConsumer setNormal(float x,float y,float z) { target.setNormal(x,y,z); return this; }
                @Override public VertexConsumer setLineWidth(float width) { target.setLineWidth(width); return this; }
            };
        }, time, reversed);
    }

    static void drawDisc(PoseStack poses, net.minecraft.client.renderer.MultiBufferSource buffers,
                         float radius, boolean reversed) {
        int coreColor = reversed ? 0xFFFFFF : 0x000000;
        int edgeColor = reversed ? 0x000000 : 0xFFFFFF;
        // Inverted outer hull first, smaller outward core second. Back-face
        // culling exposes the far hull, so the nearer core survives depth testing.
        // Both write depth and remain fully opaque, including against particles.
        VertexConsumer consumer = buffers.getBuffer(PROJECTED_SILHOUETTE_LAYER);
        drawCube(consumer, poses, radius, edgeColor, true);
        drawCube(consumer, poses, radius * 0.78F, coreColor, false);
    }

    private static void drawCube(VertexConsumer consumer, PoseStack poses, float radius, int color, boolean inverted) {
        face(consumer, poses, -radius,-radius,radius, radius,-radius,radius, radius,radius,radius,-radius,radius,radius,0,0,1,color,inverted);
        face(consumer, poses, radius,-radius,-radius, -radius,-radius,-radius, -radius,radius,-radius,radius,radius,-radius,0,0,-1,color,inverted);
        face(consumer, poses, radius,-radius,radius, radius,-radius,-radius, radius,radius,-radius,radius,radius,radius,1,0,0,color,inverted);
        face(consumer, poses, -radius,-radius,-radius, -radius,-radius,radius, -radius,radius,radius,-radius,radius,-radius,-1,0,0,color,inverted);
        face(consumer, poses, -radius,radius,radius, radius,radius,radius, radius,radius,-radius,-radius,radius,-radius,0,1,0,color,inverted);
        face(consumer, poses, -radius,-radius,-radius, radius,-radius,-radius, radius,-radius,radius,-radius,-radius,radius,0,-1,0,color,inverted);
    }

    private static void face(VertexConsumer c, PoseStack p, float x0,float y0,float z0,float x1,float y1,float z1,
                             float x2,float y2,float z2,float x3,float y3,float z3,float nx,float ny,float nz,int rgb, boolean inverted) {
        vertex(c,p,x0,y0,z0,nx,ny,nz,rgb);
        if (inverted) {
            vertex(c,p,x3,y3,z3,nx,ny,nz,rgb); vertex(c,p,x2,y2,z2,nx,ny,nz,rgb);
            vertex(c,p,x1,y1,z1,nx,ny,nz,rgb);
        } else {
            vertex(c,p,x1,y1,z1,nx,ny,nz,rgb); vertex(c,p,x2,y2,z2,nx,ny,nz,rgb);
            vertex(c,p,x3,y3,z3,nx,ny,nz,rgb);
        }
    }

    private static void vertex(VertexConsumer c, PoseStack p, float x,float y,float z,float nx,float ny,float nz,int rgb) {
        c.addVertex(p.last(),x,y,z).setUv(rgb == 0 ? 0.75F : 0.25F, 0.5F).setColor(255,255,255,255);
    }
}
