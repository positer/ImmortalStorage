package com.immortalstorage.immortalstorage.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two 0.1.8 recipe decisions, pinned as data contracts.
 *
 * <p>1. Activating a Nurturing Crystal Bedrock is reachable from the crafting table again: eight
 * Immortal Yuan around one inactive bedrock. The 0.1.6 compressor route stays as well, so the two
 * stations must agree on their input and output.
 *
 * <p>2. AE2's Flawless Budding Certus Quartz becomes compressible — one Certus Quartz Block plus
 * eight Immortal Yuan — but only on an installation that actually has AE2. The recipe therefore has
 * to carry a mod-loaded condition instead of being registered unconditionally, otherwise a vanilla
 * or non-AE2 pack would ship a recipe whose ingredient cannot resolve.
 */
class ZeroPointOnePointEightRecipeContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path MAIN = locateMainSourceRoot();
    private static final String RECIPES = "resources/data/immortalstorage/recipe/";

    private static Path locateMainSourceRoot() {
        for (Path cursor = Path.of("").toAbsolutePath(); cursor != null; cursor = cursor.getParent()) {
            Path candidate = cursor.resolve("src/main");
            if (Files.isDirectory(candidate.resolve("java"))
                    && Files.isDirectory(candidate.resolve("resources"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not locate the NeoForge src/main directory");
    }

    private static JsonObject recipe(String relative) throws Exception {
        String text = Files.readString(MAIN.resolve(RECIPES + relative), StandardCharsets.UTF_8);
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static String itemId(JsonObject ingredient) {
        // 1.21.1 writes {"item": "id"}; the 26.1.2 overlay flattens it to "id".
        return ingredient.has("item") ? ingredient.get("item").getAsString() : ingredient.getAsString();
    }

    @Test
    void eightImmortalYuanAroundAnInactiveBedrockActivateTheCraftingTableRoute() throws Exception {
        JsonObject recipe = recipe("nurturing_crystal_bedrock.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        JsonArray pattern = recipe.getAsJsonArray("pattern");
        assertEquals(3, pattern.size(), "the activation pattern is a full 3x3 grid");
        for (int row = 0; row < 3; row++) {
            assertEquals(3, pattern.get(row).getAsString().length());
        }
        assertEquals("III", pattern.get(0).getAsString());
        assertEquals("IBI", pattern.get(1).getAsString());
        assertEquals("III", pattern.get(2).getAsString());

        JsonObject key = recipe.getAsJsonObject("key");
        assertEquals("immortalstorage:immortal_yuan", itemId(key.getAsJsonObject("I")));
        assertEquals("immortalstorage:inactive_nurturing_crystal_bedrock", itemId(key.getAsJsonObject("B")));

        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals("immortalstorage:nurturing_crystal_bedrock", result.get("id").getAsString());
        assertEquals(1, result.get("count").getAsInt());
    }

    @Test
    void theCompressorRouteStaysSoBothStationsAgree() throws Exception {
        JsonObject compression = recipe("spirit_compression/nurturing_crystal_bedrock.json");
        assertEquals("immortalstorage:spirit_compression", compression.get("type").getAsString());
        JsonObject inner = compression.getAsJsonObject("recipe");
        assertEquals("immortalstorage:inactive_nurturing_crystal_bedrock", itemId(inner.getAsJsonObject("ingredient")));
        assertEquals("immortalstorage:nurturing_crystal_bedrock", inner.getAsJsonObject("result").get("id").getAsString());
        JsonArray additional = compression.getAsJsonArray("additional_inputs");
        assertEquals(1, additional.size());
        JsonObject yuan = additional.get(0).getAsJsonObject();
        assertEquals("immortalstorage:immortal_yuan", itemId(yuan.getAsJsonObject("ingredient")));
        assertEquals(8, yuan.get("count").getAsInt(), "both stations must bill the same eight Immortal Yuan");
    }

    @Test
    void flawlessBuddingCertusQuartzIsCompressedOnlyWhenAe2IsInstalled() throws Exception {
        JsonObject recipe = recipe("spirit_compression/flawless_budding_quartz.json");
        assertEquals("immortalstorage:spirit_compression", recipe.get("type").getAsString());

        JsonArray conditions = recipe.getAsJsonArray("neoforge:conditions");
        assertEquals(1, conditions.size(), "exactly one gate keeps the recipe optional");
        JsonObject condition = conditions.get(0).getAsJsonObject();
        assertEquals("neoforge:mod_loaded", condition.get("type").getAsString());
        assertEquals("ae2", condition.get("modid").getAsString());

        JsonObject inner = recipe.getAsJsonObject("recipe");
        assertEquals("ae2:quartz_block", itemId(inner.getAsJsonObject("ingredient")));
        assertEquals("ae2:flawless_budding_quartz", inner.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, inner.getAsJsonObject("result").get("count").getAsInt());

        JsonArray additional = recipe.getAsJsonArray("additional_inputs");
        assertEquals(1, additional.size());
        JsonObject yuan = additional.get(0).getAsJsonObject();
        assertEquals("immortalstorage:immortal_yuan", itemId(yuan.getAsJsonObject("ingredient")));
        assertEquals(8, yuan.get("count").getAsInt());
    }

    @Test
    void theAe2CompressionRecipeIsTheOnlyForeignIngredientInTheCompressorCatalog() throws Exception {
        Path compression = MAIN.resolve(RECIPES + "spirit_compression");
        List<Path> files;
        try (var stream = Files.list(compression)) {
            files = stream.filter(path -> path.toString().endsWith(".json")).toList();
        }
        for (Path file : files) {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            boolean foreign = text.contains("\"ae2:")
                    || text.contains("\"botania:")
                    || text.contains("\"ars_nouveau:")
                    || text.contains("\"mekanism:")
                    || text.contains("\"refinedstorage:")
                    || text.contains("\"create:");
            boolean gated = text.contains("neoforge:mod_loaded");
            String name = file.getFileName().toString();
            assertTrue(!foreign || gated,
                    name + ": a cross-mod compression recipe must carry its own mod-loaded condition");
            assertTrue(!gated || foreign,
                    name + ": a mod-loaded condition must only gate a cross-mod recipe");
        }
    }
}
