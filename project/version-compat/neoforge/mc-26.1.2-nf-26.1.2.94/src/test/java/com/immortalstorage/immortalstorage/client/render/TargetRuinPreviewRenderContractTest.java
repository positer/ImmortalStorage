package com.immortalstorage.immortalstorage.client.render;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TargetRuinPreviewRenderContractTest {
    @Test
    void targetPreviewUsesPureColorDepthOccludedSilhouetteLayers() throws Exception {
        String faces = Files.readString(generated("RuinFaceHighlightRenderer.java"));
        String core = Files.readString(effective("MiniatureImmortalRuinRenderer.java"));
        assertTrue(faces.contains("textures/block/white_concrete.png"));
        assertFalse(faces.contains("textures/misc/white.png"));
        assertTrue(faces.contains("render(poses, buffers, box, face, 1.0F, 1.0F, 1.0F, 0.35F)"));
        assertTrue(faces.contains("0.0F, 1.0F, 0.0F, 0.35F"));
        assertTrue(faces.contains("1.0F, 0.0F, 0.0F, 0.35F"));
        assertTrue(core.contains("int coreColor = reversed ? 0xFFFFFF : 0x000000"));
        assertTrue(core.contains("drawCube"));
        assertFalse(core.contains("drawRing"));
        assertTrue(core.contains("DefaultVertexFormat.POSITION_TEX_COLOR"));
        assertTrue(core.contains("RenderPipelines.DEBUG_FILLED_SNIPPET"));
        assertFalse(core.contains("textures/block/white_concrete.png"));
        assertTrue(core.contains("setUv("));
        assertTrue(core.contains("ruin_polarity.png"));
        // The projection's forwarding VertexConsumer implements the full interface;
        // actual silhouette vertices still contain no normal or lighting attributes.
        String vertexBody = core.substring(core.indexOf("private static void vertex("));
        assertFalse(vertexBody.contains("setNormal("));
        assertFalse(core.contains("setLight("));
        assertFalse(core.contains("GREATER_DEPTH_TEST"));
        assertTrue(core.contains("CompareOp.LESS_THAN_OR_EQUAL"));
        assertFalse(core.contains("DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false)"));
        assertTrue(core.contains("DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true)"));
        assertTrue(core.contains(".withCull(true)"));
        assertTrue(core.contains("ColorTargetState.DEFAULT"));
        String opaquePipeline = core.substring(core.indexOf("private static final RenderPipeline PROJECTED_SILHOUETTE_PIPELINE"),
                core.indexOf("private static final RenderPipeline HANDBOOK_PROJECTION_PIPELINE"));
        assertFalse(opaquePipeline.contains("BlendFunction"));
        assertTrue(core.contains("BlendFunction.TRANSLUCENT"));
        assertTrue(core.contains("drawProjection("));
        assertTrue(core.contains("target.setColor(r,g,b,alpha)"));
        assertTrue(core.contains("PROJECTED_SILHOUETTE_LAYER"));
        assertTrue(core.contains("Inverted outer hull first, smaller outward core second"));
        assertFalse(core.contains("drawBoundaryBeams"));
        assertFalse(core.contains("beamHalfWidth"));
        assertFalse(core.contains("entityTranslucentEmissive"));
        assertFalse(core.contains("SILHOUETTE_RIM_LAYER"));
        assertFalse(core.contains("OPAQUE_CORE_LAYER"));
        assertTrue(core.indexOf("drawCube(consumer, poses, radius, edgeColor, true)")
                < core.indexOf("drawCube(consumer, poses, radius * 0.78F, coreColor, false)"));
        assertFalse(core.contains("edgeColor, 112"));
    }

    private static Path generated(String name) {
        Path current = Path.of("").toAbsolutePath();
        Path suffix = Path.of("project", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94",
                "src", "main", "generated-java", "com", "immortalstorage", "immortalstorage",
                "client", "render", name);
        while (current != null) {
            Path candidate = current.resolve(suffix);
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate generated renderer " + name);
    }

    private static Path effective(String name) {
        Path current = Path.of(".").toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(Path.of("project", "version-compat", "neoforge",
                    "mc-26.1.2-nf-26.1.2.94", "src", "main", "java", "com", "immortalstorage",
                    "immortalstorage", "client", "render", name));
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        return generated(name);
    }
}
