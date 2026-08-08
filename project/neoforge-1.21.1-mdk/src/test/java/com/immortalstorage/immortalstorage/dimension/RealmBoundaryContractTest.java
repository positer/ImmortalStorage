package com.immortalstorage.immortalstorage.dimension;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class RealmBoundaryContractTest {
    @Test
    void finiteRealmsDrawAWorldBorderAndClampPlayersWithoutBarrierBlocks() throws IOException {
        Path main = locateMainSources();
        String helper = Files.readString(main.resolve("dimension/RealmHelper.java"));
        String events = Files.readString(main.resolve("event/CommonEvents.java"));
        String generator = Files.readString(main.resolve("dimension/XianqiaoRealmChunkGenerator.java"));

        assertTrue(helper.contains("stage < 6 || stage >= 9"));
        assertTrue(helper.contains("Mth.clamp(player.getX()"));
        assertTrue(helper.contains("Mth.clamp(player.getZ()"));
        assertTrue(helper.contains("player.teleportTo(realm"));
        assertTrue(helper.contains("realm.getWorldBorder().setCenter(8.0D, 8.0D)"));
        assertTrue(helper.contains("realm.getWorldBorder().setSize(size)"));
        assertTrue(helper.contains("stage >= 6 && stage < 9"));
        assertTrue(helper.contains("APPLIED_BORDER_SIZE"));
        assertTrue(helper.contains("message.immortalstorage.realm_boundary_reached"));
        assertTrue(helper.contains("BOUNDARY_NOTICE_COOLDOWN_TICKS"));
        assertTrue(events.contains("RealmHelper.enforcePlayerBoundary(p)"));
        assertTrue(events.contains("RealmHelper.isInOwnRealm(p)"));
        assertTrue(!generator.contains("Blocks.BARRIER"));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("src", "main", "java", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate main sources");
    }
}
