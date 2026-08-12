package com.immortalstorage.immortalstorage.event;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StageEffectLifecycleContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void stageEffectsReconcileAcrossGameModeRespawnLoginAndDimensionChanges() throws IOException {
        String source = Files.readString(locateMainSources().resolve("event/CommonEvents.java"));

        assertTrue(source.contains("onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent"));
        assertTrue(source.contains("com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(p.level()).execute(() -> restoreStageEffects(p))"));
        assertTrue(source.contains("onPlayerRespawn(PlayerEvent.PlayerRespawnEvent"));
        assertTrue(source.contains("onPlayerJoin(PlayerEvent.PlayerLoggedInEvent"));
        assertTrue(source.contains("onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent"));
        assertTrue(source.contains("if (data.isTribulationActive())"));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate main sources");
    }
}
