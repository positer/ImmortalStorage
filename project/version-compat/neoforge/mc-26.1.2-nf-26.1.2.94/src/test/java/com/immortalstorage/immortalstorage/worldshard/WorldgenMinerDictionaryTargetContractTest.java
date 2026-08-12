package com.immortalstorage.immortalstorage.worldshard;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Target-only contracts for the 26.1.2 worldgen and world-shard migration.
 *
 * These checks intentionally inspect the source/resource boundary as well as
 * the migrated Java call sites: a successful Java compile alone cannot catch
 * an old datapack codec or a resource overlay omitted from the production JAR.
 */
final class WorldgenMinerDictionaryTargetContractTest {
    private static final Path TARGET_ROOT = Path.of(
            "project", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94");
    private static final Path CANONICAL_ROOT = Path.of(
            "project", "neoforge-1.21.1-mdk");

    @Test
    void targetDimensionAndBiomeUseThe26DataCodecs() throws IOException {
        Path workspace = locateWorkspace();
        JsonObject dimension = json(workspace.resolve(TARGET_ROOT).resolve(Path.of(
                "src", "main", "resources", "data", "immortalstorage",
                "dimension_type", "xianqiao_realm.json")));
        assertFalse(dimension.has("bed_works"), "26.1 must not receive the removed dimension field");
        assertFalse(dimension.has("effects"), "26.1 dimension visuals belong in attributes");
        assertTrue(dimension.get("attributes").isJsonObject());
        assertTrue(dimension.get("attributes").getAsJsonObject()
                .has("minecraft:gameplay/bed_rule"));
        assertEquals(false, dimension.get("has_ender_dragon_fight").getAsBoolean());
        assertEquals("minecraft:overworld", dimension.get("default_clock").getAsString());
        assertEquals("#minecraft:in_overworld", dimension.get("timelines").getAsString());
        assertEquals(-64, dimension.get("min_y").getAsInt());
        assertEquals(384, dimension.get("height").getAsInt());

        JsonObject biome = json(workspace.resolve(TARGET_ROOT).resolve(Path.of(
                "src", "main", "resources", "data", "immortalstorage",
                "worldgen", "biome", "xianqiao_realm.json")));
        assertTrue(biome.get("carvers").isJsonArray(),
                "26.1 removed the legacy carving-step object shape");
        assertEquals(0, biome.getAsJsonArray("carvers").size());
        assertEquals(12, biome.getAsJsonArray("features").size());
        assertTrue(biome.get("attributes").getAsJsonObject()
                .has("minecraft:audio/ambient_sounds"));
        assertEquals("#3f76e4", biome.getAsJsonObject("effects")
                .get("water_color").getAsString());
        assertFalse(biome.getAsJsonObject("effects").has("mood_sound"),
                "ambient sounds moved to biome attributes in the target format");
    }

    @Test
    void spiritOresKeepTheirConfiguredWorldgenTargets() throws IOException {
        Path workspace = locateWorkspace();
        Path resources = workspace.resolve(CANONICAL_ROOT).resolve(Path.of("src", "main", "resources"));
        JsonObject iron = json(resources.resolve(Path.of(
                "data", "immortalstorage", "worldgen", "configured_feature", "spirit_iron_ore.json")));
        JsonObject crystal = json(resources.resolve(Path.of(
                "data", "immortalstorage", "worldgen", "configured_feature", "spirit_crystal_ore.json")));
        assertOreTargets(iron, "immortalstorage:spirit_iron_ore", "immortalstorage:deepslate_spirit_iron_ore");
        assertOreTargets(crystal, "immortalstorage:spirit_crystal_ore",
                "immortalstorage:deepslate_spirit_crystal_ore");
    }

    @Test
    void targetMinerReadsFinalPlacedFeatureHoldersAndBasinReadsReloadableLootTables() throws IOException {
        Path workspace = locateWorkspace();
        Path generated = workspace.resolve(TARGET_ROOT).resolve(Path.of(
                "src", "main", "generated-java", "com", "immortalstorage", "immortalstorage"));
        String scanner = Files.readString(generated.resolve(Path.of(
                "worldshard", "WorldShardOreScanner.java")));
        assertTrue(scanner.contains("placed.getFeatures().forEach(configured ->"));
        assertTrue(scanner.contains("configured.value().config()"));
        assertTrue(scanner.contains("registryAccess.lookupOrThrow(Registries.BIOME)"));
        assertTrue(scanner.contains("biomeRegistry.getTagOrEmpty"));
        assertFalse(scanner.contains("configured.config()"),
                "the old direct ConfiguredFeature accessor cannot read 26.1 holders");

        String basin = Files.readString(generated.resolve(Path.of(
                "block", "entity", "TreasureBasinBlockEntity.java")));
        assertTrue(basin.contains("WorldShardLootCatalog.active()"));
        assertTrue(basin.contains("reloadableRegistries().getLootTable("));
        assertTrue(basin.contains("ResourceKey.create(Registries.LOOT_TABLE"));
        assertTrue(basin.contains("table.getRandomItems(params, lootSeed)"));
    }

    @Test
    void targetDataOverlayIsExplicitlyCopiedAfterCanonicalResources() throws IOException {
        Path workspace = locateWorkspace();
        String build = Files.readString(workspace.resolve(Path.of(
                "project", "neoforge-1.21.1-mdk", "build.gradle")));
        assertTrue(build.contains("mc-26.1.2-nf-26.1.2.94/src/main/resources/data"));
        assertTrue(Files.isRegularFile(workspace.resolve(TARGET_ROOT).resolve(Path.of(
                "src", "main", "resources", "data", "immortalstorage",
                "dimension_type", "xianqiao_realm.json"))));
        assertTrue(Files.isRegularFile(workspace.resolve(TARGET_ROOT).resolve(Path.of(
                "src", "main", "resources", "data", "immortalstorage",
                "worldgen", "biome", "xianqiao_realm.json"))));
    }

    private static void assertOreTargets(JsonObject configured, String stone, String deepslate) {
        assertEquals("minecraft:ore", configured.get("type").getAsString());
        JsonArray targets = configured.getAsJsonObject("config").getAsJsonArray("targets");
        assertEquals(2, targets.size());
        assertEquals(stone, targets.get(0).getAsJsonObject().getAsJsonObject("state")
                .get("Name").getAsString());
        assertEquals(deepslate, targets.get(1).getAsJsonObject().getAsJsonObject("state")
                .get("Name").getAsString());
    }

    private static JsonObject json(Path file) throws IOException {
        assertTrue(Files.isRegularFile(file), "missing migration resource: " + file);
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static Path locateWorkspace() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve(TARGET_ROOT))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate workspace for 26.1.2 migration contracts");
    }
}
