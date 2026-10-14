package com.immortalstorage.immortalstorage.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Depth-independent colour-only lines never write into the world's depth buffer. */
public final class ToolLocatorRenderer {
    private static final RenderType LINES=RenderType.create("immortalstorage_tool_locator",DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.DEBUG_LINES,1536,false,false,RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionColorShader))
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL).setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setLineState(new RenderStateShard.LineStateShard(java.util.OptionalDouble.of(2D))).createCompositeState(false));
    private ToolLocatorRenderer() {}
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc=Minecraft.getInstance();
        if (mc.level==null || mc.player==null || mc.screen!=null || ToolLocatorClient.visible().isEmpty()) return;
        var poses=event.getPoseStack();var camera=event.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();
        var vertices=buffers.getBuffer(LINES);
        for(var target:ToolLocatorClient.visible()) {
            poses.pushPose();poses.translate(target.pos().getX()-camera.x,target.pos().getY()-camera.y,target.pos().getZ()-camera.z);
            ToolLocatorEdges.draw(poses,vertices,target);poses.popPose();
        }
        buffers.endBatch(LINES);
    }
}
