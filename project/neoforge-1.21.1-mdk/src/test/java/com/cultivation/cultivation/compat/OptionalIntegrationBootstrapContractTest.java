package com.cultivation.cultivation.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the always-loaded/optional-mod boundary before runtime matrix tests. */
final class OptionalIntegrationBootstrapContractTest {
    private static final Path PROJECT = locateProject();
    private static final Path JAVA = PROJECT.resolve(Path.of("src", "main", "java"));

    @Test
    void managerUsesTheGoalModIdsAndDeclaresEveryRequiredGate() throws IOException {
        String manager = managerSource();
        assertTrue(manager.contains("modPresent(\"industrialforegoingsouls\")"));
        assertTrue(manager.contains("modPresent(\"irons_spellbooks\")"));
        assertTrue(manager.contains("modPresent(\"fluxnetworks\")"));
        assertFalse(manager.contains("modPresent(\"soulsweapons\")"));
    }

    @Test
    void presentBotaniaRegistersItsCapabilityLifecycleAndLiveOwnerBridge() throws IOException {
        String manager = managerSource();
        assertTrue(manager.contains("if (BOTANIA_LOADED)"));
        assertTrue(manager.contains("registerBotaniaCapabilities"));
        assertTrue(manager.contains("installBotaniaBridge"));
        assertTrue(manager.contains("ExternalResourceChannels.BOTANIA_MANA"));
        assertTrue(manager.contains("resolveExternalResourceStore"));
        assertTrue(manager.contains("SideMode.PUSH"));

        String endpoint = Files.readString(JAVA.resolve(Path.of(
                "com", "cultivation", "cultivation", "block", "entity",
                "XianqiaoInterfaceBlockEntity.java")));
        assertTrue(endpoint.contains("getPlayerList().getPlayer(owner)"));
        assertTrue(endpoint.contains("getStage() < 8"));
    }

    @Test
    void alwaysLoadedManagerStillHasNoOptionalApiTypes() throws IOException {
        String manager = managerSource();
        assertFalse(manager.contains("vazkii.botania"));
        assertFalse(manager.contains("mekanism.api"));
        assertFalse(manager.contains("appeng.api"));
        assertFalse(manager.contains("com.refinedmods"));
    }

    private static String managerSource() throws IOException {
        return Files.readString(JAVA.resolve(Path.of(
                "com", "cultivation", "cultivation", "compat", "CompatManager.java")));
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
