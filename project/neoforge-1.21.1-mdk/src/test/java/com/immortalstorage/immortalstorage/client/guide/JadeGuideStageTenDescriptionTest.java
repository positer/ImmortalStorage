package com.immortalstorage.immortalstorage.client.guide;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JadeGuideStageTenDescriptionTest {
    @Test
    void stageTenHomeTaskTracksTheConfiguredYuanChannel() {
        assertEquals("guide.immortalstorage.jade.stage.10.next.generated",
                JadeGuideProgression.nextGoalKey(10, false));
        assertEquals("guide.immortalstorage.jade.stage.10.next.infinite",
                JadeGuideProgression.nextGoalKey(10, true));
    }

    @Test
    void localizedTasksCoverEveryStageAndCurrentTribulationRules() throws IOException {
        Path resources = locateResources();
        for (String language : new String[]{"zh_cn.json", "en_us.json"}) {
            String text = Files.readString(resources.resolve(language));
            for (int stage = 0; stage <= 9; stage++) {
                assertTrue(text.contains("guide.immortalstorage.jade.stage." + stage + ".next"));
            }
            assertTrue(text.contains("guide.immortalstorage.jade.stage.10.next.generated"));
            assertTrue(text.contains("guide.immortalstorage.jade.stage.10.next.infinite"));
            assertTrue(text.contains("256"));
            assertTrue(text.contains(language.startsWith("zh_") ? "永不减少" : "without diminishing"));
        }
    }

    private static Path locateResources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("src", "main", "resources", "assets", "immortalstorage", "lang"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate language resources");
    }
}
