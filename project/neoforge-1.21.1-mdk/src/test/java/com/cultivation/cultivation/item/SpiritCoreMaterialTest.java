package com.cultivation.cultivation.item;

import com.cultivation.cultivation.item.custom.SpiritCoreItem;
import com.cultivation.cultivation.player.yuan.YuanProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpiritCoreMaterialTest {
    @Test
    void coreIsAStackableInertIntermediateMaterial() throws Exception {
        Path root = locateMainSources();
        String registration = Files.readString(root.resolve("item/ModItems.java"));
        String item = Files.readString(root.resolve("item/custom/SpiritCoreItem.java"));
        assertTrue(registration.contains("new SpiritCoreItem(p.stacksTo(16))"));
        assertFalse(item.contains("inventoryTick"));
        assertFalse(item.contains("use("));
        assertEquals(YuanProfile.forStage(5, false), YuanProfile.forStage(5, true));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("src", "main", "java", "com", "cultivation", "cultivation"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate main sources");
    }
}
