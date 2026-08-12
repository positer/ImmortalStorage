package com.immortalstorage.immortalstorage.item;

import com.immortalstorage.immortalstorage.item.custom.SpiritCoreItem;
import com.immortalstorage.immortalstorage.player.yuan.YuanProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpiritCoreMaterialTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

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
            Path candidate = current.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate main sources");
    }
}
