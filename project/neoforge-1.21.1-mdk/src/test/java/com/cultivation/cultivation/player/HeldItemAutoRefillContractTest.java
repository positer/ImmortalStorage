package com.cultivation.cultivation.player;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class HeldItemAutoRefillContractTest {
    @Test
    void exhaustedSelectedStackUsesExactComponentsAndManagementToggle() throws Exception {
        Path root = locate();
        String refill = Files.readString(root.resolve("player/HeldItemAutoRefill.java"));
        String data = Files.readString(root.resolve("player/CultivationPlayerData.java"));
        String menu = Files.readString(root.resolve("menu/custom/XianqiaoStorageMenu.java"));
        assertTrue(refill.contains("previous.template()"));
        assertTrue(refill.contains("data.extractStack(previous.template(), amount)"));
        assertTrue(refill.contains("previous.selectedSlot() == player.getInventory().selected"));
        assertTrue(data.contains("handAutoRefill"));
        assertTrue(menu.contains("HAND_AUTO_REFILL_BUTTON"));
    }

    private static Path locate() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("src", "main", "java", "com", "cultivation", "cultivation"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate main sources");
    }
}
