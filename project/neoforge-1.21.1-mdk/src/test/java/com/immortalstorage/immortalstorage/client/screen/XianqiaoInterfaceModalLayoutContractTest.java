package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression contracts for the interface amount modal and face-control layout. */
final class XianqiaoInterfaceModalLayoutContractTest {
    private static final Path MAIN = locateMainSources();

    @Test
    void amountModalOwnsTheTopRenderAndInputLayer() throws IOException {
        String screen = source("client", "screen", "XianqiaoInterfaceScreen.java");
        String viewerBridge = source("client", "screen",
                "XianqiaoInterfaceViewerConfiguration.java");
        String jeiLookup = source("compat", "jei", "XianqiaoInterfaceJeiGuiHandler.java");
        String emiPlugin = source("compat", "emi", "ImmortalStorageEmiPlugin.java");

        assertTrue(screen.contains("MODAL_Z"),
                "the dialog needs an explicit layer above item/count decoration depth");
        assertTrue(screen.contains("translate(0.0F, 0.0F, MODAL_Z)"));
        assertTrue(screen.contains("sideButton.visible = !amountDialogOpen"),
                "background face buttons must not render through the modal");
        assertTrue(screen.contains("public boolean mouseDragged"));
        assertTrue(screen.contains("public boolean mouseReleased"));
        assertTrue(screen.contains("public boolean mouseScrolled"));
        assertTrue(screen.contains("if (amountDialogOpen) return true;"),
                "non-dialog pointer input must be consumed while the modal is open");
        assertTrue(screen.contains("isAmountDialogOpen()"));
        assertTrue(viewerBridge.contains("screen.isAmountDialogOpen()"),
                "JEI/EMI ghost targets must disappear while the amount modal owns input");
        assertTrue(jeiLookup.contains("screen.isAmountDialogOpen()"),
                "JEI R/U lookup must not click through the modal");
        assertTrue(emiPlugin.contains("screen.isAmountDialogOpen()"),
                "EMI R/U lookup must not click through the modal");
    }

    @Test
    void allSixFaceControlsUseTheSameTwoHorizontalRowsAsTheSourceVein() throws IOException {
        String screen = source("client", "screen", "XianqiaoInterfaceScreen.java");
        String sourceScreen = source("client", "screen", "SourceVeinScreen.java");
        String menu = source("menu", "custom", "XianqiaoInterfaceMenu.java");

        assertTrue(screen.contains("SCREEN_WIDTH = 176"),
                "the two-row controls must retain the vanilla 176px container width");
        assertTrue(screen.contains("SCREEN_HEIGHT = 243"));
        assertTrue(screen.contains("SIDE_GRID_X = 56"));
        assertTrue(screen.contains("SIDE_GRID_Y = 97"),
                "the face grid belongs immediately below the output cache row");
        assertTrue(screen.contains("SIDE_BUTTON_WIDTH = 20"));
        assertTrue(screen.contains("SIDE_COLUMN_STRIDE = 22"));
        assertTrue(screen.contains("SIDE_ROW_STRIDE = 21"));
        assertTrue(screen.contains("(index % 3) * SIDE_COLUMN_STRIDE"));
        assertTrue(screen.contains("(index / 3) * SIDE_ROW_STRIDE"));
        assertTrue(screen.contains("Direction.UP, Direction.NORTH, Direction.DOWN"));
        assertTrue(screen.contains("Direction.WEST, Direction.SOUTH, Direction.EAST"));
        assertTrue(sourceScreen.contains("Direction.UP, Direction.NORTH, Direction.DOWN"));
        assertTrue(sourceScreen.contains("Direction.WEST, Direction.SOUTH, Direction.EAST"));
        assertTrue(sourceScreen.contains("FacePreviewButton"));
        assertTrue(sourceScreen.contains("(index % 3) * 22"));
        assertTrue(sourceScreen.contains("(index / 3) * 21"));
        assertFalse(screen.contains("index * SIDE_BUTTON_STRIDE"),
                "the six controls must not regress to one oversized row");
        assertTrue(menu.contains("PLAYER_INVENTORY_Y = 152"));
        assertTrue(menu.contains("HOTBAR_Y = 210"));
        assertTrue(screen.contains("this.inventoryLabelY = 139"));
        assertTrue(screen.contains("8, 86, TEXT, false"));
    }

    @Test
    void scaleThreeLayoutKeepsTheHotbarAboveEmiSearchAndEveryVerticalBandSeparate()
            throws IOException {
        String screen = source("client", "screen", "XianqiaoInterfaceScreen.java");
        String menu = source("menu", "custom", "XianqiaoInterfaceMenu.java");

        int framebufferHeight = 760;
        int guiScale = 3;
        int logicalHeight = (framebufferHeight + guiScale - 1) / guiScale;
        int screenHeight = intAssignment(screen, "SCREEN_HEIGHT");
        int top = (logicalHeight - screenHeight) / 2;
        int emiCenterSearchY = logicalHeight - 21;
        int sideGridY = intAssignment(screen, "SIDE_GRID_Y");
        int sideRowStride = intAssignment(screen, "SIDE_ROW_STRIDE");
        int inventoryLabelY = intAssignment(screen, "inventoryLabelY");
        int playerInventoryY = intAssignment(menu, "PLAYER_INVENTORY_Y");
        int hotbarY = intAssignment(menu, "HOTBAR_Y");

        int secondButtonRowTop = top + sideGridY + sideRowStride;
        int secondButtonRowBottomExclusive = secondButtonRowTop + 20;
        int inventoryLabelTop = top + inventoryLabelY;
        int inventoryLabelBottomExclusive = inventoryLabelTop + 9;
        int firstPlayerSlotSpriteTop = top + playerInventoryY - 1;
        int thirdPlayerRowSpriteBottomExclusive = top + playerInventoryY + 2 * 18 + 17;
        int hotbarSlotSpriteTop = top + hotbarY - 1;
        int hotbarSlotSpriteBottomExclusive = top + hotbarY + 17;

        assertEquals(254, logicalHeight);
        assertEquals(233, emiCenterSearchY);
        assertTrue(secondButtonRowBottomExclusive <= inventoryLabelTop,
                "the lower face-button row must end before the inventory label");
        assertTrue(inventoryLabelBottomExclusive <= firstPlayerSlotSpriteTop,
                "the inventory label must end before the first player slot sprite");
        assertTrue(thirdPlayerRowSpriteBottomExclusive <= hotbarSlotSpriteTop,
                "the third inventory row must not overlap the hotbar sprite");
        assertTrue(hotbarSlotSpriteBottomExclusive < emiCenterSearchY,
                "the complete vanilla hotbar slot sprite must stay above EMI's centered search field");
    }

    private static int intAssignment(String source, String name) {
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*=\\s*(\\d+)")
                .matcher(source);
        assertTrue(matcher.find(), () -> "missing integer assignment for " + name);
        return Integer.parseInt(matcher.group(1));
    }

    private static String source(String... parts) throws IOException {
        return Files.readString(MAIN.resolve(Path.of("", parts)));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "src", "main", "java", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage main sources");
    }
}
