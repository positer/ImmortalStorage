package com.immortalstorage.immortalstorage.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PatchouliOptionalBoundaryTest {
    @Test
    void commonJadeItemKeepsTheBuiltInGuideAsTheNoPatchouliFallback() throws IOException {
        String item = Files.readString(source("item", "SimpleJadeGuideItem.java"));
        assertTrue(item.contains("CompatManager.PATCHOULI_LOADED"));
        assertTrue(item.contains("Class.forName("));
        assertTrue(item.contains("PacketDistributor.sendToPlayer(serverPlayer, new ModPayloads.OpenJadeGuideScreen())"));
        assertFalse(item.contains("import vazkii.patchouli"),
                "the always-loaded Jade item must not reference optional Patchouli types");
    }

    @Test
    void optionalBridgeUsesTheOfficialServerOpenApi() throws IOException {
        String bridge = Files.readString(source("compat", "patchouli", "PatchouliJadeGuideCompat.java"));
        assertTrue(bridge.contains("PatchouliAPI.get().openBookGUI(player, BOOK)"));
        assertTrue(bridge.contains("ImmortalStorageMod.MODID, \"jade_guide\""));
    }

    @Test
    void localizedBookHasDetailedCategoriesAndValidJson() throws IOException {
        Path resources = resources();
        JsonObject book = json(resources.resolve("data/immortalstorage/patchouli_books/jade_guide/book.json"));
        assertTrue(book.get("use_resource_pack").getAsBoolean());
        for (String locale : new String[]{"zh_cn", "en_us"}) {
            Path root = resources.resolve("assets/immortalstorage/patchouli_books/jade_guide").resolve(locale);
            assertTrue(Files.list(root.resolve("categories")).count() >= 6);
            assertTrue(Files.walk(root.resolve("entries")).filter(path -> path.toString().endsWith(".json")).count() >= 12);
            try (var paths = Files.walk(root)) {
                for (Path path : paths.filter(value -> value.toString().endsWith(".json")).toList()) json(path);
            }
        }
    }

    private static JsonObject json(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static Path source(String... parts) {
        Path root = project().resolve("src/main/java/com/immortalstorage/immortalstorage");
        for (String part : parts) root = root.resolve(part);
        return root;
    }

    private static Path resources() {
        return project().resolve("src/main/resources");
    }

    private static Path project() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src/main/java/com/immortalstorage/immortalstorage"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate NeoForge project");
    }
}
