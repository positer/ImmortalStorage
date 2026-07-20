package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerCache;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldShardMinerMenuContractTest {
    @BeforeAll
    static void bootstrap() {
        Bootstrap.bootStrap();
    }

    @Test
    void minerExposesExactlyOneVanillaThreeRowContainer() {
        assertEquals(27, WorldShardMinerCache.SLOT_COUNT);
        assertTrue(MenuProvider.class.isAssignableFrom(WorldShardMinerBlockEntity.class));
        assertTrue(Container.class.isAssignableFrom(WorldShardMinerBlockEntity.class));

        SimpleContainer storage = new SimpleContainer(WorldShardMinerCache.SLOT_COUNT);
        ChestMenu menu = WorldShardMinerBlockEntity.createCacheMenu(7, new Inventory(null), storage);

        assertSame(MenuType.GENERIC_9x3, menu.getType());
        assertEquals(3, menu.getRowCount());
        assertSame(storage, menu.getContainer());
        assertEquals(27 + Inventory.INVENTORY_SIZE, menu.slots.size());
    }
}
