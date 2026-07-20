package com.immortalstorage.immortalstorage.compat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingGadgetsOptionalBoundaryTest {
    private static final Path ROOT = locateMainSourceRoot();

    @Test
    void optionalMixinIsLoaderGatedAndTargetsOnlyMaterialLookups() throws Exception {
        String config = Files.readString(ROOT.resolve("resources/immortalstorage.buildinggadgets.mixins.json"));
        String plugin = Files.readString(ROOT.resolve(
                "java/com/immortalstorage/immortalstorage/mixin/buildinggadgets/BuildingGadgetsMixinConfigPlugin.java"));
        String mixin = Files.readString(ROOT.resolve(
                "java/com/immortalstorage/immortalstorage/mixin/buildinggadgets/BuildingUtilsStorageMixin.java"));

        assertTrue(config.contains("BuildingGadgetsMixinConfigPlugin"));
        assertTrue(plugin.contains("getModFileById(\"buildinggadgets2\")"));
        assertTrue(mixin.contains("method = \"removeStacksFromInventory\""));
        assertTrue(mixin.contains("method = \"countItemStacks\""));
        assertFalse(mixin.contains("com.direwolf20.buildinggadgets2.common.items"));
    }

    @Test
    void bridgeRequiresHeldCopyPasteGadgetAndUsesTransactionalStorageCalls() throws Exception {
        String bridge = Files.readString(ROOT.resolve(
                "java/com/immortalstorage/immortalstorage/compat/buildinggadgets/BuildingGadgetsStorageBridge.java"));

        assertTrue(bridge.contains("gadget_copy_paste"));
        assertTrue(bridge.contains("holdsCopyPasteGadget(player)"));
        assertTrue(bridge.contains("TerminalStorageAction.EXECUTE"));
        assertTrue(bridge.contains("if (!simulate)"));
        assertTrue(bridge.contains("getKongqiaoItems()"));
        assertTrue(bridge.contains("getXianqiaoItemSummary()"));
    }

    private static Path locateMainSourceRoot() {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (cursor != null) {
            Path direct = cursor.resolve("src/main");
            if (Files.isRegularFile(direct.resolve("java/com/immortalstorage/immortalstorage/ImmortalStorageMod.java"))) {
                return direct;
            }
            Path nested = cursor.resolve("project/neoforge-1.21.1-mdk/src/main");
            if (Files.isRegularFile(nested.resolve("java/com/immortalstorage/immortalstorage/ImmortalStorageMod.java"))) {
                return nested;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Unable to locate ImmortalStorage src/main");
    }
}
