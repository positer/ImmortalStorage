package com.immortalstorage.immortalstorage.metadata;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModDescriptionContractTest {
    @Test
    void descriptionRepresentsTheZeroPointZeroPointElevenMiniatureRuinRules() throws Exception {
        Path project = locateProject();
        String properties = Files.readString(project.resolve("gradle.properties"));

        assertTrue(properties.contains("cultivation progression, personal storage, automation"));
        assertTrue(properties.contains("persistent-player-bound realm"));
        assertTrue(properties.contains("Version 0.0.11"));
        assertTrue(properties.contains("exact 13x1x13 block-layer collision box"));
        assertTrue(properties.contains("excludes players holding the miniature ruin"));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("gradle.properties"))
                    && Files.isDirectory(current.resolve("src/main"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate NeoForge project");
    }
}
