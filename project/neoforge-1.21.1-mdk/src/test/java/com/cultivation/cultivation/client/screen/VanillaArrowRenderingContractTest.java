package com.cultivation.cultivation.client.screen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VanillaArrowRenderingContractTest {
    private static final Path SCREEN_SOURCES = locateMainSources().resolve("client/screen");

    @Test
    void terminalUsesVanillaCraftingAndFurnaceArrows() throws IOException {
        String source = Files.readString(SCREEN_SOURCES.resolve("VanillaGuiPainter.java"));
        String furnaceModule = methodBody(source, "static void terminalFurnaceModule");
        String furnaceFlame = methodBody(source, "static void furnaceFlame");

        assertTrue(source.contains("textures/gui/container/crafting_table.png"));
        assertTrue(source.contains("90.0F, 35.0F, 22, 15, 256, 256"));
        assertTrue(source.contains("textures/gui/container/furnace.png"));
        assertTrue(source.contains("79.0F, 34.0F, 24, 16, 256, 256"));
        assertTrue(source.contains("container/furnace/burn_progress"));
        assertTrue(source.contains("container/immortal_furnace/lit_progress"));
        assertTrue(furnaceModule.contains("furnaceFlame("));
        assertTrue(furnaceFlame.contains("56.0F, 36.0F, 14, 14, 256, 256"));
        assertTrue(furnaceFlame.contains("14 - height"));
        assertTrue(furnaceFlame.contains("y + 14 - height"));
        assertFalse(furnaceModule.contains("g.fill("));
        assertFalse(furnaceFlame.contains("g.fill("));
        assertFalse(source.contains("private static void arrow("));
        assertFalse(source.contains("private static void flame("));
        assertFalse(source.contains("container/furnace_burn_progress"));
    }

    @Test
    void terminalUsesTheExactVanillaCraftingResultRecessWithoutANestedSlotFrame() throws IOException {
        String source = Files.readString(SCREEN_SOURCES.resolve("VanillaGuiPainter.java"));
        String terminalPanel = methodBody(source, "static void terminalPanel");

        assertTrue(source.contains("119.0F, 30.0F, 26, 26, 256, 256"));
        assertTrue(terminalPanel.contains("craftingResultSlot("));
        assertFalse(terminalPanel.contains("vanillaInset(g, x + TerminalLayout.CRAFT_RESULT_X"));
        assertFalse(terminalPanel.contains("slot(g, x + TerminalLayout.CRAFT_RESULT_X"));
    }

    @Test
    void standaloneImmortalFurnaceHasBaseArrowsAndValidProgressSprites() throws IOException {
        String source = Files.readString(SCREEN_SOURCES.resolve("ImmortalFurnaceScreen.java"));
        String background = methodBody(source, "protected void renderBg");

        assertTrue(source.contains("textures/gui/container/furnace.png"));
        assertTrue(source.contains("79.0F, 34.0F, 24, 16, 256, 256"));
        assertTrue(source.contains("container/furnace/burn_progress"));
        assertTrue(background.contains("VanillaGuiPainter.furnaceFlame("));
        assertFalse(background.contains("graphics.fill(x + 21"));
        assertFalse(background.contains("graphics.fill(x + 19"));
        assertFalse(background.contains("graphics.fill(x + 22"));
        assertFalse(source.contains("container/furnace_lit_progress"));
        assertFalse(source.contains("container/furnace_burn_progress"));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "src", "main", "java", "com", "cultivation", "cultivation"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate Cultivation main sources from "
                + Path.of("").toAbsolutePath());
    }

    private static String methodBody(String source, String signature) {
        int name = source.indexOf(signature);
        if (name < 0) return "";
        int opening = source.indexOf('{', name);
        if (opening < 0) return "";
        int depth = 0;
        for (int index = opening; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') depth++;
            if (current == '}' && --depth == 0) return source.substring(opening, index + 1);
        }
        return "";
    }
}
