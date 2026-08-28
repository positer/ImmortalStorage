package com.immortalstorage.immortalstorage.client.render;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuinPreviewRenderContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void previewUsesPureColorDepthOccludedSilhouetteLayers() throws Exception {
        String faces = Files.readString(source("RuinFaceHighlightRenderer.java"));
        String core = Files.readString(source("MiniatureImmortalRuinRenderer.java"));
        assertTrue(faces.contains("textures/block/white_concrete.png"));
        assertFalse(faces.contains("textures/misc/white.png"));
        assertTrue(faces.contains("render(poses, buffers, box, face, 1.0F, 1.0F, 1.0F, 0.35F)"));
        assertTrue(faces.contains("0.0F, 1.0F, 0.0F, 0.35F"));
        assertTrue(faces.contains("1.0F, 0.0F, 0.0F, 0.35F"));
        assertTrue(core.contains("int coreColor = reversed ? 0xFFFFFF : 0x000000"));
        assertTrue(core.contains("drawCube"));
        assertFalse(core.contains("drawRing"));
        assertTrue(core.contains("DefaultVertexFormat.POSITION_COLOR"));
        assertTrue(core.contains("POSITION_COLOR_SHADER") || core.contains("RenderPipelines.DEBUG_FILLED_SNIPPET"));
        assertFalse(core.contains("textures/block/white_concrete.png"));
        assertFalse(core.contains("setUv("));
        assertFalse(core.contains("setNormal("));
        assertFalse(core.contains("setLight("));
        assertFalse(core.contains("GREATER_DEPTH_TEST"));
        assertTrue(core.contains("LEQUAL_DEPTH_TEST") || core.contains("CompareOp.LESS_THAN_OR_EQUAL"));
        assertTrue(core.contains("COLOR_WRITE") || core.contains("DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false)"));
        boolean canonicalDualLayer = core.contains("SILHOUETTE_RIM_LAYER")
                && core.contains("OPAQUE_CORE_LAYER");
        boolean targetSingleSubmission = core.contains("PROJECTED_SILHOUETTE_LAYER")
                && core.contains("DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false)")
                && !core.contains("DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true)");
        assertTrue(canonicalDualLayer || targetSingleSubmission);
        assertFalse(core.contains("entityTranslucentEmissive"));
        if (canonicalDualLayer) {
            assertTrue(core.contains("COLOR_DEPTH_WRITE")
                    || core.contains("DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true)"));
            assertTrue(core.indexOf("buffers.getBuffer(SILHOUETTE_RIM_LAYER)")
                    < core.indexOf("buffers.getBuffer(OPAQUE_CORE_LAYER)"));
        } else {
            assertTrue(core.indexOf("drawCube(consumer, poses, radius, edgeColor)")
                    < core.indexOf("drawCube(consumer, poses, radius * 0.78F, coreColor)"));
            assertFalse(core.contains("drawBoundaryBeams"));
        }
        assertFalse(core.contains("edgeColor, 112"));
        for (String itemRenderer : new String[]{"RuinCoreItemDecorator.java",
                "EntangledRuinCoreItemDecorator.java", "GuiItemPreviewOverlay.java"}) {
            String item = Files.readString(source(itemRenderer));
            assertTrue(item.contains("drawSquare"), itemRenderer);
            assertFalse(item.contains("drawDisc"), itemRenderer);
        }
    }

    private static Path source(String name) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage",
                    "immortalstorage", "client", "render", name));
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate renderer " + name);
    }
}
