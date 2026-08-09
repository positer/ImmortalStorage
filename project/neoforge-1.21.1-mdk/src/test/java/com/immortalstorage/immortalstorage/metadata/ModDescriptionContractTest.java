package com.immortalstorage.immortalstorage.metadata;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModDescriptionContractTest {
    @Test
    void descriptionRepresentsTheZeroPointZeroPointTenTooltipAndHandbookUpdate() throws Exception {
        Path project = locateProject();
        String properties = Files.readString(project.resolve("gradle.properties"));

        assertTrue(properties.contains("cultivation progression, personal storage, automation"));
        assertTrue(properties.contains("persistent-player-bound realm"));
        assertTrue(properties.contains("Version 0.0.10"));
        assertTrue(properties.contains("realm, device, puppet, AE2 and RS disk ownership"));
        assertTrue(properties.contains("one-shot legacy migration"));
        assertTrue(properties.contains("AE2 external-resource rendering crashes"));
        assertTrue(properties.contains("sword tempering tooltip coefficients"));
        assertTrue(properties.contains("bilingual Patchouli handbook"));
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
