package com.immortalstorage.immortalstorage.dimension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PersonalRealmBedRespawnContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void personalRealmAllowsBedsAndRestoresDynamicRespawnLevelOnLogin() throws Exception {
        Path main = locateMain();
        String dimensionType = Files.readString(main.resolve(
                "resources/data/immortalstorage/dimension_type/xianqiao_realm.json"));
        String helper = Files.readString(main.resolve(
                "java/com/immortalstorage/immortalstorage/dimension/RealmHelper.java"));
        String events = Files.readString(main.resolve(
                "java/com/immortalstorage/immortalstorage/event/CommonEvents.java"));

        assertTrue(dimensionType.contains("\"bed_works\": true"));
        assertFalse(dimensionType.contains("\"bed_works\": false"));
        assertFalse(dimensionType.contains("\"fixed_time\""),
                "The client sky must follow the realm's locked day/night time");
        assertTrue(helper.contains("ensureRespawnRealmRegistered(ServerPlayer player)"));
        assertTrue(helper.contains("player.getRespawnDimension()"));
        assertTrue(helper.contains("PersonalRealmLevelFactory.getOrCreate"));
        assertTrue(events.contains("RealmHelper.ensureRespawnRealmRegistered(p)"));
    }

    private static Path locateMain() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("src", "main"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate main sources");
    }
}
