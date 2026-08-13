package com.immortalstorage.immortalstorage.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the no-AE2 client/server class-loading boundary. */
final class Ae2OptionalBoundaryTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProject();
    private static final Path JAVA = PROJECT.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source");

    @Test
    void allHardAe2ReferencesStayInsideTheOptionalModule() throws IOException {
        Path optionalRoot = JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "ae2"));
        assertTrue(Files.isDirectory(optionalRoot));
        try (var files = Files.walk(optionalRoot)) {
            assertTrue(files.anyMatch(path -> path.toString().endsWith(".java")),
                    "the optional AE2 module must contain its own adapter classes");
        }

        try (var files = Files.walk(JAVA)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (source.startsWith(optionalRoot)) continue;
                String text = Files.readString(source);
                assertFalse(text.contains("import appeng.") || text.contains("appeng.api."),
                        () -> "hard AE2 reference escaped optional module: " + source);
            }
        }

        String manager = source("compat", "CompatManager.java");
        assertTrue(manager.contains("Class.forName(className"));
        assertTrue(manager.contains("compat.ae2.Ae2Compat"));
        assertFalse(manager.contains("import appeng."));
        assertFalse(source("item", "custom", "XianqiaoExchangeCellItem.java").contains("appeng."));

        String endpoint = source("api", "storage", "ExternalResourceStorage.java");
        assertTrue(endpoint.contains("ResourceChannelKey"));
        assertFalse(endpoint.contains("appeng."),
                "the authoritative extra-resource API must work without AE2");

        String sharedKey = Files.readString(PROJECT.resolve(Path.of(
                "..", "immortalstorage-core", "src", "main", "java", "com", "immortalstorage",
                "core", "resource", "ResourceChannelKey.java")).normalize());
        assertFalse(sharedKey.contains("appeng."),
                "the shared resource identity must not depend on AE2");
    }

    @Test
    void dependencyAndRecipeRemainOptionalAndPinnedForThisAdapter() throws IOException {
        String gradle = Files.readString(PROJECT.resolve("build.gradle"));
        assertTrue(gradle.contains(
                "addCompatDependency('compileOnly', 'ae2', 'compile')"));
        String matrix = Files.readString(PROJECT.resolve(Path.of(
                "..", "version-compat", "compatibility-mod-matrix.json")).normalize());
        assertTrue(matrix.contains(
                "org.appliedenergistics:appliedenergistics2:26.1.10-beta:api"));
        assertFalse(gradle.contains(
                "implementation 'org.appliedenergistics:appliedenergistics2"));

        String mods = Files.readString(PROJECT.resolve("build/resources/main/META-INF/neoforge.mods.toml"));
        int dependency = mods.indexOf("modId=\"ae2\"");
        assertTrue(dependency >= 0);
        String ae2Section = mods.substring(dependency);
        assertTrue(ae2Section.contains("type=\"optional\""));
        assertTrue(ae2Section.contains("versionRange=\"[26.1.10-beta,)\""));
        assertTrue(ae2Section.contains("side=\"BOTH\""));

        String recipe = resource("data/immortalstorage/recipe/xianqiao_exchange_cell.json");
        for (String required : List.of(
                "\"neoforge:conditions\"", "\"type\": \"neoforge:mod_loaded\"",
                "\"modid\": \"ae2\"", "\"item\": \"ae2:item_cell_housing\"",
                "\"item\": \"immortalstorage:xianqiao_manager\"")) {
            assertTrue(recipe.contains(required), required);
        }
        assertTrue(Files.isRegularFile(PROJECT.resolve(
                "src/main/resources/assets/immortalstorage/models/item/xianqiao_exchange_cell.json")));
        assertTrue(Files.isRegularFile(PROJECT.resolve(
                "src/main/resources/assets/immortalstorage/textures/item/xianqiao_exchange_cell.png")));

        String fluidHousingRecipe = resource(
                "data/immortalstorage/recipe/xianqiao_exchange_cell_from_fluid_housing.json");
        for (String required : List.of(
                "\"neoforge:conditions\"", "\"type\": \"neoforge:mod_loaded\"",
                "\"modid\": \"ae2\"", "\"item\": \"ae2:fluid_cell_housing\"",
                "\"item\": \"immortalstorage:xianqiao_manager\"")) {
            assertTrue(fluidHousingRecipe.contains(required), required);
        }
    }

    private static String source(String... relative) throws IOException {
        Path path = JAVA.resolve(Path.of("com", "immortalstorage", "immortalstorage"));
        for (String part : relative) path = path.resolve(part);
        return Files.readString(path);
    }

    private static String resource(String relative) throws IOException {
        return Files.readString(PROJECT.resolve("src/main/resources").resolve(relative));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))
                    && Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project root");
    }
}
