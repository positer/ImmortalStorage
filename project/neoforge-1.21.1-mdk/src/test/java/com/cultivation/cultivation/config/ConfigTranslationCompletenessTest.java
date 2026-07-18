package com.cultivation.cultivation.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigTranslationCompletenessTest {
    private static final List<String> KEYS = List.of(
            "loot", "loot.startWithJadeGuide", "loot.jadeGuideInVillageChests",
            "loot.jadeGuideChestChance", "loot.villageRefinedPillChance",
            "loot.villageRefinedPillMin", "loot.villageRefinedPillMax",
            "loot.netherBreakthroughChance", "loot.netherBreakthroughMin",
            "loot.netherBreakthroughMax", "loot.endCityAscensionChance",
            "loot.endShipImmortalChance", "loot.ascensionDanChance",
            "source_blocks", "source_blocks.allowOtherPlayerClaim",
            "source_blocks.allowOtherPlayerBreak", "source_blocks.allowMobBreak",
            "xianqiao_interface", "xianqiao_interface.xianqiaoInterfaceItemSlotLimit",
            "xianqiao_interface.xianqiaoInterfaceFluidSlotLimitMb",
            "spirit_staff", "spirit_staff.buildLimit", "progression",
            "progression.maximumStage", "progression.stageTenInfiniteImmortalYuan",
            "progression.tribulation_targets", "progression.tribulation_targets.stage6To7",
            "progression.tribulation_targets.stage7To8", "progression.tribulation_targets.stage8To9",
            "progression.tribulation_targets.stage9To10");

    @Test
    void everyCommonConfigSectionAndValueHasNaturalLanguageInBothLocales() throws IOException {
        Path lang = locateLang();
        for (String locale : List.of("zh_cn.json", "en_us.json")) {
            JsonObject json = JsonParser.parseString(Files.readString(lang.resolve(locale))).getAsJsonObject();
            for (String key : KEYS) {
                String full = "cultivation.configuration." + key;
                assertTrue(json.has(full), () -> locale + " missing " + full);
                if (key.contains(".")) {
                    assertTrue(json.has(full + ".tooltip"), () -> locale + " missing " + full + ".tooltip");
                }
            }
        }
    }

    @Test
    void everyNestedConfigEntryExplicitlyBindsItsFullTranslationPath() throws IOException {
        String source = Files.readString(locateSource());
        Pattern binding = Pattern.compile("translation\\(key\\(\\\"([^\\\"]+)\\\"\\)\\)");
        List<String> boundKeys = binding.matcher(source).results().map(match -> match.group(1)).toList();
        assertEquals(KEYS.size(), boundKeys.size(), "every current config section/value must bind one explicit translation key");
        for (String key : KEYS) {
            assertTrue(boundKeys.contains(key), () -> "missing explicit config translation binding for " + key);
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

    private static Path locateSource() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("src/main/java/com/cultivation/cultivation/config/CultivationConfig.java");
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate CultivationConfig.java");
    }
}
