package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldShardOutputAndWeightsTest {
    @BeforeAll
    static void bootstrap() {
        Bootstrap.bootStrap();
    }

    @Test
    void cacheHasWoodenBarrelCapacityAndStandardHandlerSemantics() {
        WorldShardMinerCache cache = new WorldShardMinerCache(() -> {});
        assertEquals(27, cache.getSlots());
        assertTrue(cache.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false).isEmpty());
        assertEquals(64, cache.extractItem(0, 64, true).getCount());
        assertEquals(64, cache.getStackInSlot(0).getCount(), "simulation must stay read-only");
    }

    @Test
    void partiallyFullCacheRejectsTheWholeMultiItemBatchWithoutPlanningLeaks() {
        WorldShardMinerCache cache = new WorldShardMinerCache(null);
        for (int slot = 0; slot < 26; slot++) {
            cache.setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        cache.setStackInSlot(26, new ItemStack(Items.DIAMOND, 63));

        WorldShardOutputRouter.RouteResult result = WorldShardOutputRouter.routeCache(
                java.util.List.of(new ItemStack(Items.DIAMOND, 2),
                        new ItemStack(Items.GOLD_INGOT, 1)), cache);

        assertEquals(63, cache.getStackInSlot(26).getCount());
        assertEquals(0L, result.accepted());
        assertEquals(3L, result.unaccepted());
    }

    @Test
    void weightedBatchIsStableAcrossInputOrderAndAggregatesBeforeInsertion() {
        Map<Item, Long> firstOrder = new LinkedHashMap<>();
        firstOrder.put(Items.DIAMOND, 1L);
        firstOrder.put(Items.IRON_INGOT, 3L);
        Map<Item, Long> reverseOrder = new LinkedHashMap<>();
        reverseOrder.put(Items.IRON_INGOT, 3L);
        reverseOrder.put(Items.DIAMOND, 1L);

        Map<Item, Integer> first = WorldShardOrePool.of(firstOrder)
                .sampleBatch(RandomSource.create(12345L), 4_096);
        Map<Item, Integer> second = WorldShardOrePool.of(reverseOrder)
                .sampleBatch(RandomSource.create(12345L), 4_096);

        assertEquals(first, second);
        assertEquals(4_096, first.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(2, first.size());
    }

}
