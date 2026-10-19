package com.immortalstorage.immortalstorage.compressor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The enchantment machines reuse the Spirit Forging shell and screen, so the handbook has to say
 * exactly where they differ and what deferred consumption means. These checks lock both locales
 * together, lock the 27-cell projection against the real blocks, and hold the pre-0.1.7 language
 * key set so a dropped key cannot hide behind a matching page count. The rule needles follow the
 * v1.2 user ruling: books are materials only, a split carrier book is consumed, and an uncollected
 * product is voided instead of paid for; the v1.3 fix round adds that breaking the block drops
 * the materials with it, wrench pick-up carries them inside the recovered item, losing the structure
 * ejects nothing, and a family swap never inherits the other family's face or Xianqiao configuration.
 */
final class EnchantmentHandbookContractTest {
    private static final List<String> LOCALES = List.of("en_us", "zh_cn");
    private static final String ENTRY_PATH =
            "assets/immortalstorage/patchouli_books/jade_guide/%s/entries/automation/enchantment_compressor.json";
    private static final String LANG_PATH = "assets/immortalstorage/lang/%s.json";
    private static final List<String> NEW_KEYS = List.of(
            "container.immortalstorage.enchantment_compressor",
            "container.immortalstorage.enchantment_decompressor");
    private static final Map<String, String> ENGLISH_KEYS = Map.of(
            "container.immortalstorage.enchantment_compressor", "Enchantment Compressor",
            "container.immortalstorage.enchantment_decompressor", "Enchantment Decompressor");
    private static final Map<String, String> CHINESE_KEYS = Map.of(
            "container.immortalstorage.enchantment_compressor", "附魔压缩机",
            "container.immortalstorage.enchantment_decompressor", "附魔解压器");
    // Count and digest of the sorted key set the 0.1.6 language files shipped with, which is what
    // remains once the two new keys are removed: the 0.1.7 spirit_compressor.pending key was retired
    // before release after no code referenced it. A deliberate language change must update both
    // numbers; an accidental delete or rename breaks the digest.
    private static final int BASELINE_KEY_COUNT = 729;
    private static final String BASELINE_KEY_DIGEST =
            "c30de4b4324d8f604441cedcdfa099055222f3a5c3debdc8909f67b2a1e36ee6";
    private static final String[] LAYER_ANCHORS = {
            "immortalstorage:world_shard_miner",
            "immortalstorage:miniature_immortal_ruin_block",
            "minecraft:enchanting_table"};

    @Test
    void entrySitsRightAfterSpiritForgingInBothLocales() throws Exception {
        for (String locale : LOCALES) {
            JsonObject entry = entry(locale);
            assertEquals("immortalstorage:automation", entry.get("category").getAsString(), locale);
            assertEquals("immortalstorage:miniature_immortal_ruin", entry.get("icon").getAsString(), locale);
            assertEquals(20, entry.get("sortnum").getAsInt(), locale + " must sort right after spirit_compressor at 19");
        }
        assertEquals("Enchantment Compressor", entry("en_us").get("name").getAsString());
        assertEquals("附魔压缩机", entry("zh_cn").get("name").getAsString());
    }

    @Test
    void bothLocalesCarryTheSameTwelvePageSequence() throws Exception {
        JsonArray english = entry("en_us").getAsJsonArray("pages");
        JsonArray chinese = entry("zh_cn").getAsJsonArray("pages");
        assertEquals(12, english.size());
        assertEquals(12, chinese.size());
        List<String> types = new ArrayList<>();
        for (int page = 0; page < english.size(); page++) {
            JsonObject en = english.get(page).getAsJsonObject();
            JsonObject zh = chinese.get(page).getAsJsonObject();
            types.add(en.get("type").getAsString());
            assertEquals(en.get("type").getAsString(), zh.get("type").getAsString(), "page " + page + " type");
            assertEquals(en.keySet(), zh.keySet(), "page " + page + " fields");
            assertEquals(en.has("enable_visualize") && en.get("enable_visualize").getAsBoolean(),
                    zh.has("enable_visualize") && zh.get("enable_visualize").getAsBoolean(),
                    "page " + page + " visualization flag");
        }
        assertEquals(List.of("patchouli:text", "patchouli:multiblock", "patchouli:multiblock",
                "patchouli:multiblock", "patchouli:multiblock", "patchouli:multiblock", "patchouli:text",
                "patchouli:text", "patchouli:text", "patchouli:text", "patchouli:text", "patchouli:text"), types);
    }

