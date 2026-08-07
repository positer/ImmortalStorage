package com.immortalstorage.immortalstorage.metadata;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModDescriptionContractTest {
    @Test
    void descriptionRepresentsTheCompleteZeroPointZeroPointEightFeatureSet() throws Exception {
        Path project = locateProject();
        String properties = Files.readString(project.resolve("gradle.properties"));

        assertTrue(properties.contains("cultivation progression, personal storage, automation"));
        assertTrue(properties.contains("UUID-bound realm"));
        assertTrue(properties.contains("beautified Xianqiao Interface and Spirit Sword family textures/models"));
        assertTrue(properties.contains("Advanced Xianqiao Interface"));
        assertTrue(properties.contains("advanced-stabilized-ruin-style range scheduling"));
        assertTrue(properties.contains("xyz/+xzy"));
        assertTrue(properties.contains("preview"));
        assertTrue(properties.contains("item/fluid/power/chemical cache slots"));
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
