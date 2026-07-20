package com.immortalstorage.immortalstorage.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BeyondDimensionsOptionalBoundaryTest {
    private static final Path PROJECT = locateProject();
    private static final Path JAVA = PROJECT.resolve(Path.of("src", "main", "java"));

    @Test
    void hardApiTypesStayInsideTheOptionalPackage() throws IOException, ClassNotFoundException {
        Path optionalRoot = JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "beyonddimensions"));
        try (var files = Files.walk(JAVA)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (source.startsWith(optionalRoot)) continue;
                assertFalse(Files.readString(source).contains("com.wintercogs.beyonddimensions"),
                        () -> "hard Beyond Dimensions reference escaped optional package: " + source);
            }
        }

        assertTrue(Class.forName(
                "com.immortalstorage.immortalstorage.network.storage.backend.PersonalStorageBackendRouter") != null);
        assertTrue(Class.forName(
                "com.immortalstorage.immortalstorage.network.storage.PersonalStorageNetwork") != null);
    }

    @Test
    void dependencyAndMetadataPinTheAuditedOfficialRelease() throws IOException {
        String gradle = Files.readString(PROJECT.resolve("build.gradle"));
        assertTrue(gradle.contains(
                "compileOnly('maven.modrinth:beyonddimensions:0.7.24-1.21.1-neoforge')"));
        assertFalse(gradle.contains(
                "implementation 'maven.modrinth:beyonddimensions"));

        String mods = Files.readString(PROJECT.resolve(Path.of(
                "src", "main", "resources", "META-INF", "neoforge.mods.toml")));
        int dependency = mods.indexOf("modId=\"beyonddimensions\"");
        assertTrue(dependency >= 0);
        String section = mods.substring(dependency);
        assertTrue(section.contains("type=\"optional\""));
        assertTrue(section.contains("versionRange=\"[0.7.24]\""));
        assertTrue(section.contains("side=\"BOTH\""));
    }

    @Test
    void concreteAdapterUsesPrimaryNetAndOfficialLongStorageTypes() throws IOException {
        String manager = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "CompatManager.java")));
        assertFalse(manager.contains("initializeBeyondDimensions"),
                "Beyond Dimensions must not replace or disable ImmortalStorage personal storage");
        assertFalse(manager.contains("BeyondDimensionsCompat"),
                "the legacy authority-backend bootstrap must remain dormant");

        String compat = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "beyonddimensions",
                "BeyondDimensionsCompat.java")));
        assertTrue(compat.contains("DimensionsNet.getPrimaryNetFromPlayer"));
        assertTrue(compat.contains("getUnifiedStorage()"));

        String items = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "beyonddimensions",
                "BeyondDimensionsItemStorage.java")));
        assertTrue(items.contains("new ItemStackKey"));
        assertTrue(items.contains("KeyAmount"));
        assertTrue(items.contains("long"));

        String fluids = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "beyonddimensions",
                "BeyondDimensionsFluidStorage.java")));
        assertTrue(fluids.contains("new FluidStackKey"));
        assertTrue(fluids.contains("KeyAmount"));
        assertTrue(fluids.contains("long"));
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
