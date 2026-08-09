package com.immortalstorage.immortalstorage.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PatchouliBundledHandbookContractTest {
    private static final Path PROJECT_ROOT = locateProjectRoot();
    private static final Path ROOT = PROJECT_ROOT.resolve("src/main");

    @Test
    void patchouliIsRequiredAndBundled() throws Exception {
        String build = Files.readString(PROJECT_ROOT.resolve("build.gradle"));
        String mods = Files.readString(ROOT.resolve("resources/META-INF/neoforge.mods.toml"));
        assertTrue(build.contains("jarJar('vazkii.patchouli:Patchouli:1.21.1-93-NEOFORGE')"));
        assertTrue(mods.contains("modId=\"patchouli\"\ntype=\"required\""));
    }

    @Test
    void ancientJadeHasOnlyThePatchouliEntryPoint() throws Exception {
        String item = Files.readString(ROOT.resolve(
                "java/com/immortalstorage/immortalstorage/item/SimpleJadeGuideItem.java"));
        String payloads = Files.readString(ROOT.resolve(
                "java/com/immortalstorage/immortalstorage/network/ModPayloads.java"));
        assertTrue(item.contains("PatchouliJadeGuideCompat.open(serverPlayer)"));
        assertFalse(item.contains("Class.forName"));
        assertFalse(payloads.contains("OpenJadeGuideScreen"));
        assertFalse(Files.exists(ROOT.resolve(
                "java/com/immortalstorage/immortalstorage/client/screen/JadeGuideScreen.java")));
        Path legacyGuideDir = ROOT.resolve("java/com/immortalstorage/immortalstorage/client/guide");
        assertTrue(!Files.exists(legacyGuideDir)
                || Files.list(legacyGuideDir).findAny().isEmpty());
    }

    @Test
    void handbookHasRootDefinitionAndAllPostVersionThreeEntries() throws Exception {
        JsonObject book = resource("assets/immortalstorage/patchouli_books/jade_guide/book.json");
        assertEqualsString("item.immortalstorage.jade_guide", book, "name");
        for (String entry : new String[]{
                "entries/recipes/nurturing_crystal.json",
                "entries/equipment/substitute_puppet.json",
                "entries/equipment/immortal_ruin_sword.json",
                "entries/equipment/one_qi_returning_origin_sword.json",
                "entries/equipment/soul_catcher.json",
                "entries/automation/immortal_ruins.json",
                "entries/automation/simulated_machines.json",
                "entries/storage/spirit_drive.json"}) {
            assertNotNull(PatchouliBundledHandbookContractTest.class.getClassLoader().getResource(
                    "assets/immortalstorage/patchouli_books/jade_guide/zh_cn/" + entry));
            assertNotNull(PatchouliBundledHandbookContractTest.class.getClassLoader().getResource(
                    "assets/immortalstorage/patchouli_books/jade_guide/en_us/" + entry));
        }
    }

    @Test
    void bilingualEntryTreesStayInSync() throws Exception {
        Path guide = ROOT.resolve("resources/assets/immortalstorage/patchouli_books/jade_guide");
        assertEquals(relativeJsonFiles(guide.resolve("zh_cn/entries")),
                relativeJsonFiles(guide.resolve("en_us/entries")));
    }

    @Test
    void handbookCoversEveryUserFacingSystemAddedAfterVersionThree() throws Exception {
        String[][] coverage = {
                {"entries/recipes/primordial_qi.json", "qi_collecting_bottle", "primordial_qi"},
                {"entries/equipment/one_qi_returning_origin_sword.json",
                        "one_qi_returning_origin_sword", "64"},
                {"entries/equipment/soul_catcher.json", "soul_catcher"},
                {"entries/automation/simulated_machines.json",
                        "simulated_reincarnation_furnace", "simulated_spirit_field"},
                {"entries/automation/immortal_ruins.json",
                        "entangled_stabilized_miniature_immortal_ruin",
                        "advanced_stabilized_miniature_immortal_ruin",
                        "advanced_entangled_stabilized_miniature_immortal_ruin"},
                {"entries/automation/sources.json", "echo_shard_vein", "72"},
                {"entries/automation/advanced_interface.json", "advanced_xianqiao_interface"},
                {"entries/storage/xianqiao_manager.json", "xianqiao_manager", "Create", "Building Gadgets"},
                {"entries/storage/spirit_drive.json", "spirit_drive", "UUID"},
                {"entries/storage/furnace_realm.json", "immortal_furnace", "0.0.8", "UUID"},
                {"entries/automation/miner_basin.json", "world_shard_miner", "world_shard_loot", "2400"},
                {"entries/compat/viewers_storage.json", "AE2", "RS", "ExtraStorage", "Building Gadgets", "Create", "Beyond Dimensions"},
                {"entries/equipment/sword.json", "0%", "1.5%"},
                {"entries/equipment/one_qi_returning_origin_sword.json", "0%", "10"}
        };
        for (String locale : new String[]{"zh_cn", "en_us"}) {
            for (String[] contract : coverage) {
                String text = resourceText("assets/immortalstorage/patchouli_books/jade_guide/"
                        + locale + "/" + contract[0]);
                for (int i = 1; i < contract.length; i++) {
                    assertTrue(text.contains(contract[i]),
                            locale + "/" + contract[0] + " missing " + contract[i]);
                }
            }
        }
    }

    private static JsonObject resource(String path) throws Exception {
        var stream = PatchouliBundledHandbookContractTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String resourceText(String path) throws Exception {
        var stream = PatchouliBundledHandbookContractTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Set<String> relativeJsonFiles(Path root) throws Exception {
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(root::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.toSet());
        }
    }

    private static void assertEqualsString(String expected, JsonObject object, String key) {
        assertTrue(object.has(key));
        assertTrue(expected.equals(object.get(key).getAsString()));
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
