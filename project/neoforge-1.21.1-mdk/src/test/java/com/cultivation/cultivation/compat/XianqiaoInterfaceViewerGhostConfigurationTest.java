package com.cultivation.cultivation.compat;

import com.cultivation.cultivation.client.screen.XianqiaoInterfaceViewerConfiguration;
import net.minecraft.client.renderer.Rect2i;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source-boundary contract for optional recipe-viewer ghost configuration. */
final class XianqiaoInterfaceViewerGhostConfigurationTest {
    @Test
    void neutralTargetBoundaryAcceptsNineConfigurationSlotsAndRejectsFirstCacheSlot() {
        for (int slot = 0; slot < 9; slot++) {
            int configurationSlot = slot;
            assertDoesNotThrow(() -> new XianqiaoInterfaceViewerConfiguration.Target(
                    configurationSlot, new Rect2i(0, 0, 16, 16)));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new XianqiaoInterfaceViewerConfiguration.Target(
                        9, new Rect2i(0, 0, 16, 16)));
    }

    @Test
    void jeiRegistersOnlyTheNineMixedConfigurationSlots() throws IOException {
        Path java = locateMainSources();
        String plugin = read(java, "compat/jei/CultivationJeiPlugin.java");
        String handler = read(java, "compat/jei/XianqiaoInterfaceJeiGhostHandler.java");
        String lookup = read(java, "compat/jei/XianqiaoInterfaceJeiGuiHandler.java");

        assertTrue(plugin.contains("addGhostIngredientHandler(XianqiaoInterfaceScreen.class"));
        assertTrue(plugin.contains("addGuiContainerHandler(XianqiaoInterfaceScreen.class"),
                "JEI R/U lookup must cover interface resources independently of ghost targets");
        assertTrue(handler.contains("VanillaTypes.ITEM_STACK"), "JEI item ghosts must be accepted");
        assertTrue(handler.contains("DEFAULT_ITEM_AMOUNT"),
                "JEI item ghosts must default to one item");
        assertTrue(handler.contains("NeoForgeTypes.FLUID_STACK"), "JEI fluid ghosts must be accepted");
        assertTrue(handler.contains("DEFAULT_FLUID_AMOUNT_MB"),
                "JEI fluid ghosts must default to one bucket");
        assertTrue(handler.contains("XianqiaoInterfaceViewerConfiguration.targets(screen)"));
        assertFalse(handler.contains("BUFFER_START"), "real cache slots must never be JEI ghost targets");
        assertTrue(lookup.contains("XianqiaoInterfaceMenu.PLAYER_START"),
                "JEI lookup covers both the nine configuration and nine cache slots");
        assertTrue(lookup.contains("cultivation$getFluidAt"),
                "JEI lookup must return fluid identities instead of their bucket display markers");
    }

    @Test
    void emiRegistersOnlyTheNineMixedConfigurationSlots() throws IOException {
        Path java = locateMainSources();
        String plugin = read(java, "compat/emi/CultivationEmiPlugin.java");
        String handler = read(java, "compat/emi/XianqiaoInterfaceEmiGhostHandler.java");

        assertTrue(plugin.contains("addDragDropHandler(XianqiaoInterfaceScreen.class"));
        assertTrue(plugin.contains("addStackProvider(XianqiaoInterfaceScreen.class"),
                "EMI R/U lookup must cover interface resources independently of ghost targets");
        assertTrue(handler.contains("getItemStack()"), "EMI item ghosts must be accepted");
        assertTrue(handler.contains("DEFAULT_ITEM_AMOUNT"),
                "EMI item ghosts must default to one item");
        assertTrue(handler.contains("getKeyOfType(Fluid.class)"), "EMI fluid ghosts must be accepted");
        assertTrue(handler.contains("DEFAULT_FLUID_AMOUNT_MB"),
                "EMI fluid ghosts must default to one bucket");
        assertTrue(handler.contains("XianqiaoInterfaceViewerConfiguration.targets(screen)"));
        assertFalse(handler.contains("BUFFER_START"), "real cache slots must never be EMI ghost targets");
        assertTrue(plugin.contains("XianqiaoInterfaceMenu.PLAYER_START"),
                "EMI lookup covers both the nine configuration and nine cache slots");
        assertTrue(plugin.contains("cultivation$getFluidAt"),
                "EMI lookup must return fluid identities instead of their bucket display markers");
    }

    @Test
    void bothViewersUseOneRevisionCheckedServerTransactionBridge() throws IOException {
        Path java = locateMainSources();
        String bridge = read(java,
                "client/screen/XianqiaoInterfaceViewerConfiguration.java");

        assertTrue(bridge.contains("XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT"));
        assertTrue(bridge.contains("menu.containerId"));
        assertTrue(bridge.contains("menu.getConfigRevision()"));
        assertTrue(bridge.contains("getBlockPos()"));
        assertTrue(bridge.contains("SetXianqiaoInterfaceItemTarget"));
        assertTrue(bridge.contains("SetXianqiaoInterfaceFluidTarget"));
        assertTrue(bridge.contains("PacketDistributor.sendToServer"));
        assertTrue(bridge.contains("DEFAULT_ITEM_AMOUNT = 1L"),
                "viewer item drops must start at one item");
        assertTrue(bridge.contains("DEFAULT_FLUID_AMOUNT_MB = 1_000L"),
                "viewer fluid drops must start at exactly one bucket");
    }

    private static String read(Path java, String relative) throws IOException {
        return Files.readString(java.resolve(Path.of(relative)));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "src", "main", "java", "com", "cultivation", "cultivation"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate Cultivation main sources");
    }
}
