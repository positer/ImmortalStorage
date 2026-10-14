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
        assertTrue(core.contains("DefaultVertexFormat.NEW_ENTITY") || core.contains("DefaultVertexFormat.POSITION_TEX_COLOR"));
        assertTrue(core.contains("ruin_polarity.png"));
        assertTrue(core.contains("setUv("));
        assertTrue(core.contains("0.75F : 0.25F"));
        assertFalse(core.contains("GREATER_DEPTH_TEST"));
        assertTrue(core.contains("LEQUAL_DEPTH_TEST") || core.contains("CompareOp.LESS_THAN_OR_EQUAL"));
        assertTrue(core.contains("COLOR_DEPTH_WRITE") || core.contains("DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true)"));
        assertTrue(core.contains("radius * 0.78F"));
        assertTrue(core.indexOf("radius, edgeColor") < core.indexOf("radius * 0.78F, coreColor"));
        assertFalse(core.contains("drawBoundaryBeams"));
        assertFalse(core.contains("edgeColor, 112"));
        String nativeItem = Files.readString(source("WorldBlockItemRenderer.java"));
        assertTrue(nativeItem.contains("getRenderer(visual.entity())"));
        assertFalse(nativeItem.contains("drawSquare"));
        assertFalse(Files.readString(source("GuiItemPreviewOverlay.java")).contains("drawSquare"));
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
