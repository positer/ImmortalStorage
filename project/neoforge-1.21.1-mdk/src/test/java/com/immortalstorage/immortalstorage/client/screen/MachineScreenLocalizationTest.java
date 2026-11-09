package com.immortalstorage.immortalstorage.client.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

final class MachineScreenLocalizationTest {
    private static final Path PROJECT = locateProject();
    private static final Pattern DIRECT_KEY = Pattern.compile("Component\\.translatable\\(\\s*\"((?:container|tooltip|block|item)\\.immortalstorage\\.[a-zA-Z0-9_.]+)\"\\s*[,)]");
    private static final Pattern FORMAT = Pattern.compile("%(?:\\d+\\$)?[a-zA-Z%]");

    @Test void visibleScreenTranslationKeysResolveInBothLanguages() throws Exception {
        JsonObject zh = language("immortalstorage", "zh_cn"), en = language("immortalstorage", "en_us");
        Set<String> keys = new HashSet<>();
        try (var files = Files.list(PROJECT.resolve("src/main/java/com/immortalstorage/immortalstorage/client/screen"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                var matcher = DIRECT_KEY.matcher(Files.readString(file));
                while (matcher.find()) keys.add(matcher.group(1));
            }
        }
        assertTrue(keys.size() > 50, "the audit must include real screen and tooltip keys");
        for (String key : keys) {
            assertTrue(zh.has(key), "missing Chinese: " + key);
            assertTrue(en.has(key), "missing English: " + key);
        }
    }

    @Test void bothLanguagesHaveTheSameKeysAndCompatibleFormatArguments() throws Exception {
        for (String namespace : List.of("immortalstorage", "nemos_inventory_sorting")) {
            JsonObject zh = language(namespace, "zh_cn"), en = language(namespace, "en_us");
            assertEquals(zh.keySet(), en.keySet(), namespace + " language key parity");
            for (String key : zh.keySet()) {
                assertFalse(zh.get(key).getAsString().isBlank(), key);
                assertFalse(en.get(key).getAsString().isBlank(), key);
                assertEquals(formats(zh.get(key).getAsString()), formats(en.get(key).getAsString()), key);
            }
        }
    }

    @Test void dynamicFacesForcesAndExternalFilterHaveLocalizedText() throws Exception {
        JsonObject zh = language("immortalstorage", "zh_cn"), en = language("immortalstorage", "en_us");
        String[] faces = {"up", "down", "north", "south", "west", "east"};
        String[] chinese = {"上", "下", "北", "南", "西", "东"};
        String[] english = {"U", "D", "N", "S", "W", "E"};
        for (int i = 0; i < faces.length; i++) {
            String key = "container.immortalstorage.configuration.face." + faces[i];
            assertEquals(chinese[i], zh.get(key).getAsString());
            assertEquals(english[i], en.get(key).getAsString());
        }
        for (String mode : List.of("none", "light", "medium", "strong", "teleport")) {
            String key = "container.immortalstorage.miniature_ruin.force." + mode;
            assertTrue(hasChinese(zh.get(key).getAsString()), key);
            assertFalse(hasChinese(en.get(key).getAsString()), key);
        }
        JsonObject nemoZh = language("nemos_inventory_sorting", "zh_cn");
        for (String key : nemoZh.keySet()) assertTrue(hasChinese(nemoZh.get(key).getAsString()), key);
    }

    private static boolean hasChinese(String text) {
        return text.codePoints().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN);
    }
    private static List<String> formats(String text) {
        var result = new ArrayList<String>(); var matches = FORMAT.matcher(text);
        while (matches.find()) if (!matches.group().equals("%%")) result.add(matches.group());
        result.sort(String::compareTo); return result;
    }
    private static JsonObject language(String namespace, String locale) throws Exception {
        return JsonParser.parseString(Files.readString(PROJECT.resolve("src/main/resources/assets/" + namespace + "/lang/" + locale + ".json"))).getAsJsonObject();
    }
    private static Path locateProject() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve("src/main/resources/assets/immortalstorage/lang"))) return p;
            Path mdk = p.resolve("project/neoforge-1.21.1-mdk");
            if (Files.isDirectory(mdk.resolve("src/main/resources/assets/immortalstorage/lang"))) return mdk;
        }
        throw new IllegalStateException("ImmortalStorage language resources not found");
    }
}
