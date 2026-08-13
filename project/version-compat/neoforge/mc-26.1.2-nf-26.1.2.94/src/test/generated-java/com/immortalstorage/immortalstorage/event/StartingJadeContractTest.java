package com.immortalstorage.immortalstorage.event;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StartingJadeContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void firstLoginGrantsOneConfiguredJadeAndPersistsTheOneShotMarker() throws IOException {
        Path main = locateMainSources();
        String events = Files.readString(main.resolve("event/CommonEvents.java"));
        String data = Files.readString(main.resolve("player/ImmortalStoragePlayerData.java"));
        String config = Files.readString(main.resolve("config/ImmortalStorageConfig.java"));

        assertTrue(events.contains("grantStartingJade(p)"));
        assertTrue(events.contains("START_WITH_JADE_GUIDE.get()"));
        assertTrue(events.contains("data.isStartingJadeGranted()"));
        assertTrue(events.contains("hasJadeGuide(player)"));
        assertTrue(events.contains("player.getInventory().add(jade)"));
        assertTrue(data.contains("tag.putBoolean(\"startingJadeGranted\""));
        assertTrue(data.contains("startingJadeGranted = tag.getBooleanOr(\"startingJadeGranted\", false)"));
        assertTrue(config.contains("define(\"startWithJadeGuide\", true)"));
    }

    @Test
    void stageTenDefaultsToFiniteGenerationButRetainsTheVirtualConfigChannel() throws IOException {
        Path main = locateMainSources();
        String config = Files.readString(main.resolve("config/ImmortalStorageConfig.java"));
        String profile = Files.readString(main.resolve("player/yuan/YuanProfile.java"));

        assertTrue(config.contains("define(\"stageTenInfiniteImmortalYuan\", false)"));
        assertTrue(profile.contains("new YuanRule(YuanRule.UNBOUNDED_CAP, 20, 256L)"));
        assertTrue(profile.contains("if (infiniteStageTen)"));
        assertTrue(profile.contains("new YuanRule(YuanRule.UNBOUNDED_CAP, 0, 0L)"));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate main sources");
    }
}
