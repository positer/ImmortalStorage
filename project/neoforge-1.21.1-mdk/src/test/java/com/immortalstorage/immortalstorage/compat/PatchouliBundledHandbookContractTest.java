package com.immortalstorage.immortalstorage.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PatchouliBundledHandbookContractTest {
    private static final Path PROJECT_ROOT = locateProjectRoot();
    private static final Path MAIN_ROOT = PROJECT_ROOT.resolve("src/main");
    private static final Path GUIDE = MAIN_ROOT.resolve(
            "resources/assets/immortalstorage/patchouli_books/jade_guide");
    private static final List<String> STRUCTURAL_PAGE_FIELDS =
            List.of("type", "recipe", "recipe2", "source_recipe", "item", "flag");

    @Test
    void patchouliIsRequiredBundledAndUsesTheCurrentBookSchema() throws Exception {
        String build = Files.readString(PROJECT_ROOT.resolve("build.gradle"));
        String mods = Files.readString(PROJECT_ROOT.resolve(
                "build/resources/main/META-INF/neoforge.mods.toml"));
        JsonObject book = parse(GUIDE.resolve("book.json"));
        assertTrue(build.contains("jarJar('vazkii.patchouli:Patchouli:1.21.1-93-NEOFORGE')"));
        assertTrue(mods.contains("modId=\"patchouli\"\ntype=\"required\""));
        assertEquals("item.immortalstorage.jade_guide", book.get("name").getAsString());
        assertEquals(6, book.get("version").getAsInt());
        assertTrue(book.get("use_resource_pack").getAsBoolean());
    }

    @Test
    void ancientJadeHasOnlyThePatchouliEntryPoint() throws Exception {
        String item = Files.readString(MAIN_ROOT.resolve(
                "java/com/immortalstorage/immortalstorage/item/SimpleJadeGuideItem.java"));
        String payloads = Files.readString(MAIN_ROOT.resolve(
                "java/com/immortalstorage/immortalstorage/network/ModPayloads.java"));
        assertTrue(item.contains("PatchouliJadeGuideCompat.open(serverPlayer)"));
        assertFalse(item.contains("Class.forName"));
        assertFalse(payloads.contains("OpenJadeGuideScreen"));
        assertFalse(Files.exists(MAIN_ROOT.resolve(
                "java/com/immortalstorage/immortalstorage/client/screen/JadeGuideScreen.java")));
    }

    @Test
    void everyHandbookJsonParsesAndBilingualTreesStayInLockstep() throws Exception {
        try (var files = Files.walk(GUIDE)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                assertNotNull(parse(file), "invalid handbook json: " + file);
            }
        }

        for (String section : List.of("categories", "entries", "templates")) {
            Path zhRoot = GUIDE.resolve("zh_cn").resolve(section);
            Path enRoot = GUIDE.resolve("en_us").resolve(section);
            assertEquals(relativeJsonFiles(zhRoot), relativeJsonFiles(enRoot),
                    "bilingual " + section + " tree differs");
        }

        for (String relative : relativeJsonFiles(GUIDE.resolve("zh_cn/entries"))) {
            JsonArray zhPages = parse(GUIDE.resolve("zh_cn/entries").resolve(relative))
                    .getAsJsonArray("pages");
            JsonArray enPages = parse(GUIDE.resolve("en_us/entries").resolve(relative))
                    .getAsJsonArray("pages");
            assertEquals(zhPages.size(), enPages.size(), relative + " page count differs");
            for (int page = 0; page < zhPages.size(); page++) {
                JsonObject zh = zhPages.get(page).getAsJsonObject();
                JsonObject en = enPages.get(page).getAsJsonObject();
                for (String key : STRUCTURAL_PAGE_FIELDS) {
                    assertEquals(stringValue(zh, key), stringValue(en, key),
                            relative + " page " + page + " differs at " + key);
                }
            }
        }
    }

    @Test
    void everyPillAndImmortalFurnaceRecipeHasAVisibleRecipeSurface() throws Exception {
        Set<String> pills = structuredRecipeReferences(entry("zh_cn", "recipes/pills.json"));
        assertEquals(Set.of(
                "immortalstorage:crude_pill_embryo",
                "immortalstorage:refined_pill_embryo",
                "immortalstorage:breakthrough_pill_embryo_yuan",
                "immortalstorage:crude_pill_smelting",
                "immortalstorage:refined_pill_smelting",
                "immortalstorage:crude_pill_immortal_furnace",
                "immortalstorage:refined_pill_immortal_furnace",
                "immortalstorage:breakthrough_pill_smelting",
                "immortalstorage:immortal_pill_yuan",
                "immortalstorage:immortal_pill_xian"), pills);

        Set<String> furnace = structuredRecipeReferences(
                entry("zh_cn", "recipes/immortal_furnace_processes.json"));
        assertEquals(Set.of(
                "immortalstorage:crude_spirit_iron_immortal_furnace",
                "immortalstorage:spirit_iron_ore_immortal_furnace",
                "immortalstorage:deepslate_spirit_iron_ore_immortal_furnace",
                "immortalstorage:spirit_crystal_ore_immortal_furnace",
                "immortalstorage:deepslate_spirit_crystal_ore_immortal_furnace",
                "immortalstorage:crude_pill_immortal_furnace",
                "immortalstorage:refined_pill_immortal_furnace",
                "immortalstorage:breakthrough_pill_smelting",
                "immortalstorage:inactive_nurturing_crystal_bedrock_immortal_furnace",
                "immortalstorage:heavy_core_immortal_furnace",
                "immortalstorage:spirit_sword_tempering_immortal_furnace"), furnace);
    }

    @Test
    void everyBuiltInSourceVeinRecipeIsShown() throws Exception {
        Set<String> expected = new LinkedHashSet<>(List.of(
                "water_vein", "milk_vein", "lava_vein", "cobblestone_vein",
                "stone_vein_smelting", "smooth_stone_vein_smelting",
                "white_concrete_vein", "orange_concrete_vein", "magenta_concrete_vein",
                "light_blue_concrete_vein", "yellow_concrete_vein", "lime_concrete_vein",
                "pink_concrete_vein", "gray_concrete_vein", "light_gray_concrete_vein",
                "cyan_concrete_vein", "purple_concrete_vein", "blue_concrete_vein",
                "brown_concrete_vein", "green_concrete_vein", "red_concrete_vein",
                "black_concrete_vein", "dirt_vein", "oak_log_vein", "coal_vein",
                "copper_vein", "iron_vein", "gold_vein", "lapis_vein", "redstone_vein",
                "spirit_iron_vein", "spirit_crystal_vein", "diamond_vein", "emerald_vein",
                "echo_shard_vein", "ancient_debris_vein", "nether_star_vein",
                "enchanted_golden_apple_vein", "dragon_egg_vein"));
        expected = expected.stream().map(id -> "immortalstorage:" + id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertEquals(expected, structuredRecipeReferences(
                entry("zh_cn", "automation/sources.json")));
    }

    @Test
    void everyMainCreativeTabObjectIsIntroducedSomewhereInTheGuide() throws Exception {
        String handbook = allHandbookText();
        for (String id : List.of(
                "jade_guide", "true_yuan", "immortal_yuan", "crude_pill_embryo", "crude_pill",
                "refined_pill_embryo", "refined_pill", "breakthrough_pill_embryo",
                "breakthrough_pill", "immortal_pill", "ascension_dan", "white_day_thunder",
                "spirit_iron", "spirit_iron_nugget", "crude_spirit_iron", "spirit_crystal",
                "nurturing_crystal", "spirit_core", "premixed_heavy_compound",
                "substitute_puppet", "miniature_immortal_ruin", "qi_collecting_bottle",
                "disposable_qi_collecting_bottle", "primordial_qi", "spirit_sword",
                "immortal_ruin_forged_spirit_sword", "one_qi_returning_origin_sword",
                "soul_catcher", "spirit_staff", "spirit_drive", "dimensional_peeking_order",
                "dimensional_parallel_talisman", "great_thousand_world_parallel_edict",
                "xianqiao_exchange_cell", "xianqiao_rs_exchange_disk", "spirit_iron_ore",
                "spirit_crystal_ore", "deepslate_spirit_iron_ore",
                "deepslate_spirit_crystal_ore", "immortal_furnace",
                "simulated_reincarnation_furnace", "simulated_spirit_field", "energy_crystal",
                "mana_crystal", "source_crystal", "xianqiao_manager", "xianqiao_interface",
                "advanced_xianqiao_interface", "source_vein_manager", "world_shard_miner",
                "treasure_basin", "spirit_iron_block", "spirit_crystal_block",
                "inactive_nurturing_crystal_bedrock", "nurturing_crystal_bedrock",
                "small_nurturing_crystal_bud", "medium_nurturing_crystal_bud",
                "large_nurturing_crystal_bud", "nurturing_crystal_cluster",
                "stabilized_miniature_immortal_ruin",
                "entangled_stabilized_miniature_immortal_ruin",
                "advanced_stabilized_miniature_immortal_ruin",
                "advanced_entangled_stabilized_miniature_immortal_ruin",
                "crude_spirit_iron_block")) {
            assertTrue(handbook.contains("immortalstorage:" + id),
                    "handbook never introduces immortalstorage:" + id);
        }
    }

    @Test
    void handbookStatesTheMachineOutputAndAddonBoundariesPrecisely() throws Exception {
        assertContainsBoth("automation/simulated_machines.json",
                "整批结果直接原子提交到所属仙窍",
                "complete batch commits atomically to that owner's Xianqiao");
        assertContainsBoth("automation/simulated_machines.json",
                "不检查本地余量，也不产生溢出掉落",
                "without checking local capacity or creating overflow drops");
        assertContainsBoth("automation/simulated_machines.json",
                "结果会原样保留并停机等待",
                "result remains pending intact and processing waits");
        assertContainsBoth("automation/simulated_machines.json",
                "余量推向已开启且可接收的邻面",
                "excess through enabled accepting faces");
        assertContainsBoth("automation/simulated_machines.json",
                "优先回填临时缓存",
                "filled from the temporary cache first");
        assertContainsBoth("automation/miner_basin.json",
                "完全一致的附加设置页",
                "complete Treasure Basin settings surface");
        assertContainsBoth("automation/miner_basin.json",
                "倍率作用在一轮产物",
                "enlarge one result");
        assertContainsBoth("automation/miner_basin.json",
                "最终拒收部分原样写入持久临时缓存",
                "final rejected stack remains intact in a persistent temporary cache");
        assertContainsBoth("automation/sources.json",
                "源方块原有的仙元收费",
                "Existing Source Vein charging");
        assertContainsBoth("compat/viewers_storage.json",
                "AEKeyTypes", "AEKeyTypes");
        assertContainsBoth("compat/viewers_storage.json",
                "ResourceType与StorageType", "ResourceType and StorageType");
        assertContainsBoth("compat/viewers_storage.json",
                "回退键", "fallback key");
        assertContainsBoth("compat/viewers_storage.json",
                "非源方块额外存储不再执行仙元转化",
                "Non-source external storage no longer performs that conversion");
    }

    private static void assertContainsBoth(String relative, String zhNeedle, String enNeedle)
            throws Exception {
        assertTrue(Files.readString(GUIDE.resolve("zh_cn/entries").resolve(relative))
                .contains(zhNeedle), relative + " missing Chinese contract");
        assertTrue(Files.readString(GUIDE.resolve("en_us/entries").resolve(relative))
                .contains(enNeedle), relative + " missing English contract");
    }

    private static Set<String> structuredRecipeReferences(JsonObject entry) {
        Set<String> recipes = new LinkedHashSet<>();
        for (JsonElement element : entry.getAsJsonArray("pages")) {
            JsonObject page = element.getAsJsonObject();
            for (String key : List.of("recipe", "recipe2", "source_recipe")) {
                if (page.has(key) && page.get(key).isJsonPrimitive()) {
                    recipes.add(page.get(key).getAsString());
                }
            }
        }
        return recipes;
    }

    private static JsonObject entry(String locale, String relative) throws Exception {
        return parse(GUIDE.resolve(locale).resolve("entries").resolve(relative));
    }

    private static JsonObject parse(Path path) throws Exception {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String allHandbookText() throws Exception {
        StringBuilder text = new StringBuilder();
        try (var files = Files.walk(GUIDE)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                text.append(Files.readString(file, StandardCharsets.UTF_8)).append('\n');
            }
        }
        return text.toString();
    }

    private static String stringValue(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsString() : "";
    }

    private static Set<String> relativeJsonFiles(Path root) throws Exception {
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(root::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    private static Path locateProjectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))
                    && Files.isDirectory(current.resolve("src/main"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate the NeoForge project root");
    }
}
