package com.cultivation.cultivation.compat.mekanism;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the no-Mekanism client/server class-loading boundary. */
final class MekanismOptionalBoundaryTest {
    private static final Path PROJECT = locateProject();
    private static final Path JAVA = PROJECT.resolve("src/main/java");

    @Test
    void officialCapabilityNamesAndApiSignaturesArePinnedInTheIsolatedDescriptor() {
        assertEquals("1.21.1-10.7.19.85", MekanismApiDescriptor.COMPILE_API_VERSION);
        assertEquals("mekanism:chemical_handler", MekanismApiDescriptor.CHEMICAL_CAPABILITY_ID);
        assertEquals("mekanism:strict_energy_handler", MekanismApiDescriptor.STRICT_ENERGY_CAPABILITY_ID);
        assertEquals("mekanism.api.chemical.IChemicalHandler",
                MekanismApiDescriptor.CHEMICAL_HANDLER_CLASS);
        assertEquals("mekanism.api.energy.IStrictEnergyHandler",
                MekanismApiDescriptor.STRICT_ENERGY_HANDLER_CLASS);
        assertEquals(8, MekanismApiDescriptor.MIN_XIANQIAO_STAGE);
    }

    @Test
    void absentMekanismApiIsDetectedWithoutLinkingOrThrowing() throws Exception {
        try (URLClassLoader empty = new URLClassLoader(new URL[0], null)) {
            MekanismApiDescriptor.Probe probe = MekanismApiDescriptor.probe(empty);
            assertFalse(probe.compatible());
            assertTrue(probe.missingClasses().contains(MekanismApiDescriptor.CHEMICAL_HANDLER_CLASS));
            assertTrue(probe.missingClasses().contains(MekanismApiDescriptor.STRICT_ENERGY_HANDLER_CLASS));
        }
    }

    @Test
    void noHardMekanismReferenceEscapesTheOptionalPackage() throws IOException {
        Path optionalRoot = JAVA.resolve(Path.of(
                "com", "cultivation", "cultivation", "compat", "mekanism"));
        assertTrue(Files.isDirectory(optionalRoot));

        try (var files = Files.walk(JAVA)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (source.startsWith(optionalRoot)) continue;
                String text = Files.readString(source);
                assertFalse(text.contains("import mekanism.") || text.contains("mekanism.api."),
                        () -> "hard Mekanism API reference escaped optional module: " + source);
            }
        }
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
