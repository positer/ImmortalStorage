package com.cultivation.cultivation.worldshard;

import com.cultivation.cultivation.item.custom.ImmortalYuanItem;
import com.cultivation.cultivation.network.storage.PersonalStorageNetwork;
import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Outcome-level contract shared by ore mining and treasure-basin loot.
 * Dimension lookup is deliberately outside this pure test: a null personal
 * endpoint is the normal non-personal-realm route, while the real endpoint is
 * the exact-owner personal-realm route.
 */
final class WorldShardUnifiedOutputTest {
    private static Item immortalYuan;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        ((MappedRegistry<Item>) BuiltInRegistries.ITEM).unfreeze();
        immortalYuan = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath("cultivation_world_shard_test", "immortal_yuan"),
                new ImmortalYuanItem(new Item.Properties().stacksTo(64)));
        BuiltInRegistries.ITEM.freeze();
    }

    @Test
    void nonPersonalRealmWritesTheWholeGenerationIntoTheSharedTwentySevenSlotCache() {
        WorldShardMinerCache cache = new WorldShardMinerCache(null);

        WorldShardOutputRouter.RouteResult routed = WorldShardOutputRouter.routeCache(
                List.of(new ItemStack(Items.DIAMOND, 64), new ItemStack(Items.DIAMOND, 16)), cache);

        assertEquals(WorldShardMinerCache.SLOT_COUNT, cache.getSlots());
        assertEquals(80L, routed.offered());
        assertEquals(80L, routed.accepted());
        assertEquals(0L, routed.unaccepted());
        assertEquals(80, cachedItemCount(cache));
    }

    @Test
    void exactOwnerPersonalRealmWritesComponentDistinctOutputsDirectlyIntoUnifiedXianqiao() {
        CultivationPlayerData ownerData = new CultivationPlayerData();
        ownerData.setStage(7);
        PersonalStorageNetwork.Endpoint ownerStorage = endpoint(ownerData);
        ItemStack namedDiamond = new ItemStack(Items.DIAMOND, 11);
        namedDiamond.set(DataComponents.CUSTOM_NAME, Component.literal("treasure variant"));

        WorldShardOutputRouter.RouteResult routed = WorldShardOutputRouter.routeDirect(
                List.of(new ItemStack(Items.DIAMOND, 7), namedDiamond), ownerStorage);

        assertEquals(18L, routed.accepted());
        assertEquals(0L, routed.unaccepted());
        assertEquals(2, ownerData.getXianqiaoItemSummary().size(),
                "complete Data Components remain distinct in the unified directory");
        assertEquals(18L, ownerData.getXianqiaoItemSummary().stream()
                .mapToLong(summary -> summary.amount()).sum());
    }

    @Test
    void unavailablePersonalRealmStorageRejectsTheBatchWithoutFallingBackToLocalCache() {
        WorldShardMinerCache cache = new WorldShardMinerCache(null);
        cache.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 5));

        WorldShardOutputRouter.RouteResult routed = WorldShardOutputRouter.routeDirect(
                List.of(new ItemStack(Items.DIAMOND, 8)), null);

        assertEquals(8L, routed.offered());
        assertEquals(0L, routed.accepted());
        assertEquals(8L, routed.unaccepted());
        assertEquals(5, cachedItemCount(cache),
                "an owner-realm failure must pause instead of spilling into the local cache");
    }

    @Test
    void fullExternalCacheDefersTheWholeGenerationWithoutPartialWrites() {
        WorldShardMinerCache cache = new WorldShardMinerCache(null);
        for (int slot = 0; slot < cache.getSlots() - 1; slot++) {
            cache.setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        int lastSlot = cache.getSlots() - 1;
        cache.setStackInSlot(lastSlot, new ItemStack(Items.DIAMOND, 63));

        WorldShardOutputRouter.RouteResult routed = WorldShardOutputRouter.routeCache(
                List.of(new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.EMERALD, 1)), cache);

        assertEquals(3L, routed.offered());
        assertEquals(0L, routed.accepted());
        assertEquals(3L, routed.unaccepted());
        assertEquals(63, cache.getStackInSlot(lastSlot).getCount());
        assertEquals(Items.DIAMOND, cache.getStackInSlot(lastSlot).getItem());
        assertEquals(0, IntStream.range(0, cache.getSlots())
                .mapToObj(cache::getStackInSlot)
                .filter(stack -> stack.is(Items.EMERALD))
                .mapToInt(ItemStack::getCount).sum());
    }

    @Test
    void ownerRealmPreflightsTheWholeYuanBatchBeforePublishingAnyItem() {
        CultivationPlayerData ownerData = new CultivationPlayerData();
        ownerData.setStage(7);
        ownerData.insertStack(new ItemStack(immortalYuan, 255), true);

        WorldShardOutputRouter.RouteResult routed = WorldShardOutputRouter.routeDirect(
                List.of(new ItemStack(Items.DIAMOND, 12),
                        new ItemStack(immortalYuan, 1),
                        new ItemStack(immortalYuan, 1)),
                endpoint(ownerData));

        assertEquals(14L, routed.offered());
        assertEquals(0L, routed.accepted());
        assertEquals(14L, routed.unaccepted());
        assertEquals(255L, ownerData.getImmortalYuan());
        assertEquals(0L, ownerData.getXianqiaoItemSummary().stream()
                .filter(summary -> summary.prototype().is(Items.DIAMOND))
                .mapToLong(summary -> summary.amount()).sum(),
                "a rejected Yuan tail must not leave earlier ordinary loot committed for replay");
    }

    private static PersonalStorageNetwork.Endpoint endpoint(CultivationPlayerData data) {
        return new PersonalStorageNetwork.Endpoint(UUID.randomUUID(), data, RegistryAccess.EMPTY, () -> { });
    }

    private static int cachedItemCount(WorldShardMinerCache cache) {
        return IntStream.range(0, cache.getSlots())
                .map(slot -> cache.getStackInSlot(slot).getCount())
                .sum();
    }
}
