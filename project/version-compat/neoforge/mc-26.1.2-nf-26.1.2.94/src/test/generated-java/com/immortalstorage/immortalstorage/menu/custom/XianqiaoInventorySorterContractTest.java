package com.immortalstorage.immortalstorage.menu.custom;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XianqiaoInventorySorterContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void playerSlotsRemainDirectVanillaInventorySlotsForSorterRecognition() throws Exception {
        Path root = locateMainSourceRoot();
        String menu = Files.readString(root.resolve(
                "java/com/immortalstorage/immortalstorage/menu/custom/XianqiaoStorageMenu.java"));
        String screen = Files.readString(root.resolve(
                "java/com/immortalstorage/immortalstorage/client/screen/XianqiaoStorageScreen.java"));

        assertTrue(menu.contains("new Slot(inv, column + row * 9 + 9"));
        assertTrue(menu.contains("new Slot(inv, column,"));
        assertTrue(menu.contains("public static final int PLAYER_START"));
        assertFalse(screen.contains("GLFW_KEY_R"));
        assertFalse(screen.contains("keyPressed(int keyCode"));
    }

    @Test
    void inventoryActionsAreCenteredInTheGapAboveThePlayerInventory() throws Exception {
        Path root = locateMainSourceRoot();
        String screen = Files.readString(root.resolve(
                "java/com/immortalstorage/immortalstorage/client/screen/XianqiaoStorageScreen.java"));

        assertTrue(screen.contains("TerminalLayout.inventoryY(this.imageHeight)"));
        assertTrue(screen.contains("(TerminalLayout.SLOT_SIZE + TerminalInventoryActionButton.SIZE) / 2"),
                "the three 8x8 action icons should be centered in the storage/inventory gap");
        assertFalse(screen.contains("this.topPos + this.imageHeight - 106"),
                "the old position overlaps the final storage row");
    }

    private static Path locateMainSourceRoot() {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (cursor != null) {
            Path direct = cursor.resolve("src/main");
            if (Files.isRegularFile(direct.resolve("java/com/immortalstorage/immortalstorage/ImmortalStorageMod.java"))) return direct;
            Path nested = cursor.resolve("project/neoforge-1.21.1-mdk/src/main");
            if (Files.isRegularFile(nested.resolve("java/com/immortalstorage/immortalstorage/ImmortalStorageMod.java"))) return nested;
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Unable to locate ImmortalStorage src/main");
    }
}
