package com.immortalstorage.immortalstorage.block;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class YuanLightContractTest {
    @Test
    void lightsAreRealBlockItemsInstantBreakDyeableAndNonDropping() throws Exception {
        Path root = locate("src/main/java/com/immortalstorage/immortalstorage");
        String block = Files.readString(root.resolve("block/custom/YuanLightBlock.java"));
        String items = Files.readString(root.resolve("item/ModItems.java"));
        String index = Files.readString(root.resolve("block/entity/YuanLightIndex.java"));
        assertTrue(block.contains("instabreak()"));
        assertTrue(block.contains("DyeItem"));
        assertTrue(block.contains("EnumProperty.create(\"color\""));
        assertTrue(block.contains("BooleanProperty.create(\"core_visible\")"));
        assertTrue(block.contains("state.cycle(CORE_VISIBLE)"));
        assertTrue(items.contains("new TrueYuanItem(ModBlocks.TRUE_YUAN_LIGHT.get()"));
        assertTrue(items.contains("new ImmortalYuanItem(ModBlocks.IMMORTAL_YUAN_LIGHT.get()"));
        assertTrue(index.contains("getChunkNow(center.x, center.z)"));
        assertTrue(index.contains("IMMORTAL_YUAN_LIGHT"));
        assertTrue(Files.readString(locate("src/main/resources/data/immortalstorage/loot_table/blocks")
                .resolve("true_yuan_light.json")).contains("\"pools\":[]"));
        assertTrue(Files.readString(locate("src/main/resources/data/immortalstorage/loot_table/blocks")
                .resolve("immortal_yuan_light.json")).contains("\"pools\":[]"));
    }

    private static Path locate(String relative) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relative);
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate " + relative);
    }
}
