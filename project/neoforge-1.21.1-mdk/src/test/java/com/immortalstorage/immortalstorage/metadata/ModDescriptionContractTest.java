package com.immortalstorage.immortalstorage.metadata;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModDescriptionContractTest {
    @Test
    void descriptionRepresentsTheCompleteZeroPointZeroPointThreeFeatureSet() throws Exception {
        Path project = locateProject();
        String properties = Files.readString(project.resolve("gradle.properties"));

        assertTrue(properties.contains("cultivation progression, personal storage, automation"));
        assertTrue(properties.contains("UUID-bound realm"));
        assertTrue(properties.contains("Ancient Jade handbook"));
        assertTrue(properties.contains("Miniature Immortal Ruins"));
        assertTrue(properties.contains("Immortal-Ruin-Forged Spirit Sword"));
        assertTrue(properties.contains("Version 0.0.3 bundles Patchouli"));
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
