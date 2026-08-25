package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalStonecutterLayoutContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path MAIN = locateMainSources();
    private static final Path SCREENS = MAIN.resolve(Path.of("client", "screen"));

    @Test
    void stonecutterKeepsTheVanillaCanvasFreeOfAnOversizedToggleButton() throws Exception {
        for (String name : List.of("KongqiaoScreen.java", "XianqiaoStorageScreen.java")) {
            String source = Files.readString(SCREENS.resolve(name));
            assertFalse(source.contains("stonecutterToggleButton"), name);
            assertFalse(source.contains(".bounds(this.leftPos + 30"), name);
            assertTrue(source.contains("stonecutter_title"), name);
            assertTrue(source.contains("selectSmithingModule()"), name);
            assertTrue(source.contains("this.smithingVisible || this.stonecutterVisible"), name);
        }
    }

    @Test
    void moduleTabConsumesClicksBeforeVanillaCarriedItemHandling() throws Exception {
        String source = Files.readString(SCREENS.resolve("KongqiaoScreen.java"));
        int tabGuard = source.indexOf("this.smithingModuleButton.isMouseOver(mouseX, mouseY)");
        int vanillaFallback = source.indexOf("return super.mouseClicked(mouseX, mouseY, button);");
        assertTrue(tabGuard >= 0);
        assertTrue(vanillaFallback > tabGuard);
        assertTrue(source.contains("selectSmithingModule();\n            return true;"));
    }

    @Test
    void sharedRendererUsesTheVanillaStonecutterGeometry() throws Exception {
        String gui = Files.readString(SCREENS.resolve("TerminalStonecutterGui.java"));
        String painter = Files.readString(SCREENS.resolve("VanillaGuiPainter.java"));
        assertTrue(gui.contains("left + 52"));
        assertTrue(gui.contains("left + 119"));
        assertTrue(gui.contains("GRID_TOP_OFFSET = 19"));
        assertTrue(gui.contains("COLUMNS = 4"));
        assertTrue(gui.contains("ROWS = 3"));
        assertTrue(painter.contains("slot(g, x + 20, y + slotY, true)"));
        assertTrue(painter.contains("slot(g, x + 143, y + slotY, true)"));
        assertTrue(painter.contains("vanillaInset(g, x + 52, y + gridTop, 66, 56)"));
    }

    @Test
    void targetStonecutterUsesTheSynchronizedSelectableRecipeSet() throws Exception {
        Path workspace = MAIN;
        while (workspace != null && !Files.isDirectory(workspace.resolve(Path.of("project", "version-compat")))) {
            workspace = workspace.getParent();
        }
        assertTrue(workspace != null);
        Path target = workspace.resolve(Path.of("project", "version-compat", "neoforge",
                "mc-26.1.2-nf-26.1.2.94", "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage",
                "immortalstorage", "menu", "custom", "EmbeddedStonecutterBackend.java"));
        if (!Files.isRegularFile(target)) return;
        String source = Files.readString(target);
        assertTrue(source.contains("SelectableRecipe.SingleInputSet<StonecutterRecipe>"));
        assertTrue(source.contains("level.recipeAccess().stonecutterRecipes().acceptsInput(stack)"));
        assertTrue(source.contains("level.recipeAccess().stonecutterRecipes().selectByInput(itemstack)"));
        assertFalse(source.contains("(net.minecraft.world.item.crafting.RecipeManager) level.recipeAccess()"));
        assertFalse(source.contains("if (!mayTake()) return;"),
                "the target backend must not reject onTake after vanilla clears the result slot");
        assertTrue(source.contains("isValidRecipeIndex(selectedRecipeIndex.get()) || input.getItem(INPUT).isEmpty()"));
        assertTrue(source.contains("setupResultSlot();"),
                "the target backend must regenerate the selected output while input remains");
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage main sources");
    }
}
