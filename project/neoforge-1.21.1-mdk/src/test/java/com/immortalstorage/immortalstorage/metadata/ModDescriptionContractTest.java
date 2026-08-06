package com.immortalstorage.immortalstorage.metadata;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModDescriptionContractTest {
    @Test
    void descriptionRepresentsTheCompleteZeroPointZeroPointSevenFeatureSet() throws Exception {
        Path project = locateProject();
        String properties = Files.readString(project.resolve("gradle.properties"));

        assertTrue(properties.contains("cultivation progression, personal storage, automation"));
        assertTrue(properties.contains("UUID-bound realm"));
        assertTrue(properties.contains("Echo Shard Source Vein"));
        assertTrue(properties.contains("Create fan-processing catalysts"));
        assertTrue(properties.contains("AE addon storage-capability adaptation"));
        assertTrue(properties.contains("Entangled / Advanced / Advanced-Entangled"));
        assertTrue(properties.contains("container scheduling and Xianqiao memory routing"));
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
