package com.immortalstorage.immortalstorage.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class ToolLocatorRenderer {
    private static final RenderPipeline PIPELINE=RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation("immortalstorage/pipeline/tool_locator").withCull(false)
            .withColorTargetState(com.mojang.blaze3d.pipeline.ColorTargetState.DEFAULT)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS,false))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.DEBUG_LINES).build();
    private static final RenderType LINES=RenderType.create("immortalstorage_tool_locator",RenderSetup.builder(PIPELINE).createRenderSetup());
    private ToolLocatorRenderer() {}
    public static void render(RenderLevelStageEvent.AfterTranslucentParticles event) {
        var mc=Minecraft.getInstance();
        if (mc.level==null || mc.player==null || mc.screen!=null || ToolLocatorClient.visible().isEmpty()) return;
        var poses=event.getPoseStack();var camera=event.getLevelRenderState().cameraRenderState.pos;var buffers=mc.renderBuffers().bufferSource();
        var vertices=buffers.getBuffer(LINES);
        for(var target:ToolLocatorClient.visible()) {
            poses.pushPose();poses.translate(target.pos().getX()-camera.x,target.pos().getY()-camera.y,target.pos().getZ()-camera.z);
            ToolLocatorEdges.draw(poses,vertices,target);poses.popPose();
        }
        buffers.endBatch(LINES);
    }
}
