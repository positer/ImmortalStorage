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

    @Test
    void specialOperationUsesRebindableGraveAccentMapping() throws Exception {
        Path root = locateMainSources();
        String keys = Files.readString(root.resolve("client/keybind/CultivationKeybinds.java"));
        String preview = Files.readString(root.resolve("client/render/SpiritStaffBuildPreview.java"));
        assertTrue(keys.contains("SPECIAL_OPERATION = new KeyMapping("));
        assertTrue(keys.contains("GLFW.GLFW_KEY_GRAVE_ACCENT"));
        assertTrue(preview.contains("CultivationKeybinds.SPECIAL_OPERATION.isDown()"));
        assertTrue(preview.contains("SpiritSwordFurnaceOperation.STORE"));
        assertTrue(preview.contains("SpiritSwordFurnaceOperation.SUMMON"));
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
