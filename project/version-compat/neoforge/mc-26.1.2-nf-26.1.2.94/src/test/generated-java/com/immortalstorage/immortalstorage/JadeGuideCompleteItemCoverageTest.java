package com.immortalstorage.immortalstorage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JadeGuideCompleteItemCoverageTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProject();
    private static final Pattern DIRECT_ITEM = Pattern.compile("registerItem\\(\\s*\"([^\"]+)\"");
    private static final Pattern BLOCK_ITEM = Pattern.compile("(?:\\breg|BLOCKS\\.register)\\(\\s*\"([^\"]+)\"");

    @Test
    void everyDirectlyRegisteredItemAppearsInBothLanguages() throws IOException {
        String source = Files.readString(PROJECT.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/item/ModItems.java"));
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = DIRECT_ITEM.matcher(source);
        while (matcher.find()) ids.add(matcher.group(1));

        for (String locale : new String[]{"zh_cn", "en_us"}) {
            Path localeRoot = PROJECT.resolve(
                    "src/main/resources/assets/immortalstorage/patchouli_books/jade_guide/" + locale);
            String guide;
            try (Stream<Path> files = Files.walk(localeRoot)) {
                guide = files.filter(path -> path.toString().endsWith(".json"))
                        .map(JadeGuideCompleteItemCoverageTest::readUnchecked)
                        .reduce("", (left, right) -> left + "\n" + right);
            }
            for (String id : ids) {
                assertTrue(guide.contains("immortalstorage:" + id),
                        () -> locale + " guide omits item immortalstorage:" + id);
            }
        }
    }

    @Test
    void everyRegisteredBlockItemAppearsInBothLanguages() throws IOException {
        String source = Files.readString(PROJECT.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/block/ModBlocks.java"));
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = BLOCK_ITEM.matcher(source);
        while (matcher.find()) ids.add(matcher.group(1));
        assertIdsCovered(ids);
    }

    @Test
    void immortalFurnacePagesUseFurnaceArtworkAndExplicitFiringLanguage() throws IOException {
        for (String locale : new String[]{"zh_cn", "en_us"}) {
            String entry = Files.readString(PROJECT.resolve(
                    "src/main/resources/assets/immortalstorage/patchouli_books/jade_guide/" + locale
                            + "/entries/equipment/immortal_forging.json"));
            assertTrue(entry.contains("\"type\":\"patchouli:smelting\""));
            assertTrue(locale.equals("zh_cn") ? entry.contains("仙炉煅烧")
                    : entry.contains("Immortal-Furnace Firing"));
        }
    }

    @Test
    void everyRecipeIdAppearsInBothLanguages() throws IOException {
        Path recipes = PROJECT.resolve("src/main/resources/data/immortalstorage/recipe");
        try (Stream<Path> recipeFiles = Files.list(recipes)) {
            for (Path recipe : recipeFiles.filter(path -> path.toString().endsWith(".json")).toList()) {
                String id = "immortalstorage:" + recipe.getFileName().toString().replaceFirst("\\.json$", "");
                for (String locale : new String[]{"zh_cn", "en_us"}) {
                    Path localeRoot = PROJECT.resolve(
                            "src/main/resources/assets/immortalstorage/patchouli_books/jade_guide/" + locale);
                    String guide;
                    try (Stream<Path> files = Files.walk(localeRoot)) {
                        guide = files.filter(path -> path.toString().endsWith(".json"))
                                .map(JadeGuideCompleteItemCoverageTest::readUnchecked)
                                .reduce("", (left, right) -> left + "\n" + right);
                    }
                    assertTrue(guide.contains(id), () -> locale + " guide omits recipe " + id);
                }
            }
        }
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void assertIdsCovered(Set<String> ids) throws IOException {
        for (String locale : new String[]{"zh_cn", "en_us"}) {
            Path localeRoot = PROJECT.resolve(
                    "src/main/resources/assets/immortalstorage/patchouli_books/jade_guide/" + locale);
            String guide;
            try (Stream<Path> files = Files.walk(localeRoot)) {
                guide = files.filter(path -> path.toString().endsWith(".json"))
                        .map(JadeGuideCompleteItemCoverageTest::readUnchecked)
                        .reduce("", (left, right) -> left + "\n" + right);
            }
            for (String id : ids) {
                assertTrue(guide.contains("immortalstorage:" + id),
                        () -> locale + " guide omits block item immortalstorage:" + id);
            }
        }
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
