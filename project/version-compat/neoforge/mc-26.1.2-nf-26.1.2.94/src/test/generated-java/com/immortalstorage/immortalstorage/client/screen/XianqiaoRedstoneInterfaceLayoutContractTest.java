package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class XianqiaoRedstoneInterfaceLayoutContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test void screenRemainsAnExactTwoRowCompactLayout() throws Exception {
        Path source = locateMain().resolve("java/com/immortalstorage/immortalstorage/client/screen/XianqiaoRedstoneInterfaceScreen.java");
        String text = Files.readString(source);
        assertTrue(text.contains("imageHeight = 147"));
        assertTrue(text.contains("FIRST_ROW_Y = 8"));
        assertTrue(text.contains("SECOND_ROW_Y = 30"));
        assertTrue(text.contains(".bounds(leftPos + SLOT_X, topPos + SECOND_ROW_Y, 18, 18)"));
        assertTrue(text.contains("0xFFD03030 : 0xFF30A050"));
        assertTrue(text.contains("graphics.fill(x, y + 16, x + 18, y + 18, color)"));
        assertTrue(text.contains("xianqiao_redstone_interface.above"));
        assertTrue(text.contains("xianqiao_redstone_interface.below"));
        assertTrue(text.contains("renderLabels"));
        assertTrue(text.contains("openExternalResourceDialog"));
        assertTrue(text.contains("mouseScrolled"));
        assertTrue(text.contains("ConfigureXianqiaoRedstoneExternalTarget"));
        assertTrue(text.contains("configurationSynchronized()"));
        assertTrue(text.contains("GLFW.GLFW_KEY_ENTER"));
        assertTrue(text.contains("GLFW.GLFW_KEY_KP_ENTER"));
        assertTrue(text.contains("if (apply()) setFocused(null)"));
        assertTrue(text.contains("playerInventoryTitle"));
        assertTrue(text.contains("PLAYER_INVENTORY_Y"));
        assertTrue(text.contains("HOTBAR_Y"));
    }

    private static Path locateMain() {
        for (Path cursor = Path.of("").toAbsolutePath(); cursor != null; cursor = cursor.getParent()) {
            Path workspace = cursor.resolve("project/neoforge-1.21.1-mdk/src/main");
            if (Files.isDirectory(workspace)) return workspace;
            Path module = cursor.resolve("src/main");
            if (Files.isDirectory(module)) return module;
        }
        throw new IllegalStateException("Could not locate main source root");
    }
}
