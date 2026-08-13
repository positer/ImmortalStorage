package com.immortalstorage.immortalstorage.compat.ae2;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class Ae2InstalledAddonRetreatContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void installedAddonKeysAreCanonicalAndImmortalStorageRemainsFallback() throws IOException {
        Path root = locateProject().resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com",
                "immortalstorage", "immortalstorage", "compat", "ae2"));
        String addons = Files.readString(root.resolve("InstalledAddonExternalKeyBridges.java"));
        String fallback = Files.readString(root.resolve("ImmortalStorageExternalResourceKeyBridge.java"));
        String init = Files.readString(root.resolve("Ae2Compat.java"));
        String compatManager = Files.readString(root.getParent().resolve("CompatManager.java"));

        assertTrue(addons.contains("\"appflux\""));
        assertTrue(addons.contains("\"appmek\""));
        assertTrue(addons.contains("\"appbot\""));
        assertTrue(addons.contains("\"arseng\""));
        assertTrue(addons.contains("return 100;"));
        assertTrue(fallback.contains("return Integer.MIN_VALUE;"));
        int keyRegistration = init.indexOf("registerExternalResourceKeyType() {");
        int initialization = init.indexOf("public static synchronized void initialize() {");
        int addonRegistration = init.indexOf("InstalledAddonExternalKeyBridges.registerPresent()");
        assertTrue(addonRegistration > initialization);
        assertTrue(addonRegistration > keyRegistration,
                "addon keys must be resolved after registries bind, not during block registration");
        assertTrue(init.contains("AECapabilities.ME_STORAGE"));
        assertTrue(init.contains("ModBlockEntities.XIANQIAO_MANAGER.get()"));
        assertTrue(init.contains("storage.setActive(true)"));
        assertTrue(compatManager.contains("modBus.addListener(CompatManager::registerAe2Capabilities)"));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))
                    && Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project root");
    }
}
