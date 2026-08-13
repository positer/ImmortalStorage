package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class FacePreviewButtonContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void interfaceSourceAndCacheMasksUseAdjacentBlockPreviewButtons() throws IOException {
        Path java = locateProject().resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com",
                "immortalstorage", "immortalstorage", "client", "screen"));
        String button = Files.readString(java.resolve("FacePreviewButton.java"));
        String xianqiao = Files.readString(java.resolve("XianqiaoInterfaceScreen.java"));
        String source = Files.readString(java.resolve("SourceVeinScreen.java"));

        assertTrue(button.contains("graphics.fakeItem"));
        assertTrue(xianqiao.contains("adjacentBlockPreview"));
        assertTrue(xianqiao.contains("interfaceModeColor"));
        assertTrue(xianqiao.contains("slot_face_tooltip"));
        assertTrue(source.contains("adjacentBlockPreview"));
        assertTrue(source.contains("sourceModeColor"));
        assertTrue(source.contains("case BYPASS_PUSH -> 0xFF9A4BC2"));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))
                    && Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project root");
    }
}
