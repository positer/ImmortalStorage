package com.immortalstorage.immortalstorage.compat.ae2;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class Ae2InstalledAddonRetreatContractTest {
    @Test
    void installedAddonKeysAreCanonicalAndImmortalStorageRemainsFallback() throws IOException {
        Path root = locateProject().resolve(Path.of("src", "main", "java", "com",
                "immortalstorage", "immortalstorage", "compat", "ae2"));
        String addons = Files.readString(root.resolve("InstalledAddonExternalKeyBridges.java"));
        String fallback = Files.readString(root.resolve("ImmortalStorageExternalResourceKeyBridge.java"));
        String init = Files.readString(root.resolve("Ae2Compat.java"));

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
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))
                    && Files.isDirectory(current.resolve("src/main/java"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project root");
    }
}
