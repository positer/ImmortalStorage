package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class MachineRedstonePlacementContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test void productionMachinesPutRedstoneControlInsideSettingsPanel() throws Exception {
        for (String name : new String[]{"SimulatedSpiritFieldScreen.java",
                "SimulatedReincarnationFurnaceScreen.java", "EnergyCrystalScreen.java"}) {
            String source = Files.readString(screen(name));
            assertTrue(source.contains("settingsWidgets.add(redstoneModeButton)"), name);
            assertTrue(source.contains("leftPos + imageWidth + 16, topPos + 18"), name);
        }
        String basin = Files.readString(screen("TreasureBasinScreen.java"));
        assertTrue(basin.contains("redstoneModeButton.visible = settingsOpen"));
    }

    @Test void advancedStabilizedRuinRedstoneOccupiesTopOfSettingsPanelWithoutMovingSchedulingControls() throws Exception {
        String source = Files.readString(screen("AdvancedStabilizedMiniatureImmortalRuinScreen.java"));
        assertTrue(source.contains("redstoneButtonY() { return topPos + 2; }"));
        assertTrue(source.contains("topPos + 226"));

        String entangled = Files.readString(screen("AdvancedEntangledMiniatureRuinScreen.java"));
        assertTrue(entangled.contains("leftPos + imageWidth + 6, topPos + 2, menu"));
        assertTrue(entangled.contains("leftPos + imageWidth + 98, topPos + 4"));
        assertTrue(entangled.contains("topPos + 228"));
    }

    private static Path screen(String name) {
        for (Path cursor = Path.of("").toAbsolutePath(); cursor != null; cursor = cursor.getParent()) {
            Path candidate = cursor.resolve("src/main/java/com/immortalstorage/immortalstorage/client/screen/" + name);
            if (Files.isRegularFile(candidate)) return candidate;
            candidate = cursor.resolve("project/neoforge-1.21.1-mdk/src/main/java/com/immortalstorage/immortalstorage/client/screen/" + name);
            if (Files.isRegularFile(candidate)) return candidate;
        }
        throw new IllegalStateException(name);
    }
}
