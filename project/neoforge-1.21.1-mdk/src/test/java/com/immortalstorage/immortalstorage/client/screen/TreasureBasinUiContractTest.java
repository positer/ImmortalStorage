package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.immortalstorage.block.entity.TreasureBasinBlockEntity;
import com.immortalstorage.immortalstorage.menu.custom.TreasureBasinMenu;
import net.minecraft.world.MenuProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TreasureBasinUiContractTest {
    private static final Path PROJECT = locateProject();

    @Test
    void basinIsMenuProviderAndMenuUsesItsOwnThreeRowCacheSurface() throws Exception {
        assertTrue(MenuProvider.class.isAssignableFrom(TreasureBasinBlockEntity.class));
        assertEquals(27, TreasureBasinMenu.CACHE_SLOT_COUNT);
        assertEquals(36, TreasureBasinMenu.PLAYER_SLOT_COUNT);
        TreasureBasinMenu.class.getMethod("getCacheContainer");

        String menu = source("menu/custom/TreasureBasinMenu.java");
        assertTrue(menu.contains("this(id, inventory, basin, liveStatus(basin), basin)"),
                "Production menu must pass the basin itself as its own cache Container");
        assertTrue(menu.contains("private final @Nullable TreasureBasinBlockEntity basin"));
        assertFalse(menu.contains("private final @Nullable WorldShardMinerBlockEntity miner"));
        assertTrue(menu.contains("new Slot(cacheContainer, column + row * 9"));
        assertTrue(menu.contains("for (int row = 0; row < 3; row++)"));
    }

    @Test
    void rightClickPassesTheBasinPositionAndScreenUsesRuntimeVanillaChestPixels() throws IOException {
        String block = source("block/custom/TreasureBasinBlock.java");
        String screen = source("client/screen/TreasureBasinScreen.java");
        String client = source("client/ClientSetup.java");
        String menus = source("menu/ModMenus.java");

        assertTrue(block.contains("serverPlayer.openMenu(basin, pos)"));
        assertTrue(screen.contains("textures/gui/container/generic_54.png"));
        assertFalse(screen.contains("menu.getActiveLevel()"));
        assertTrue(screen.contains("case CALIBRATING"));
        assertTrue(screen.contains("menu.getFilledSlots()"));
        assertTrue(menus.contains("MENUS.register(\"treasure_basin\""));
        assertTrue(client.contains("ModMenus.TREASURE_BASIN.get(), TreasureBasinScreen::new"));
    }

    @Test
    void bothLanguagesContainEveryReadOnlyStatusLabel() throws IOException {
        for (String locale : new String[] {"zh_cn", "en_us"}) {
            String language = Files.readString(PROJECT.resolve(
                    "src/main/resources/assets/immortalstorage/lang/" + locale + ".json"));
            for (String key : new String[] {
                    "container.immortalstorage.treasure_basin.mode",
                    "container.immortalstorage.treasure_basin.running",
                    "container.immortalstorage.treasure_basin.calibrating",
                    "container.immortalstorage.treasure_basin.stopped",
                    "container.immortalstorage.treasure_basin.cache_full",
                    "container.immortalstorage.treasure_basin.storage_unavailable",
                    "container.immortalstorage.treasure_basin.cache"
            }) {
                assertTrue(language.contains("\"" + key + "\""), locale + " missing " + key);
            }
        }
    }

    private static String source(String relative) throws IOException {
        return Files.readString(PROJECT.resolve("src/main/java/com/immortalstorage/immortalstorage/")
                .resolve(relative));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && current != null; i++, current = current.getParent()) {
            if (Files.isDirectory(current.resolve("src/main/java"))
                    && Files.isDirectory(current.resolve("src/main/resources"))) {
                return current;
            }
        }
        throw new IllegalStateException("Cannot locate NeoForge project from "
                + Path.of("").toAbsolutePath());
    }
}
