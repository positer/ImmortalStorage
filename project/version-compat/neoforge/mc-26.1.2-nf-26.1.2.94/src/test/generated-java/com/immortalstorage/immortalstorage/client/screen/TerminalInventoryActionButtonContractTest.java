package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalInventoryActionButtonContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void inventoryActionsUseHalfSizeButtonsAndCompactSpacing() throws IOException {
        String button = read("TerminalInventoryActionButton.java");
        String screen = read("XianqiaoStorageScreen.java");

        assertTrue(button.contains("static final int SIZE = 8"));
        assertTrue(button.contains("static final int SPACING = 10"));
        assertTrue(button.contains("super(x, y, SIZE, SIZE"));
        assertTrue(screen.contains("TerminalInventoryActionButton.SPACING * 2"));
    }

    private static String read(String file) throws IOException {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com",
                    "immortalstorage", "immortalstorage", "client", "screen", file));
            if (Files.isRegularFile(candidate)) return Files.readString(candidate);
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate " + file);
    }
}
