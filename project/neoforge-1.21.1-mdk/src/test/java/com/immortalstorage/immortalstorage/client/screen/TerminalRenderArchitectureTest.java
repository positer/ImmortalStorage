package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalRenderArchitectureTest {
    private static final Path MAIN = locateMainSources();
    private static final Path SCREENS = MAIN.resolve(Path.of("client", "screen"));

    @Test
    void terminalUsesStableSlotIndicesInsteadOfPerFrameLinearSearches() throws IOException {
        String screen = Files.readString(SCREENS.resolve("AbstractTerminalScreen.java"));

        assertTrue(screen.contains("IdentityHashMap<Slot, Integer>"));
        assertFalse(screen.contains("this.menu.slots.indexOf(slot)"));
        assertTrue(screen.contains("intersectingBufferedRows"));
    }

    @Test
    void itemAndFluidPassesIterateTheIntersectingWindowInsteadOfTheDoubleBuffer() throws IOException {
        String terminal = Files.readString(SCREENS.resolve("AbstractTerminalScreen.java"));
        String xianqiao = Files.readString(SCREENS.resolve("XianqiaoStorageScreen.java"));

        assertTrue(methodBody(terminal, "protected final void renderStorageSlotsClipped")
                .contains("visibleBufferedRows()"));
        assertTrue(methodBody(xianqiao, "private void renderFluidStorage")
                .contains("visibleBufferedRows()"));
        assertFalse(methodBody(xianqiao, "private FluidCell fluidCellAt")
                .contains("for (int index"));
    }

    @Test
    void optimizationKeepsThePerFrameScrollAnimation() throws IOException {
        String screen = Files.readString(SCREENS.resolve("AbstractTerminalScreen.java"));

        assertTrue(methodBody(screen, "protected final void renderTerminalChrome")
                .contains("tickScrollAnimation()"));
        assertTrue(methodBody(screen, "private void tickScrollAnimation")
                .contains("Mth.lerp"));
        assertFalse(screen.contains("frameInterval"));
        assertFalse(screen.contains("frameCounter"));
        assertFalse(screen.contains("partialTick == 0"));
        assertFalse(screen.contains("Thread.sleep"));
        assertTrue(methodBody(screen, "protected void renderSlot")
                .contains("if (!shouldRenderMenuSlot(menuIndex))"));
        assertTrue(methodBody(screen, "protected void renderSlot")
                .contains("intersectsStorageViewport"));
    }

    @Test
    void onlyPartiallyVisibleStorageRowsPayTheScissorStateCost() throws IOException {
        String terminal = Files.readString(SCREENS.resolve("AbstractTerminalScreen.java"));
        String xianqiao = Files.readString(SCREENS.resolve("XianqiaoStorageScreen.java"));

        assertTrue(methodBody(terminal, "protected final void renderStorageSlotsClipped")
                .contains("fractionalScrollOffset() != 0"));
        assertTrue(methodBody(terminal, "protected void renderSlot")
                .contains("storageCellRequiresScissor"));
        assertTrue(methodBody(terminal, "protected void renderSlotHighlight")
                .contains("storageCellRequiresScissor"));
        assertTrue(methodBody(terminal, "protected final void renderStorageAmountOverlays")
                .contains("fractionalScrollOffset() != 0"));
        assertTrue(terminal.contains("protected final boolean storageCellRequiresScissor"));
    }

    @Test
    void storageAmountsRenderInAForegroundPassAfterAllItemModels() throws IOException {
        String terminal = Files.readString(SCREENS.resolve("AbstractTerminalScreen.java"));
        String kongqiao = Files.readString(SCREENS.resolve("KongqiaoScreen.java"));
        String xianqiao = Files.readString(SCREENS.resolve("XianqiaoStorageScreen.java"));

        String foregroundPass = methodBody(terminal,
                "protected final void renderStorageAmountOverlays");
        assertTrue(foregroundPass.contains("graphics.flush()"),
                "queued item-model buffers must be committed before amount text");
        assertTrue(foregroundPass.contains("TerminalLayout.STORAGE_AMOUNT_Z"));
        assertTrue(foregroundPass.contains("visibleBufferedRows()"));

        String kongqiaoRender = methodBody(kongqiao, "public void render(");
        String kongqiaoLabels = methodBody(kongqiao, "protected void renderLabels");
        assertFalse(kongqiaoRender.contains("renderStorageAmountOverlays("),
                "the amount pass must stay below the carried-item layer");
        assertOrdered(kongqiaoLabels, "playerInventoryTitle", "renderStorageAmountOverlays(");

        String xianqiaoRender = methodBody(xianqiao, "public void render(");
        String xianqiaoLabels = methodBody(xianqiao, "protected void renderLabels");
        assertFalse(xianqiaoRender.contains("renderStorageAmountOverlays("));
        assertFalse(xianqiaoRender.contains("renderFluidAmountOverlays("));
        assertOrdered(xianqiaoLabels, "playerInventoryTitle", "renderStorageAmountOverlays(");
        assertOrdered(xianqiaoLabels, "renderStorageAmountOverlays(", "renderFluidAmountOverlays(");
        assertFalse(methodBody(xianqiao, "protected void renderSlot")
                .contains("drawString"),
                "aggregated counts must not be submitted inside the item-model pass");
    }

    @Test
    void neoForgeContainerScreensDelegateTheSingleBackgroundPassToTheirSuperclass()
            throws IOException {
        for (String name : List.of(
                "KongqiaoScreen.java",
                "XianqiaoStorageScreen.java",
                "XianqiaoInterfaceScreen.java",
                "SourceVeinScreen.java",
                "ImmortalFurnaceScreen.java")) {
            String screen = Files.readString(SCREENS.resolve(name));
            String render = methodBody(screen, "public void render(");
            assertTrue(render.contains("super.render("), name);
            assertFalse(render.contains("renderBackground("),
                    name + " must not repeat NeoForge AbstractContainerScreen's background pass");
        }
    }

    @Test
    void fullRecipeSourcesAreLoadedOnlyWhileTheCraftModuleNeedsThem() throws IOException {
        String menu = Files.readString(MAIN.resolve(Path.of(
                "menu", "custom", "XianqiaoStorageMenu.java")));

        assertTrue(methodBody(menu, "public boolean shouldSendRecipeSources")
                .contains("activeModule == 0"));
        assertTrue(methodBody(menu, "public void broadcastChanges")
                .contains("activeModule == 0 && catalog.revision() != lastRecipeSourcesSnapshotRevision"));
    }

    @Test
    void menuRenderingHitTestingAndViewerBoundsShareTheCraftingLayoutModel() throws IOException {
        String screen = Files.readString(SCREENS.resolve("AbstractTerminalScreen.java"));
        String painter = Files.readString(SCREENS.resolve("VanillaGuiPainter.java"));
        for (String menuName : List.of("KongqiaoMenu.java", "XianqiaoStorageMenu.java")) {
            String menu = Files.readString(MAIN.resolve(Path.of("menu", "custom", menuName)));
            assertTrue(menu.contains("TerminalCraftingLayout.inputX"), menuName);
            assertTrue(menu.contains("TerminalCraftingLayout.resultY"), menuName);
            assertFalse(menu.contains("private static final int CRAFT_RESULT_X"), menuName);
            assertFalse(menu.contains("private static final int CRAFT_RESULT_Y"), menuName);
        }
        assertTrue(screen.contains("TerminalLayout.craftResultSlotBounds"));
        assertTrue(methodBody(screen, "public Rect2i immortalstorage$getSlotBounds")
                .contains("visualSlotX"));
        assertTrue(methodBody(screen, "public Rect2i immortalstorage$getSlotBounds")
                .contains("visualSlotY"));
        assertTrue(painter.contains("TerminalLayout.CRAFT_RESULT_FRAME_MARGIN"));
        assertTrue(painter.contains("119.0F, 30.0F, 26, 26, 256, 256"));
    }

    @Test
    void terminalOwnsNormalStorageClicksBeforeVanillaOrRecipeViewerFallbacks() throws IOException {
        String screen = Files.readString(SCREENS.resolve("AbstractTerminalScreen.java"));
        String mouseClicked = methodBody(screen, "public boolean mouseClicked(");
        String entryAction = methodBody(screen, "private void sendXianqiaoEntryAction");

        assertTrue(mouseClicked.indexOf("immortalstorage$getSlotAt")
                        < mouseClicked.indexOf("super.mouseClicked"),
                "visible terminal cells must be resolved before any fallback input handler");
        assertTrue(mouseClicked.contains("sendXianqiaoEntryAction"));
        assertTrue(mouseClicked.contains("return true;"));
        assertTrue(entryAction.contains("button == 1 ? TerminalAction.PICKUP_ONE : TerminalAction.PICKUP_STACK"));
        assertTrue(entryAction.contains("TerminalAction.QUICK_MOVE_TO_PLAYER"));
        assertTrue(entryAction.contains("button == 1 ? TerminalAction.INSERT_ONE : TerminalAction.INSERT_CARRIED"));
    }

    @Test
    void clippedOrNonVisibleStoragePixelsNeverResolveToClickableCells() throws IOException {
        String screen = Files.readString(SCREENS.resolve("AbstractTerminalScreen.java"));
        String hitTest = methodBody(screen, "protected final StorageViewCell storageCellAt");

        assertTrue(hitTest.contains("TerminalLayout.containsHalfOpen(viewport, mouseX, mouseY)"));
        assertTrue(hitTest.contains("visibleBufferedRows().contains(bufferedRow)"));
        assertTrue(hitTest.contains("!slot.isActive() || !shouldRenderMenuSlot(menuIndex)"));
        assertTrue(hitTest.contains("TerminalLayout.containsHalfOpen(bounds, mouseX, mouseY)"));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "src", "main", "java", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage main sources from "
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

    private static void assertOrdered(String source, String first, String second) {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        assertTrue(firstIndex >= 0, "missing first marker: " + first);
        assertTrue(secondIndex > firstIndex,
                "expected " + second + " after " + first);
    }
}
