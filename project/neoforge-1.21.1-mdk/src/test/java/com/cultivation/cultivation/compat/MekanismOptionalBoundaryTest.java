package com.cultivation.cultivation.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MekanismOptionalBoundaryTest {
    private static final Path PROJECT = locateProject();
    private static final Path JAVA = PROJECT.resolve(Path.of("src", "main", "java"));

    @Test
    void mekanismApiTypesStayInsideTheOptionalPackage() throws IOException {
        Path optionalRoot = JAVA.resolve(Path.of(
                "com", "cultivation", "cultivation", "compat", "mekanism"));
        try (var files = Files.walk(JAVA)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (source.startsWith(optionalRoot)) continue;
                assertFalse(Files.readString(source).contains("mekanism.api"),
                        () -> "hard Mekanism reference escaped optional package: " + source);
            }
        }
    }

    @Test
    void officialApiVersionAndCapabilityLifecycleArePinned() throws IOException {
        String build = Files.readString(PROJECT.resolve("build.gradle"));
        assertTrue(build.contains("mekanism:Mekanism:1.21.1-10.7.19.85:api"));

        String metadata = Files.readString(PROJECT.resolve(Path.of(
                "src", "main", "resources", "META-INF", "neoforge.mods.toml")));
        assertTrue(metadata.contains("modId=\"mekanism\""));
        assertTrue(metadata.contains("versionRange=\"[10.7.19,10.8)\""));

        String compat = Files.readString(JAVA.resolve(Path.of(
                "com", "cultivation", "cultivation", "compat", "mekanism", "MekanismCompat.java")));
        assertTrue(compat.contains("strict_energy_handler"));
        assertTrue(compat.contains("IStrictEnergyHandler.class"));
        assertTrue(compat.contains("chemical_handler"));
        assertTrue(compat.contains("IChemicalHandler.class"));
        assertTrue(compat.contains("RegisterCapabilitiesEvent"));
        assertTrue(compat.contains("activeChemicalTransferTick"));
        assertTrue(compat.contains(
                "mode == XianqiaoInterfaceBlockEntity.SideMode.PUSH"));
        assertTrue(compat.contains("&& blockEntity.isActivePushEnabled()"));
        assertTrue(compat.contains("XianqiaoInterfaceCompatHooks"));

        String chemical = Files.readString(JAVA.resolve(Path.of(
                "com", "cultivation", "cultivation", "compat", "mekanism",
                "XianqiaoMekanismChemicalAdapter.java")));
        assertTrue(chemical.contains("implements IChemicalHandler"));
        assertTrue(chemical.contains("ExternalResourceChannels.mekanismChemical"));
        assertTrue(chemical.contains("Long.MAX_VALUE"));
        assertTrue(chemical.contains("ResourceTransferAction.SIMULATE"));

        String manager = Files.readString(JAVA.resolve(Path.of(
                "com", "cultivation", "cultivation", "compat", "CompatManager.java")));
        assertTrue(manager.contains("if (MEKANISM_LOADED)"));
        assertTrue(manager.contains("registerMekanismCapabilities"));
        assertTrue(manager.contains("ExternalResourceChannels.FE"));
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
