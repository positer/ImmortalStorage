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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every item reference in the Ancient Jade guide must name a registered item.
 * A block id without a BlockItem is not a valid Patchouli "item" value: the
 * guide page then fails to build in game ("Unknown item").
 */
class JadeGuideItemReferenceTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProject();
    private static final Pattern MOD_ITEM = Pattern.compile("registerItem\\(\\s*\"([^\"]+)\"");
    private static final Pattern ITEMS_REGISTER = Pattern.compile("ITEMS\\.register\\(\\s*\"([^\"]+)\"");
    private static final Pattern BLOCK_HELPER = Pattern.compile("(?<![\\w.])reg\\(\\s*\"([^\"]+)\"");
    private static final Pattern GUIDE_ITEM = Pattern.compile("\"(?:item|icon)\"\\s*:\\s*\"([^\"]*immortalstorage:[^\"]*)\"");

    @Test
    void everyGuideItemReferenceIsARegisteredItem() throws IOException {
        Set<String> items = registeredItemIds();
        assertTrue(items.contains("true_yuan"), "sanity: true_yuan must be a registered item");
        assertFalse(items.contains("true_yuan_light"), "sanity: true_yuan_light is a block, not an item");

        Set<String> missing = new LinkedHashSet<>();
        for (String locale : new String[]{"zh_cn", "en_us"}) {
            Path root = PROJECT.resolve(
                    "src/main/resources/assets/immortalstorage/patchouli_books/jade_guide/" + locale);
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                    Matcher reference = GUIDE_ITEM.matcher(Files.readString(file));
                    while (reference.find()) {
                        for (String part : reference.group(1).split(",")) {
                            String id = part.trim();
                            if (!id.startsWith("immortalstorage:")) continue;
                            String name = id.substring("immortalstorage:".length());
                            if (!items.contains(name)) {
                                missing.add(locale + " " + root.relativize(file) + " -> " + id);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), () -> "guide references unregistered items: " + missing);
    }

    private static Set<String> registeredItemIds() throws IOException {
        Path javaRoot = PROJECT.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source/com/immortalstorage/immortalstorage");
        Set<String> ids = new LinkedHashSet<>();
        String modItems = Files.readString(javaRoot.resolve("item/ModItems.java"));
        collect(MOD_ITEM, modItems, ids);
        collect(ITEMS_REGISTER, modItems, ids);
        // ModBlocks.reg(...) also registers a BlockItem; BLOCKS.register(...) alone does not.
        collect(BLOCK_HELPER, Files.readString(javaRoot.resolve("block/ModBlocks.java")), ids);
        return ids;
    }

    private static void collect(Pattern pattern, String source, Set<String> into) {
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) into.add(matcher.group(1));
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
