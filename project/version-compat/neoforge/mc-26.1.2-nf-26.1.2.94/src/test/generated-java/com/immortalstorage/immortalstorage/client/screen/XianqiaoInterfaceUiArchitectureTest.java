package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceUiArchitectureTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path MAIN = locateMainSources();

    @Test
    void fallbackScreenUsesRuntimeVanillaSlotSpritesAndNoOptionalModClasses() throws IOException {
        String screen = source("client", "screen", "XianqiaoInterfaceScreen.java");

        assertTrue(screen.contains("VanillaGuiPainter.slot"));
        assertTrue(screen.contains("VanillaGuiPainter.panel"));
        assertFalse(screen.contains("appeng."));
        assertFalse(screen.contains("pending"));
    }

    @Test
    void menuKeepsGhostConfigurationOutsideTheExposedItemHandlerSlots() throws IOException {
        String menu = source("menu", "custom", "XianqiaoInterfaceMenu.java");

        assertTrue(menu.contains("class ConfigurationSlot"));
        assertTrue(menu.contains("return false; // Ghost targets are never real inventory extraction slots."));
        assertTrue(menu.contains("class BufferOutputSlot"));
        assertTrue(menu.contains("bufferMirror.setItem"),
                "remote slot sync must not cast the backend to IItemHandlerModifiable");
        assertFalse(menu.contains("SlotItemHandler"));
        assertFalse(menu.contains("appeng."));
    }

    @Test
    void screenDoesNotThrottleOrSkipAnimationFrames() throws IOException {
        String screen = source("client", "screen", "XianqiaoInterfaceScreen.java");

        assertFalse(screen.contains("frameInterval"));
        assertFalse(screen.contains("frameCounter"));
        assertFalse(screen.contains("partialTick == 0"));
    }

    @Test
    void screenProvidesSixIndependentDirectionControlsAndDirectPerSlotAmountInput() throws IOException {
        String screen = source("client", "screen", "XianqiaoInterfaceScreen.java");
        String menu = source("menu", "custom", "XianqiaoInterfaceMenu.java");

        assertTrue(screen.contains("EditBox"),
                "the selected target amount must be directly typeable instead of inferred only from held count");
        assertTrue(screen.contains("SetXianqiaoInterfaceSideMode"));
        assertTrue(screen.contains("SetXianqiaoInterfaceTargetAmount"));
        assertTrue(screen.contains("SetXianqiaoInterfaceExternalTarget"));
        assertTrue(screen.contains("XianqiaoInterfaceMenu.DEFAULT_EXTERNAL_CACHE_AMOUNT"));
        assertTrue(screen.contains("FacePreviewButton"));
        assertTrue(screen.contains("adjacentBlockPreview"));
        assertTrue(screen.contains("openExternalResourceDialog"));
        assertTrue(screen.contains("refreshExternalResourceButtons"));
        assertTrue(screen.contains("SIDE_ORDER"),
                "all six physical faces need independent controls");
        assertTrue(menu.contains("getConfigRevision"),
                "configuration packets must carry the block-owned revision shown by the open menu");
        assertTrue(screen.contains("amountDialogOpen"));
        assertTrue(screen.contains("openAmountDialog"));
        assertTrue(screen.contains("lastConfigurationRevision"),
                "the modal must submit the revision captured for the exact configured identity");
        assertTrue(screen.contains("fluid_amount_hint"),
                "fluid targets show bucket units while the submitted value remains mB");
        assertTrue(screen.contains("movePointRight(3)"));
        assertTrue(screen.contains("validBucketInputShape"));
        assertTrue(screen.contains("renderSlotContents"),
                "item/fluid ghost and cache amounts must be rendered from synchronized long totals");
        assertTrue(screen.contains("menu.getItemTargetLimit()"));
        assertTrue(screen.contains("menu.getFluidTargetLimitMb()"));
        assertTrue(menu.contains("AMOUNT_HIGH_DATA_START"),
                "external-resource desired amounts must synchronize as full longs");
        assertTrue(menu.contains("CACHED_HIGH_DATA_START"),
                "external-resource cached amounts must synchronize as full longs");
    }

    @Test
    void emptyHandPrimaryClickClearsInsteadOfOpeningTheAmountDialog() throws IOException {
        String screen = source("client", "screen", "XianqiaoInterfaceScreen.java");
        String menu = source("menu", "custom", "XianqiaoInterfaceMenu.java");

        int primary = screen.indexOf("if (button == 0)");
        int normalMenuPath = screen.indexOf(
                "return super.mouseClicked(mouseX, mouseY, button);", primary);
        int secondary = screen.indexOf("if (button == 1)", normalMenuPath);
        int amountDialog = screen.indexOf("openAmountDialog(slot);", secondary);
        assertTrue(primary >= 0 && normalMenuPath > primary && secondary > normalMenuPath
                        && amountDialog > secondary,
                "empty-hand left click must reach the authoritative clear path before the modal");
        assertTrue(menu.contains("if (carried.isEmpty()) return backend.clearSlot(slot);"));
        assertTrue(menu.contains("configureTargetFromCarried(backend, slotId, getCarried(), button)"));
    }

    @Test
    void mixedResourcesRenderBeforeVanillaHoverAndFluidsOwnTheirTooltip() throws IOException {
        String screen = source("client", "screen", "XianqiaoInterfaceScreen.java");

        assertTrue(screen.contains("protected void renderSlotContents"));
        assertTrue(screen.contains("protected void renderSlot(GuiGraphicsExtractor graphics, Slot slot)"));
        assertTrue(screen.contains("XianqiaoInterfaceMenu.BUFFER_START"));
        assertTrue(screen.contains("!slot.getItem().isEmpty()"),
                "fluid cache rendering must explicitly cover vanilla's empty ItemStack slot path");
        assertTrue(screen.contains("slot.isFake()"));
        assertTrue(screen.contains("TerminalFluidAmountFormatter.exactBuckets"));
        assertTrue(screen.contains("TerminalFluidAmountFormatter.exactMillibuckets"));
        assertTrue(screen.contains("menu.getCarried().isEmpty() && fluidHover.isPresent()"),
                "fluid tooltip must follow vanilla's carried-stack suppression rule");
        assertFalse(screen.contains("renderConfiguredResources(graphics)"),
                "post-super slot redraw would cover vanilla hover and the floating carried stack");
    }

    private static String source(String... parts) throws IOException {
        return Files.readString(MAIN.resolve(Path.of("", parts)));
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
