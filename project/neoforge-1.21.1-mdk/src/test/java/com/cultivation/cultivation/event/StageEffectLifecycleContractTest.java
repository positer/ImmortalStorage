package com.cultivation.cultivation.event;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StageEffectLifecycleContractTest {
    @Test
    void stageEffectsReconcileAcrossGameModeRespawnLoginAndDimensionChanges() throws IOException {
        String source = Files.readString(locateMainSources().resolve("event/CommonEvents.java"));

        assertTrue(source.contains("onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent"));
        assertTrue(source.contains("p.server.execute(() -> restoreStageEffects(p))"));
        assertTrue(source.contains("onPlayerRespawn(PlayerEvent.PlayerRespawnEvent"));
        assertTrue(source.contains("onPlayerJoin(PlayerEvent.PlayerLoggedInEvent"));
        assertTrue(source.contains("onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent"));
        assertTrue(source.contains("if (data.isTribulationActive())"));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "src", "main", "java", "com", "cultivation", "cultivation"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate main sources");
    }
}
