package com.immortalstorage.immortalstorage.client.keybind;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class GlobalStorageKeyTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void storageKeyUsesConfiguredMappingAcrossScreensAndTerminalReturns() throws Exception {
        Path root = locateMainSources();
        String keys = Files.readString(root.resolve("client/keybind/ImmortalStorageKeybinds.java"));
        String terminal = Files.readString(root.resolve("client/screen/AbstractTerminalScreen.java"));
        assertTrue(keys.contains("ScreenEvent.KeyPressed.Pre"));
        assertTrue(keys.contains("OPEN_STORAGE.matches(new net.minecraft.client.input.KeyEvent(event.getKeyCode(), event.getScanCode(), 0))"));
        assertTrue(keys.contains("TerminalReturnNavigation.arm(returnScreen)"));
        assertTrue(terminal.contains("TerminalReturnNavigation.take()"));
        assertTrue(terminal.contains("this.minecraft.setScreen(returnScreen)"));
    }

    @Test
    void specialOperationUsesRebindableGraveAccentMapping() throws Exception {
        Path root = locateMainSources();
        String keys = Files.readString(root.resolve("client/keybind/ImmortalStorageKeybinds.java"));
        String preview = Files.readString(root.resolve("client/render/SpiritStaffBuildPreview.java"));
        assertTrue(keys.contains("SPECIAL_OPERATION = new KeyMapping("));
        assertTrue(keys.contains("GLFW.GLFW_KEY_GRAVE_ACCENT"));
        assertTrue(preview.contains("ImmortalStorageKeybinds.SPECIAL_OPERATION.isDown()"));
        assertTrue(preview.contains("SpiritSwordFurnaceOperation.STORE"));
        assertTrue(preview.contains("SpiritSwordFurnaceOperation.SUMMON"));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate main sources");
    }
}
