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
        assertEquals("immortalstorage:xianqiao_realm", dimension.get("default_clock").getAsString(),
                "the personal realm must own an isolated world clock so its accelerated tick never advances the overworld clock");
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
        // 聚宝盆每周期从世界碎片战利品目录的预解析表读取，不再每次 roll 都查
        // reloadable registry；真实战利品表解析已下沉到 WorldShardLootCatalog。
        assertTrue(basin.contains("WorldShardLootCatalog.active().resolveLootTable("));
        assertFalse(basin.contains("reloadableRegistries().getLootTable("),
                "the basin must resolve loot from the eager catalog, not per-roll reloadable registries");
        assertTrue(basin.contains("table.getRandomItemsRaw(context, rolled::add)"));
        assertTrue(basin.contains("CommonHooks.modifyLoot(selected.lootTable(), rolled, context)"));

        String catalog = Files.readString(generated.resolve(Path.of(
                "worldshard", "WorldShardLootCatalog.java")));
        assertTrue(catalog.contains("ResourceKey.create(Registries.LOOT_TABLE, id)"),
                "the catalog must eagerly resolve each loot table from the LOOT_TABLE registry");
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
        assertTrue(Files.isRegularFile(workspace.resolve(TARGET_ROOT).resolve(Path.of(
                "src", "main", "resources", "data", "immortalstorage",
                "world_clock", "xianqiao_realm.json"))),
                "the isolated realm world clock must be present in the target resources");
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
