package com.immortalstorage.immortalstorage.compat.mc2612;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class TargetGuiCompatibilityContractTest {
    @Test
    void legacyTextureRectanglesAreConvertedToExtractorEndpoints() throws Exception {
        String source = Files.readString(sourceRoot().resolve("CompatGui.java"));
        assertTrue(source.contains("x, y, x + width, y + height"));
        assertTrue(source.contains("(u + width) / textureWidth"));
        assertTrue(source.contains("(v + height) / textureHeight"));
    }

    @Test
    void targetInputEventsReachLegacyScreenOverrides() throws Exception {
        String source = Files.readString(sourceRoot().resolve("CompatAbstractContainerScreen.java"));
        assertTrue(source.contains("mouseClicked(MouseButtonEvent event, boolean doubleClick)"));
        assertTrue(source.contains("return mouseClicked(event.x(), event.y(), event.button());"));
        assertTrue(source.contains("mouseDragged(MouseButtonEvent event, double dragX, double dragY)"));
        assertTrue(source.contains("mouseReleased(MouseButtonEvent event)"));
        assertTrue(source.contains("keyPressed(KeyEvent event)"));
        assertTrue(source.contains("charTyped(CharacterEvent event)"));
    }

    @Test
    void slotHighlightIsExtractedOnlyByTheOfficialContainerPipeline() throws Exception {
        String source = Files.readString(sourceRoot().resolve("CompatAbstractContainerScreen.java"));
        assertFalse(source.contains("super.extractContents(graphics, mouseX, mouseY, partialTick)"));
        assertTrue(source.contains("terminal.immortalstorage$getSlotBounds(slot)"));
        assertTrue(source.contains("extractCompatSlotHighlight(graphics, SLOT_HIGHLIGHT_BACK)"));
        assertTrue(source.contains("extractCompatSlotHighlight(graphics, SLOT_HIGHLIGHT_FRONT)"));
        assertFalse(source.contains("graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16"));
    }

    @Test
    void machineProgressUsesDirectTexturesInsteadOfPartialSpriteExtraction() throws Exception {
        Path screens = generatedScreenRoot();
        String painter = Files.readString(screens.resolve("VanillaGuiPainter.java"));
        assertTrue(painter.contains("textures/gui/sprites/container/furnace/burn_progress.png"));
        assertTrue(painter.contains("textures/gui/sprites/container/immortal_furnace/lit_progress.png"));
        assertTrue(painter.contains("static void furnaceProgress"));
        assertFalse(painter.contains("CompatGui.blitSprite(g, IMMORTAL_FURNACE_LIT_PROGRESS"));

        for (String screen : List.of("ImmortalFurnaceScreen.java",
                "SimulatedSpiritFieldScreen.java", "SimulatedReincarnationFurnaceScreen.java")) {
            String source = Files.readString(screens.resolve(screen));
            assertTrue(source.contains("VanillaGuiPainter.furnaceProgress"));
            assertFalse(source.contains("container/furnace/burn_progress"));
        }
    }

    @Test
    void targetFluidSlotsRenderAtlasSpritesInsteadOfBuckets() throws Exception {
        Path screens = generatedScreenRoot();
        for (String screen : List.of("XianqiaoStorageScreen.java", "XianqiaoInterfaceScreen.java",
                "AdvancedXianqiaoInterfaceScreen.java")) {
            String source = Files.readString(screens.resolve(screen));
            assertFalse(source.contains("fakeItem(stack.getFluidType().getBucket"));
            assertTrue(source.contains("getFluidStateModelSet()"));
            assertTrue(source.contains("fluidModel.stillMaterial().sprite()"));
            assertTrue(source.contains("fluidModel.fluidTintSource().colorAsStack(stack)"));
            assertTrue(source.contains("CompatGui.blitSprite"));
        }
    }

    private static Path sourceRoot() {
        Path current = Path.of("").toAbsolutePath();
        Path marker = Path.of("project", "version-compat", "neoforge",
                "mc-26.1.2-nf-26.1.2.94", "src", "main", "java",
                "com", "immortalstorage", "immortalstorage", "compat", "mc2612");
        while (current != null) {
            Path candidate = current.resolve(marker);
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate 26.1.2 compatibility source root");
    }

    private static Path generatedScreenRoot() {
        Path current = Path.of("").toAbsolutePath();
        Path marker = Path.of("project", "version-compat", "neoforge",
                "mc-26.1.2-nf-26.1.2.94", "src", "main", "generated-java",
                "com", "immortalstorage", "immortalstorage", "client", "screen");
        while (current != null) {
            Path candidate = current.resolve(marker);
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate 26.1.2 generated screen source root");
    }
}
