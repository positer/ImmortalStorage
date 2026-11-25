package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class MachineRedstonePlacementContractTest {
    @Test void productionMachinesPutRedstoneControlInsideSettingsPanel() throws Exception {
        for (String name : new String[]{"SimulatedSpiritFieldScreen.java",
                "SimulatedReincarnationFurnaceScreen.java", "EnergyCrystalScreen.java"}) {
            String source = Files.readString(screen(name));
            assertTrue(source.contains("settingsWidgets.add(redstoneModeButton)"), name);
            assertTrue(source.contains("leftPos + imageWidth + 14, topPos + 18"), name);
        }
        String basin = Files.readString(screen("TreasureBasinScreen.java"));
        assertTrue(basin.contains("redstoneModeButton.visible = settingsOpen"));
    }

    @Test void advancedRuinRedstoneStaysAboveTitleAndDoesNotOverlapSchedulingControls() throws Exception {
        String source = Files.readString(screen("AdvancedStabilizedMiniatureImmortalRuinScreen.java"));
        assertTrue(source.contains("redstoneButtonY() { return topPos - 18; }"));
        assertCompactSchedule(source);

        String entangled = Files.readString(screen("AdvancedEntangledMiniatureRuinScreen.java"));
        assertTrue(entangled.contains("leftPos + imageWidth + 6, topPos - 18, menu"));
        assertTrue(entangled.contains("configurationSideTitle()"));
        assertTrue(entangled.contains("container.immortalstorage.ruin.side_reversed"));
        assertCompactSchedule(entangled);
    }

    private static void assertCompactSchedule(String source) {
        for (int y : new int[]{164, 181, 198}) {
            assertTrue(source.contains("topPos + " + y + ", 120, 16"), "scheduling row at " + y);
        }
        assertTrue(source.contains("accessButton = addActionButton("));
        assertTrue(source.contains("splitButton = addActionButton("));
        assertTrue(source.contains("orderButton = addActionButton("));
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