    @Test
    void projectionPagesMatchTheRealEnchantingTableStructureInBothLocales() throws Exception {
        for (String locale : LOCALES) {
            JsonArray pages = entry(locale).getAsJsonArray("pages");
            for (int mode = 0; mode < 2; mode++) {
                JsonObject page = pages.get(mode + 1).getAsJsonObject();
                assertEquals("patchouli:multiblock", page.get("type").getAsString());
                assertTrue(page.get("enable_visualize").getAsBoolean(), "the full structure must project");
                JsonObject shape = page.getAsJsonObject("multiblock");
                JsonArray pattern = shape.getAsJsonArray("pattern");
                JsonObject mapping = shape.getAsJsonObject("mapping");
                assertEquals("minecraft:enchanting_table", mapping.get("T").getAsString(),
                        "the Treasure Basin must be replaced by an Enchanting Table");
                assertEquals('0', pattern.get(2).getAsJsonArray().get(1).getAsString().charAt(1));
                assertEquals("immortalstorage:world_shard_miner", mapping.get("0").getAsString());
                assertTrue(mapping.get("R").getAsString().endsWith("[reversed=" + (mode == 1) + "]"), locale);
                int filled = 0;
                for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                    String encoded = encodedCell(mapping, pattern, x, y, z);
                    String actual = encoded.split("\\[")[0];
                    assertEquals(expectedBlock(x, y, z), actual,
                            locale + " projection at " + x + "," + y + "," + z);
                    if (actual.equals("minecraft:chain")) {
                        String axis = y == 0 ? "y" : x == 0 ? "x" : "z";
                        assertTrue(encoded.contains("axis=" + axis),
                                locale + " chain orientation at " + x + "," + y + "," + z);
                    }
                    if (!actual.equals("minecraft:air")) filled++;
                }
                assertEquals(23, filled);
            }
            for (int layer = 0; layer < 3; layer++) {
                JsonObject page = pages.get(layer + 3).getAsJsonObject();
                assertFalse(page.get("enable_visualize").getAsBoolean(),
                        "a single layer must not project a partial structure");
                JsonObject shape = page.getAsJsonObject("multiblock");
                assertEquals(1, shape.getAsJsonArray("pattern").size());
                assertEquals(LAYER_ANCHORS[layer], shape.getAsJsonObject("mapping").get("0").getAsString(),
                        locale + " layer " + layer);
            }
        }
    }

    @Test
    void theNewLanguageKeysCarryTheContractText() throws Exception {
        JsonObject english = lang("en_us");
        JsonObject chinese = lang("zh_cn");
        for (String key : NEW_KEYS) {
            assertTrue(english.has(key), "en_us missing " + key);
            assertTrue(chinese.has(key), "zh_cn missing " + key);
        }
        ENGLISH_KEYS.forEach((key, text) -> assertEquals(text, english.get(key).getAsString(), key));
        CHINESE_KEYS.forEach((key, text) -> assertEquals(text, chinese.get(key).getAsString(), key));
    }

    @Test
    void noExistingLanguageKeyWasDroppedFromEitherLocale() throws Exception {
        JsonObject english = lang("en_us");
        JsonObject chinese = lang("zh_cn");
        int expected = BASELINE_KEY_COUNT + NEW_KEYS.size();
        assertEquals(expected, english.size(), "en_us key count changed instead of growing by the two new keys");
        assertEquals(expected, chinese.size(), "zh_cn key count changed instead of growing by the two new keys");
        List<String> englishKeys = new ArrayList<>(english.keySet());
        List<String> chineseKeys = new ArrayList<>(chinese.keySet());
        assertEquals(englishKeys, chineseKeys, "both locales must declare their keys in the same order");
        List<String> retained = englishKeys.stream().filter(key -> !NEW_KEYS.contains(key)).toList();
        assertEquals(BASELINE_KEY_COUNT, retained.size(), "the pre-0.1.7 key count changed");
        assertEquals(BASELINE_KEY_DIGEST, keySetDigest(retained),
                "the pre-0.1.7 key set changed: update the digest only with a deliberate language change");
        retained.forEach(key -> assertFalse(english.get(key).getAsString().isEmpty(), key + " lost its text"));
    }

    @Test
    void theHandbookStatesEveryFrozenRuleInBothLanguages() throws Exception {
        String chinese = resourceText(String.format(ENTRY_PATH, "zh_cn"));
        String english = resourceText(String.format(ENTRY_PATH, "en_us"));
        assertRule(chinese, english, "上层中心的聚宝盆换成附魔台",
                "an Enchanting Table replaces the Treasure Basin at the top centre");
        assertRule(chinese, english, "换族时两套配置互不影响", "A family swap keeps the two families apart");
        assertRule(chinese, english, "新启用的那一族不会继承另一族的六面或仙窍设置",
                "the newly selected family never inherits the other one's six-face or Xianqiao settings");
        assertRule(chinese, english, "它从全部面关闭、仙窍输出关闭开始",
                "starts with every face closed and Xianqiao output off");
        assertRule(chinese, english, "只移除核心上方方块时不会弹出任何东西",
                "removing the block above the core ejects nothing and only stops the machine");
        assertRule(chinese, english, "反转核心或主动换族会作废未取走的产物并弹出缓存",
                "Reversing the core or swapping the family discards any uncollected product and ejects caches");
        assertRule(chinese, english, "右键成型结构中心打开与锻灵机完全相同的界面",
                "Right-click the formed centre for the same screen as Spirit Forging");
        assertRule(chinese, english, "主动模式每面每tick共64件",
                "active transfers share a 64-item budget per face per tick");
        assertRule(chinese, english, "仙窍输出把产物在同一tick直接写入放置者的仙窍",
                "Direct Xianqiao output writes products into the placer's Xianqiao in the same tick");
        assertRule(chinese, english, "只有真正取走产物才扣料",
                "Materials are deducted only when the result is taken");
        assertRule(chinese, english, "输入槽在服务端拒绝手动取出",
                "its input slots refuse manual pickup on the server");
        assertRule(chinese, english, "破坏方块会作废产物且不掉落",
                "Breaking the block voids the product and nothing drops");
        assertRule(chinese, english, "材料原样留在输入槽并随方块掉落在地面",
                "the materials stay in the input slots and drop on the ground with the block");
        assertRule(chinese, english, "材料随回收的迷你仙墟物品带走",
                "the materials travel inside the recovered Miniature Immortal Ruin item");
        assertRule(chinese, english, "结构因此失效时不再弹出任何东西", "ejects nothing at all");
        assertRule(chinese, english, "只有主动换族或反转核心才会弹出缓存",
                "Only a deliberate family swap or a core reversal ejects caches");
        assertRule(chinese, english, "换族会弹出被换下那一族的库存",
                "a swap returning the stock of the family left behind");
        assertRule(chinese, english, "附魔书只作材料", "Enchanted books are materials only");
        assertRule(chinese, english, "序号最小的非书物品", "the target is the lowest-index non-book item");
        assertRule(chinese, english, "把所有书槽整合成一本附魔书",
                "every book stack merges into a single enchanted book");
        assertRule(chinese, english, "同名附魔只取最高等级、不做相加",
                "the same enchantment keeps its highest level and never sums levels");
        assertRule(chinese, english, "N≤11时只输出N本单附魔附魔书",
                "at N of 11 or fewer the output is N single-enchantment books");
        assertRule(chinese, english, "载体若是附魔书则被消耗、不返还普通书",
                "a carrier book is consumed and never returned as a plain book");
        assertRule(chinese, english, "载体若是物品则始终返还", "an item carrier is always returned");
        assertRule(chinese, english, "书则成为带剩余附魔的复合附魔书",
                "a composite enchanted book holding those enchantments");
        assertRule(chinese, english, "主色0x80FF20，淡出0xE8FFB0", "0x80FF20 fading to 0xE8FFB0");
        assertRule(chinese, english, "JEI/EMI不为其新增分类", "JEI/EMI gain no category for them");
    }

    private static void assertRule(String chinese, String english, String chineseRule, String englishRule) {
        assertTrue(chinese.contains(chineseRule), "zh_cn lost: " + chineseRule);
        assertTrue(english.contains(englishRule), "en_us lost: " + englishRule);
    }

    /** Patchouli DenseMultiblock.build stores serialized layer i at height - i - 1, so the top layer is first. */
    private static String encodedCell(JsonObject mapping, JsonArray pattern, int x, int y, int z) {
        String character = String.valueOf(pattern.get(1 - y).getAsJsonArray().get(x + 1).getAsString().charAt(z + 1));
        assertNotNull(mapping.get(character), "the pattern uses the unmapped character " + character);
        return mapping.get(character).getAsString();
    }

    private static String expectedBlock(int x, int y, int z) {
        if (y == 0) {
            if (x == 0 && z == 0) return "immortalstorage:miniature_immortal_ruin_block";
            if (x != 0 && z != 0) return "minecraft:chain";
            return "minecraft:air";
        }
        if (x == 0 && z == 0) return y < 0 ? "immortalstorage:world_shard_miner" : "minecraft:enchanting_table";
        if (x != 0 && z != 0) return "immortalstorage:world_barrier";
        return "minecraft:chain";
    }

    private static String keySetDigest(List<String> keys) throws Exception {
        List<String> sorted = new ArrayList<>(keys);
        sorted.sort(String::compareTo);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(String.join("\n", sorted).getBytes(StandardCharsets.UTF_8)));
    }

    private static JsonObject entry(String locale) throws Exception {
        return JsonParser.parseString(resourceText(String.format(ENTRY_PATH, locale))).getAsJsonObject();
    }

    private static JsonObject lang(String locale) throws Exception {
        return JsonParser.parseString(resourceText(String.format(LANG_PATH, locale))).getAsJsonObject();
    }

    private static String resourceText(String path) throws Exception {
        try (var stream = EnchantmentHandbookContractTest.class.getResourceAsStream("/" + path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
