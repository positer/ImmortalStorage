package com.immortalstorage.immortalstorage.item;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the user-facing pick-mode rename stable without changing its saved mode id. */
final class SpiritStaffModeNameContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProject();

    @Test
    void miningNameKeepsTheStableInternalModeSlot() throws Exception {
        String staff = source("src/main/java/com/immortalstorage/immortalstorage/item/custom/SpiritStaffItem.java");
        String zh = source("src/main/resources/assets/immortalstorage/lang/zh_cn.json");
        String en = source("src/main/resources/assets/immortalstorage/lang/en_us.json");

        assertTrue(staff.contains("MODE_PICK = 2"), "the numeric mode id must remain save-compatible");
        assertTrue(staff.contains("mode.pick"), "the legacy translation key must remain stable");
        assertTrue(zh.contains("\"item.immortalstorage.spirit_staff.mode.pick\": \"挖掘\""));
        assertTrue(zh.contains("\"message.immortalstorage.spirit_staff.pick.stage\": \"挖掘模式精准采集需要达到六阶。\""));
        assertTrue(en.contains("\"item.immortalstorage.spirit_staff.mode.pick\": \"Mining\""));
        assertTrue(en.contains("\"message.immortalstorage.spirit_staff.pick.stage\": \"Mining mode silk harvesting requires cultivation stage 6.\""));
        assertFalse(zh.contains("\"item.immortalstorage.spirit_staff.mode.pick\": \"镐\""));
        assertFalse(en.contains("\"item.immortalstorage.spirit_staff.mode.pick\": \"Pick\""));
    }

    @Test
    void bilingualHandbookUsesMiningAsTheModeName() throws Exception {
        String zh = source("src/main/resources/assets/immortalstorage/patchouli_books/jade_guide/zh_cn/entries/equipment/instrument.json");
        String en = source("src/main/resources/assets/immortalstorage/patchouli_books/jade_guide/en_us/entries/equipment/instrument.json");

        assertTrue(zh.contains("探索、扳手、挖掘、建筑、传送五种模式"));
        assertTrue(zh.contains("挖掘模式保留下界合金等级"));
        assertFalse(zh.contains("探索、扳手、镐子、建筑、传送五种模式"));
        assertTrue(en.contains("Exploration, Wrench, Mining, Building and Teleport"));
        assertTrue(en.contains("Mining keeps netherite-level breaking"));
        assertFalse(en.contains("Exploration, Wrench, Pickaxe, Building and Teleport"));
    }

    private static String source(String relative) throws Exception {
        return Files.readString(PROJECT.resolve(relative));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))
                    && Files.isDirectory(current.resolve("src/main"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate NeoForge project");
    }
}
