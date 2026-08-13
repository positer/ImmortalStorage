package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.menu.custom.WorldShardMinerMenu;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerCache;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SimpleContainerData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldShardMinerMenuContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProject();

    @BeforeAll static void bootstrap() { Bootstrap.bootStrap(); }

    @Test
    void minerUsesItsOwnTwentySevenSlotCachePlusOneSettingsPluginSlot() {
        assertEquals(27, WorldShardMinerCache.SLOT_COUNT);
        assertTrue(MenuProvider.class.isAssignableFrom(WorldShardMinerBlockEntity.class));
        assertTrue(Container.class.isAssignableFrom(WorldShardMinerBlockEntity.class));
        assertTrue(ReinforcementPluginHost.class.isAssignableFrom(WorldShardMinerBlockEntity.class));

        SimpleContainer storage = new SimpleContainer(WorldShardMinerCache.SLOT_COUNT + 1);
        WorldShardMinerMenu menu = new WorldShardMinerMenu(7, new Inventory(null, new net.minecraft.world.entity.EntityEquipment()), storage,
                new SimpleContainerData(WorldShardMinerMenu.DATA_COUNT));
        assertSame(storage, menu.getCacheContainer());
        assertEquals(28 + Inventory.INVENTORY_SIZE, menu.slots.size());
        for (int slot = 0; slot < WorldShardMinerCache.SLOT_COUNT; slot++) {
            assertSame(storage, menu.getSlot(slot).container);
        }
    }

    @Test
    void minerMirrorsBasinSettingsAndMultipliesGeneratedOre() throws Exception {
        String entity = Files.readString(PROJECT.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/block/entity/WorldShardMinerBlockEntity.java"));
        String block = Files.readString(PROJECT.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/block/custom/WorldShardMinerBlock.java"));
        String screen = Files.readString(PROJECT.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/client/screen/WorldShardMinerScreen.java"));

        assertTrue(entity.contains("implements Container, MenuProvider, ReinforcementPluginHost"));
        assertTrue(entity.contains("ReinforcementPluginHost.multiplyOutputs("));
        assertTrue(entity.contains("generated, miner.reinforcementMultiplier()"));
        assertTrue(entity.contains("pushCacheToFaces(level)"));
        assertTrue(entity.contains("flushCacheToXianqiao(level)"));
        assertTrue(entity.contains("XianqiaoOutput"));
        assertTrue(entity.contains("AutomaticFaceOutput"));
        assertTrue(entity.contains("OutputFaces"));
        assertTrue(block.contains("serverPlayer.openMenu(miner, pos)"));
        assertTrue(screen.contains("same settings tab and geometry as the Treasure Basin"));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate NeoForge project");
    }
}
