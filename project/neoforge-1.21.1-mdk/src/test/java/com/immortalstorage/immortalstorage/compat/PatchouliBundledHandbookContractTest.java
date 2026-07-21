package com.immortalstorage.immortalstorage.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
    void handbookHasRootDefinitionAndVersionThreeEntries() throws Exception {
        JsonObject book = resource("assets/immortalstorage/patchouli_books/jade_guide/book.json");
        assertEqualsString("item.immortalstorage.jade_guide", book, "name");
        for (String entry : new String[]{
                "entries/recipes/nurturing_crystal.json",
                "entries/equipment/substitute_puppet.json",
                "entries/equipment/immortal_ruin_sword.json",
                "entries/automation/immortal_ruins.json"}) {
            assertNotNull(PatchouliBundledHandbookContractTest.class.getClassLoader().getResource(
                    "assets/immortalstorage/patchouli_books/jade_guide/zh_cn/" + entry));
            assertNotNull(PatchouliBundledHandbookContractTest.class.getClassLoader().getResource(
                    "assets/immortalstorage/patchouli_books/jade_guide/en_us/" + entry));
        }
    }

    private static JsonObject resource(String path) throws Exception {
        var stream = PatchouliBundledHandbookContractTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
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
