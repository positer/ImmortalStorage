package com.cultivation.cultivation.client.guide;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class JadeGuideImmersionToneTest {
    @Test
    void progressionGuidanceDoesNotReadLikeADevelopmentChecklist() throws IOException {
        Path lang = locateLang();
        assertImmersive(lang.resolve("zh_cn.json"),
                new String[]{"任务一", "任务二", "任务三", "测试桶", "验证桶", "当前服务器", "Long.MAX_VALUE"});
        assertImmersive(lang.resolve("en_us.json"),
                new String[]{"Task 1:", "Task 2:", "Task 3:", "Test containers", "Verify buckets",
                        "this server uses", "Long.MAX_VALUE"});
    }

    private static void assertImmersive(Path file, String[] forbidden) throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        StringBuilder progression = new StringBuilder();
        json.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("guide.cultivation.jade.stage.")
                        || entry.getKey().startsWith("guide.cultivation.jade.chapter.progression."))
                .forEach(entry -> progression.append(entry.getValue().getAsString()).append('\n'));
        for (String phrase : forbidden) {
            assertFalse(progression.toString().contains(phrase), () -> file + " still contains plan-like phrase: " + phrase);
        }
    }

    private static Path locateLang() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("src/main/resources/assets/cultivation/lang");
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate language resources");
    }
}
