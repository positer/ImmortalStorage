package com.immortalstorage.immortalstorage.client.render;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TargetRuinPreviewRenderContractTest {
    @Test
    void targetPreviewUsesPresentTextureAndPreservesWhiteGreenRedModes() throws Exception {
        String faces = Files.readString(generated("RuinFaceHighlightRenderer.java"));
        String core = Files.readString(generated("MiniatureImmortalRuinRenderer.java"));
        assertTrue(faces.contains("textures/block/white_concrete.png"));
        assertTrue(core.contains("textures/block/white_concrete.png"));
        assertFalse(faces.contains("textures/misc/white.png"));
        assertFalse(core.contains("textures/misc/white.png"));
        assertTrue(faces.contains("render(poses, buffers, box, face, 1.0F, 1.0F, 1.0F, 0.35F)"));
        assertTrue(faces.contains("0.0F, 1.0F, 0.0F, 0.35F"));
        assertTrue(faces.contains("1.0F, 0.0F, 0.0F, 0.35F"));
        assertTrue(core.contains("int coreColor = reversed ? 0xFFFFFF : 0x000000"));
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
}
