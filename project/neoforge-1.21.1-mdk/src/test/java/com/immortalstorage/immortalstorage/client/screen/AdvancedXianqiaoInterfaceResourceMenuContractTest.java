package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdvancedXianqiaoInterfaceResourceMenuContractTest {
    @Test
    void advancedCacheConfigurationKeepsTheBuiltInFeResourceMenu() throws IOException {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("src/main/java"))) {
            root = root.getParent();
        }
        if (root == null) throw new IllegalStateException("cannot locate project root");

        String screen = Files.readString(root.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/client/screen/"
                        + "AdvancedXianqiaoInterfaceScreen.java"));
        String menu = Files.readString(root.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/menu/custom/"
                        + "XianqiaoInterfaceMenu.java"));
        String catalog = Files.readString(root.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/compat/"
                        + "ExternalResourceCatalog.java"));
        String compat = Files.readString(root.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/compat/CompatManager.java"));

        assertTrue(screen.contains("AdvancedXianqiaoInterfaceMenu.CONFIG_Y"));
        assertTrue(screen.contains("tryOpenExternalResourceDialog(slot, button)"));
        assertTrue(screen.contains("openExternalResourceDialog(slot)"));
        assertTrue(screen.contains("menu.isExternalTarget(slot)"),
                "an existing FE target must be replaceable without clearing the slot first");
        assertTrue(screen.contains("hasShiftDown()"),
                "shift-right-click must remain the explicit amount-editor gesture");
        assertTrue(menu.contains("ExternalResourceCatalog.available()"));
        assertTrue(catalog.contains("ExternalResourceChannels.FE"),
                "FE must remain available before optional integrations finish bootstrapping");
        assertTrue(compat.contains("availableBuiltinExternalResources"));
        assertTrue(compat.contains("if (BOTANIA_LOADED)"));
        assertTrue(compat.contains("if (ARS_NOUVEAU_LOADED)"));
    }
}
