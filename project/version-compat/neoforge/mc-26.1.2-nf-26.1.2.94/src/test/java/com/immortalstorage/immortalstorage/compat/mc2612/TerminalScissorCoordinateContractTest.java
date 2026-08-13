package com.immortalstorage.immortalstorage.compat.mc2612;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalScissorCoordinateContractTest {
    @Test
    void foregroundStorageScissorCancelsTheContainerPoseTranslation() throws IOException {
        Path source = generatedScreenRoot().resolve("AbstractTerminalScreen.java");
        String screen = Files.readString(source);
        int start = screen.indexOf("protected final void enableStorageContentScissor");
        int end = screen.indexOf("\n    }", start);
        String helper = screen.substring(start, end);
        assertTrue(helper.contains("clip.getX() - this.leftPos"));
        assertTrue(helper.contains("clip.getY() - this.topPos"));
        assertTrue(helper.contains("clip.getX() + clip.getWidth() - this.leftPos"));
        assertTrue(helper.contains("clip.getY() + clip.getHeight() - this.topPos"));
    }

    private static Path generatedScreenRoot() {
        Path current = Path.of("").toAbsolutePath();
        Path marker = Path.of("project", "version-compat", "neoforge",
                "mc-26.1.2-nf-26.1.2.94", "src", "main", "generated-java", "com",
                "immortalstorage", "immortalstorage", "client", "screen");
        while (current != null) {
            Path candidate = current.resolve(marker);
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate 26.1.2 generated screen source root");
    }
}
