package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.block.entity.TreasureBasinBlockEntity;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerCache;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SimpleContainerData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TreasureBasinMenuContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void basinIsAMenuProviderAndOwnsItsIndependentInventory() throws NoSuchMethodException {
        assertTrue(MenuProvider.class.isAssignableFrom(TreasureBasinBlockEntity.class));
        assertTrue(Container.class.isAssignableFrom(TreasureBasinBlockEntity.class));
        assertTrue(Arrays.stream(TreasureBasinBlockEntity.class.getMethods())
                        .noneMatch(method -> method.getName().equals("getAttachedMiner")),
                "the basin must not expose a public cross-machine handle");
        assertTrue(Arrays.stream(TreasureBasinBlockEntity.class.getDeclaredFields())
                        .anyMatch(field -> field.getType() == WorldShardMinerCache.class),
                "the basin must persist its own barrel-sized cache");
    }

    @Test
    void menuUsesTheExactProvidedCacheForAllTwentySevenMachineSlots() {
        SimpleContainer basinCache = new SimpleContainer(WorldShardMinerCache.SLOT_COUNT);
        TreasureBasinMenu menu = new TreasureBasinMenu(17, new Inventory(null, new net.minecraft.world.entity.EntityEquipment()), basinCache,
                new SimpleContainerData(TreasureBasinMenu.DATA_COUNT));

        assertSame(basinCache, menu.getCacheContainer());
        assertEquals(27, menu.getCacheCapacity());
        assertEquals(27 + Inventory.INVENTORY_SIZE, menu.slots.size());
        for (int slot = 0; slot < WorldShardMinerCache.SLOT_COUNT; slot++) {
            assertSame(basinCache, menu.getSlot(slot).container,
                    "every basin machine slot must be a live view of the basin cache");
        }
    }
}
