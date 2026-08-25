package com.immortalstorage.immortalstorage.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateNurturingOptionalBoundaryTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProject();

    @Test
    void createTypesStayInsideOptionalPackage() throws IOException {
        String manager = source("compat/CompatManager.java");
        assertTrue(manager.contains("CREATE_LOADED = modPresent(\"create\")"));
        assertTrue(manager.contains("compat.create.CreateNurturingCompat"));
        assertFalse(manager.contains("import com.simibubi.create"));

        String compat = source("compat/create/CreateNurturingCompat.java");
        assertTrue(compat.contains("CreateBuiltInRegistries.FAN_PROCESSING_TYPE"));
        assertTrue(compat.contains("DeferredRegister.create("));
        assertTrue(compat.contains("public static void register(IEventBus modBus)"));
        assertTrue(compat.contains("TYPES.register(modBus)"));
        assertFalse(compat.contains("Registry.register(CreateBuiltInRegistries.FAN_PROCESSING_TYPE"),
                "Create's registry is frozen by mod construction; registration must use NeoForge's registry event");
        assertTrue(manager.contains("\"register\","));
        assertTrue(manager.contains("new Class<?>[]{IEventBus.class}"));
        assertTrue(compat.contains("ModRecipes.IMMORTAL_FURNACE_TYPE"));
        assertTrue(compat.contains("ModBlocks.TRUE_YUAN_LIGHT"));
        assertTrue(compat.contains("ModBlocks.IMMORTAL_YUAN_LIGHT"));
        assertTrue(compat.contains("MobEffects.REGENERATION"));
        assertTrue(compat.contains("30, 4"), "Regeneration V must use amplifier 4");
        assertTrue(compat.contains("0xD7FFFF") && compat.contains("0x83F4EF"));
    }

    @Test
    void targetWithoutOfficialCreateArtifactExcludesOldApi() throws IOException {
        String build = Files.readString(PROJECT.resolve("build.gradle"));
        assertTrue(build.contains("exclude 'com/immortalstorage/immortalstorage/compat/create/**'"));
        String matrix = Files.readString(PROJECT.resolve("../version-compat/compatibility-mod-matrix.json"));
        assertTrue(matrix.contains("\"create\""));
        assertTrue(matrix.contains("Create 6.0.10"));
        assertTrue(matrix.contains("no official Minecraft 26.1.2 NeoForge artifact exists"));
    }

    private static String source(String relative) throws IOException {
        return Files.readString(PROJECT.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/" + relative));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))
                    && Files.isDirectory(current.resolve("src/main/resources"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage project");
    }
}
