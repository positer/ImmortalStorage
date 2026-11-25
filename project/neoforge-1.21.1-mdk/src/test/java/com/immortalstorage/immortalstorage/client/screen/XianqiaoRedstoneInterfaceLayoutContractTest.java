package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.immortalstorage.menu.custom.XianqiaoRedstoneInterfaceMenu;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class XianqiaoRedstoneInterfaceLayoutContractTest {
    @Test void thresholdRowsDoNotOverlapMenuSlotsOrEachOther() throws Exception {
        Path source = locateMain().resolve("java/com/immortalstorage/immortalstorage/client/screen/XianqiaoRedstoneInterfaceScreen.java");
        String text = Files.readString(source);
        int first = XianqiaoRedstoneInterfaceMenu.CONFIG_Y;
        int second = constant(text, "SECOND_ROW_Y");
        int fieldX = constant(text, "FIELD_X");
        int fieldWidth = constant(text, "FIELD_WIDTH");
        int fieldHeight = constant(text, "FIELD_HEIGHT");
        int inputPadding = constant(text, "INPUT_PADDING");
        int width = constant(text, "imageWidth");
        assertTrue(text.contains("FIRST_ROW_Y = XianqiaoRedstoneInterfaceMenu.CONFIG_Y"));
        assertTrue(first - 10 >= 17, "Upper caption clears the header");
        assertTrue(first + 18 < second - 9, "Upper slot, field and lower caption do not touch");
        assertTrue(second + 18 < XianqiaoRedstoneInterfaceMenu.PLAYER_INVENTORY_Y - 12,
                "Polarity and threshold controls clear the inventory caption");
        assertTrue(fieldX > constant(text, "SLOT_X") + 18);
        assertTrue(fieldX + fieldWidth <= width - 8);
        assertTrue(fieldWidth - 2 * inputPadding >= 19 * 6, "Full signed-long positive values fit without scrolling");
        assertTrue(first + fieldHeight < second);
        assertTrue(text.contains(".bounds(leftPos + SLOT_X, topPos + SECOND_ROW_Y, 18, 18)"));
        assertTrue(text.contains("0xFF9A2828 : 0xFF1E6C38"));
        assertTrue(text.contains("box.setBordered(false)"));
        assertTrue(text.contains("drawThresholdLabels(graphics, FIRST_ROW_Y, \"above\""));
        assertTrue(text.contains("drawThresholdLabels(graphics, SECOND_ROW_Y, \"below\""));
        assertTrue(text.contains("ConfigurationStyle.header("));
        assertTrue(text.contains("addWidget(polarityButton)"));
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

    private static int constant(String source, String name) {
        var value = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*=\\s*(\\d+)\\s*;").matcher(source);
        assertTrue(value.find(), name);
        return Integer.parseInt(value.group(1));
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
