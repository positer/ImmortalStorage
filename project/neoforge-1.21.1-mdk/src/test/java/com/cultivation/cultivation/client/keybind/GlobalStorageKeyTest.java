package com.cultivation.cultivation.client.keybind;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class GlobalStorageKeyTest {
    @Test
    void storageKeyUsesConfiguredMappingAcrossScreensAndTerminalReturns() throws Exception {
        Path root = locateMainSources();
        String keys = Files.readString(root.resolve("client/keybind/CultivationKeybinds.java"));
        String terminal = Files.readString(root.resolve("client/screen/AbstractTerminalScreen.java"));
        assertTrue(keys.contains("ScreenEvent.KeyPressed.Pre"));
        assertTrue(keys.contains("OPEN_STORAGE.matches(event.getKeyCode(), event.getScanCode())"));
        assertTrue(keys.contains("TerminalReturnNavigation.arm(returnScreen)"));
        assertTrue(terminal.contains("TerminalReturnNavigation.take()"));
        assertTrue(terminal.contains("this.minecraft.setScreen(returnScreen)"));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("src", "main", "java", "com", "cultivation", "cultivation"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate main sources");
    }
}
