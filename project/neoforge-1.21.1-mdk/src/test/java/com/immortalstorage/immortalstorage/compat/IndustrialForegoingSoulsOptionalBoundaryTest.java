package com.immortalstorage.immortalstorage.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IndustrialForegoingSoulsOptionalBoundaryTest {
    private static final Path PROJECT = locateProject();
    private static final Path JAVA = PROJECT.resolve(Path.of("src", "main", "java"));

    @Test
    void soulApiTypesStayInsideTheOptionalPackage() throws IOException {
        Path optionalRoot = JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "ifsouls"));
        assertTrue(Files.isDirectory(optionalRoot));
        try (var files = Files.walk(JAVA)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (source.startsWith(optionalRoot)) continue;
                assertFalse(Files.readString(source).contains(
                                "com.buuz135.industrialforegoingsouls"),
                        () -> "hard soul API reference escaped optional package: " + source);
            }
        }
    }

    @Test
    void officialVersionCapabilityAndLifecycleArePinned() throws IOException {
        String build = Files.readString(PROJECT.resolve("build.gradle"));
        assertTrue(build.contains(
                "maven.modrinth:industrial-foregoing-souls:1.21-1.10.7"));
        String metadata = Files.readString(PROJECT.resolve(Path.of(
                "src", "main", "resources", "META-INF", "neoforge.mods.toml")));
        assertTrue(metadata.contains("modId=\"industrialforegoingsouls\""));
        assertTrue(metadata.contains("versionRange=\"[1.10.7,1.11)\""));

        String compat = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "ifsouls",
                "IndustrialForegoingSoulsCompat.java")));
        assertTrue(compat.contains("SoulCapabilities.BLOCK"));
        assertTrue(compat.contains("RegisterCapabilitiesEvent"));
        assertTrue(compat.contains("activeTransferTick"));
        assertTrue(compat.contains("XianqiaoInterfaceCompatHooks"));
        assertTrue(compat.contains("SoulActiveTransfer.push"));
        assertTrue(compat.contains("SoulActiveTransfer.pull"));

        String manager = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "CompatManager.java")));
        assertTrue(manager.contains("if (INDUSTRIAL_FOREGOING_SOULS_LOADED)"));
        assertTrue(manager.contains("registerIndustrialForegoingSoulsCapabilities"));
        assertTrue(manager.contains("ExternalResourceChannels.INDUSTRIAL_FOREGOING_SOUL"));
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
