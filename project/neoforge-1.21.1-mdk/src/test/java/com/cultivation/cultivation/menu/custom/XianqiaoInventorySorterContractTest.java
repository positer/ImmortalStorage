package com.cultivation.cultivation.menu.custom;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XianqiaoInventorySorterContractTest {
    @Test
    void playerSlotsRemainDirectVanillaInventorySlotsForSorterRecognition() throws Exception {
        Path root = locateMainSourceRoot();
        String menu = Files.readString(root.resolve(
                "java/com/cultivation/cultivation/menu/custom/XianqiaoStorageMenu.java"));
        String screen = Files.readString(root.resolve(
                "java/com/cultivation/cultivation/client/screen/XianqiaoStorageScreen.java"));

        assertTrue(menu.contains("new Slot(inv, column + row * 9 + 9"));
        assertTrue(menu.contains("new Slot(inv, column,"));
        assertTrue(menu.contains("public static final int PLAYER_START"));
        assertFalse(screen.contains("GLFW_KEY_R"));
        assertFalse(screen.contains("keyPressed(int keyCode"));
    }

    private static Path locateMainSourceRoot() {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (cursor != null) {
            Path direct = cursor.resolve("src/main");
            if (Files.isRegularFile(direct.resolve("java/com/cultivation/cultivation/CultivationMod.java"))) return direct;
            Path nested = cursor.resolve("project/neoforge-1.21.1-mdk/src/main");
            if (Files.isRegularFile(nested.resolve("java/com/cultivation/cultivation/CultivationMod.java"))) return nested;
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Unable to locate Cultivation src/main");
    }
}
