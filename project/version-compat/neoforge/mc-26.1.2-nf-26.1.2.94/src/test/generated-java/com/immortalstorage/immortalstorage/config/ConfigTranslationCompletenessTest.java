package com.immortalstorage.immortalstorage.config;

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
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final List<String> CLIENT_KEYS = List.of("terminalRows", "syncRecipeViewerSearch");
    private static final List<String> KEYS = List.of(
            "loot", "loot.startWithJadeGuide", "loot.jadeGuideInVillageChests",
            "loot.jadeGuideChestChance", "loot.jadeGuideArchaeologyChance", "loot.villageRefinedPillChance",
            "loot.villageRefinedPillMin", "loot.villageRefinedPillMax",
            "loot.netherBreakthroughChance", "loot.netherBreakthroughMin",
            "loot.netherBreakthroughMax", "loot.endCityAscensionChance",
            "loot.endShipImmortalChance", "loot.ascensionDanChance",
            "source_blocks", "source_blocks.allowOtherPlayerClaim",
            "source_blocks.allowOtherPlayerBreak", "source_blocks.allowMobBreak",
            "xianqiao_interface", "xianqiao_interface.xianqiaoInterfaceItemSlotLimit",
            "xianqiao_interface.xianqiaoInterfaceFluidSlotLimitMb",
            "spirit_staff", "spirit_staff.buildLimit",
            "immortal_ruin_sword", "immortal_ruin_sword.affectsOtherPlayers",
            "primordial_qi", "primordial_qi.entityBlacklist", "progression",
            "progression.maximumStage", "progression.stageTenInfiniteImmortalYuan",
            "progression.tribulation_targets", "progression.tribulation_targets.stage6To7",
            "progression.tribulation_targets.stage7To8", "progression.tribulation_targets.stage8To9",
            "progression.tribulation_targets.stage9To10",
            "energy_crystal", "energy_crystal.feCapacity", "energy_crystal.fePerTick",
            "resource_conversion", "resource_conversion.fe", "resource_conversion.botaniaMana",
            "resource_conversion.arsSource", "resource_conversion.fe.enabled",
            "resource_conversion.botaniaMana.enabled", "resource_conversion.arsSource.enabled",
            "resource_conversion.fe.resourcePerImmortalYuan",
            "resource_conversion.botaniaMana.resourcePerImmortalYuan",
            "resource_conversion.arsSource.resourcePerImmortalYuan",
            "resource_conversion.fe.maximumConversionPerTick",
            "resource_conversion.botaniaMana.maximumConversionPerTick",
            "resource_conversion.arsSource.maximumConversionPerTick");
    private static final int DYNAMIC_CONVERSION_KEYS = 12;

    @Test
    void everyCommonConfigSectionAndValueHasNaturalLanguageInBothLocales() throws IOException {
        Path lang = locateLang();
        for (String locale : List.of("zh_cn.json", "en_us.json")) {
            JsonObject json = JsonParser.parseString(Files.readString(lang.resolve(locale))).getAsJsonObject();
            for (String key : KEYS) {
                String full = "immortalstorage.configuration." + key;
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
        assertEquals(KEYS.size() - DYNAMIC_CONVERSION_KEYS, boundKeys.size(),
                "static config sections and values must bind one explicit translation key");
        for (String key : KEYS.subList(0, KEYS.size() - DYNAMIC_CONVERSION_KEYS)) {
            assertTrue(boundKeys.contains(key), () -> "missing explicit config translation binding for " + key);
        }
        assertTrue(source.contains("translation(key(\"resource_conversion.\" + name))"));
        assertTrue(source.contains("translation(key(\"resource_conversion.\" + name + \".enabled\"))"));
        assertTrue(source.contains("translation(key(\"resource_conversion.\" + name + \".resourcePerImmortalYuan\"))"));
        assertTrue(source.contains("translation(key(\"resource_conversion.\" + name + \".maximumConversionPerTick\"))"));
    }

    @Test
    void everyClientConfigValueHasTranslationAndTooltipInBothLocales() throws IOException {
        Path lang = locateLang();
        for (String locale : List.of("zh_cn.json", "en_us.json")) {
            JsonObject json = JsonParser.parseString(Files.readString(lang.resolve(locale))).getAsJsonObject();
            for (String key : CLIENT_KEYS) {
                String full = "immortalstorage.configuration." + key;
                assertTrue(json.has(full), () -> locale + " missing " + full);
                assertTrue(json.has(full + ".tooltip"), () -> locale + " missing " + full + ".tooltip");
            }
        }

        String source = Files.readString(locateClientSource());
        for (String key : CLIENT_KEYS) {
            assertTrue(source.contains("translation(key(\"" + key + "\"))"),
                    () -> "missing explicit client config translation binding for " + key);
        }
    }

    private static Path locateLang() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("src/main/resources/assets/immortalstorage/lang");
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate language resources");
    }

    private static Path locateSource() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("src/main/java/com/immortalstorage/immortalstorage/config/ImmortalStorageConfig.java");
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate ImmortalStorageConfig.java");
    }

    private static Path locateClientSource() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("src/main/java/com/immortalstorage/immortalstorage/config/ImmortalStorageClientConfig.java");
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate ImmortalStorageClientConfig.java");
    }
}
