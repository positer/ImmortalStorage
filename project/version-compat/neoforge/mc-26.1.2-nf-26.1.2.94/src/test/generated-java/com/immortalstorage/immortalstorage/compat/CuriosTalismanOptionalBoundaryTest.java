package com.immortalstorage.immortalstorage.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CuriosTalismanOptionalBoundaryTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProject();

    @Test
    void alwaysLoadedCombatPathDoesNotLinkCuriosTypes() throws IOException {
        String service = read("src/main/java/com/immortalstorage/immortalstorage/combat/ImmortalMasterTalismanService.java");
        String bridge = read("src/main/java/com/immortalstorage/immortalstorage/compat/accessory/AccessoryTalismanBridge.java");
        assertTrue(service.contains("AccessoryTalismanBridge.isEquipped(entity)"));
        assertTrue(!service.contains("top.theillusivec4.curios"));
        assertTrue(!bridge.contains("import top.theillusivec4.curios"));
    }

    @Test
    void officialCuriosBridgeRegistersAndQueriesCharmSlot() throws IOException {
        String compat = read("src/main/java/com/immortalstorage/immortalstorage/compat/curios/CuriosTalismanCompat.java");
        String charmTag = read("src/main/resources/data/curios/tags/item/charm.json");
        assertTrue(compat.contains("CuriosApi.registerCurio"));
        assertTrue(compat.contains("getCuriosInventory"));
        assertTrue(compat.contains("findFirstCurio"));
        assertTrue(compat.contains("\"charm\""));
        assertTrue(charmTag.contains("immortalstorage:immortal_master_talisman"));
    }

    @Test
    void bothSupportedTargetsDeclareOfficialCuriosArtifacts() throws IOException {
        String matrix = Files.readString(PROJECT.resolve("../version-compat/compatibility-mod-matrix.json"));
        assertTrue(matrix.contains("maven.modrinth:curios:9.5.1+1.21.1"));
        assertTrue(matrix.contains("maven.modrinth:curios:15.0.0+26.1.2"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(PROJECT.resolve(relative));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project");
    }
}
