package com.immortalstorage.immortalstorage.compat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AppliedBotanicsCapabilityOrderContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProjectRoot();

    @Test
    void compatibilityMixinIsGatedToAppliedBotanicsAndBotania() throws Exception {
        String plugin = Files.readString(PROJECT.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/mixin/appliedbotanics/"
                        + "AppliedBotanicsMixinConfigPlugin.java"));
        assertTrue(plugin.contains("getModFileById(\"appbot\")"));
        assertTrue(plugin.contains("getModFileById(\"botania\")"));
    }

    @Test
    void shimReusesBotaniaOfficialLookupsInsteadOfCreatingCapabilities() throws Exception {
        String mixin = Files.readString(PROJECT.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/mixin/appliedbotanics/"
                        + "BotaniaForgeCapabilitiesMixin.java"));
        assertTrue(mixin.contains("BotaniaForgeCapabilities.registerBlockApiLookup(id)"));
        assertTrue(mixin.contains("FOR_BLOCKS.containsKey(id)"));
        assertFalse(mixin.contains("BlockCapability.create("));
        assertFalse(mixin.contains("appbot."));
    }

    private static Path locateProjectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))
                    && Files.isDirectory(current.resolve("src/main"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate project root");
    }
}
