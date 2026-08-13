package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceBoundedRemovalTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static RegistryAccess.Frozen registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void rejectedIntegerMaximumCacheUsesABoundedDropListAndKeepsTheExactRemainder() {
        RejectingStorage storage = new RejectingStorage();
        XianqiaoInterfaceInventory inventory = maximumLimitInventory(storage);
        ItemStack identity = new ItemStack(Items.DIAMOND);
        storage.put(identity, Integer.MAX_VALUE);
        assertTrue(inventory.setTarget(0, identity));
        assertTrue(inventory.setTargetAmount(0, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE,
                inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));

        XianqiaoInterfaceInventory.ItemRemovalSettlement settlement =
                inventory.settleItemBuffersForRemoval();
        CompoundTag retainedState = new CompoundTag();
        inventory.saveState(retainedState, registries);
        long retained = cachedItemAmount(retainedState, 0);
        long materialized = settlement.materializedDrops().stream()
                .mapToLong(ItemStack::getCount).sum();

        assertTrue(settlement.materializedDrops().size()
                <= XianqiaoInterfaceInventory.MAX_MATERIALIZED_REMOVAL_STACKS);
        assertEquals(Integer.MAX_VALUE, materialized + retained,
                "rejected removal must preserve the exact cached amount once");
        assertEquals(retained, settlement.retainedAmount());

        XianqiaoInterfaceInventory restored = maximumLimitInventory(new RejectingStorage());
        restored.loadState(retainedState, registries);
        CompoundTag replayedState = new CompoundTag();
        restored.saveState(replayedState, registries);
        assertEquals(retained, cachedItemAmount(replayedState, 0),
                "placing the carrier again must restore the exact retained cache");
    }

    @Test
    void normal128CacheStillMaterializesAsTwoLegalStacks() {
        RejectingStorage storage = new RejectingStorage();
        XianqiaoInterfaceInventory inventory = maximumLimitInventory(storage);
        ItemStack identity = new ItemStack(Items.EMERALD);
        storage.put(identity, 128L);
        assertTrue(inventory.setTarget(0, identity));
        assertTrue(inventory.setTargetAmount(0, 128L));
        assertEquals(128, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));

        XianqiaoInterfaceInventory.ItemRemovalSettlement settlement =
                inventory.settleItemBuffersForRemoval();

        assertEquals(List.of(64, 64), settlement.materializedDrops().stream()
                .map(ItemStack::getCount).toList());
        assertEquals(0L, settlement.retainedAmount());
    }

    @Test
    void oversizedRejectedCacheTravelsInTheDroppedBlockAndSurvivesReplacementWithoutDuplication() {
        CompoundTag loadedState = maximumCachedState(Items.REDSTONE, Integer.MAX_VALUE);
        UUID owner = UUID.randomUUID();
        com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.putUuid(loadedState, "Owner", owner);
        XianqiaoInterfaceBlockEntity broken = new XianqiaoInterfaceBlockEntity(
                BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        broken.loadAdditionalLegacy(loadedState, registries);

        List<ItemStack> worldDrops = broken.prepareBuffersForRemoval();
        ItemStack carrier = new ItemStack(Items.FURNACE);
        broken.saveToItem(carrier, registries);
        CompoundTag carrierData = carrier.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        long retained = cachedItemAmount(carrierData, 0);

        assertTrue(worldDrops.size()
                <= XianqiaoInterfaceInventory.MAX_MATERIALIZED_REMOVAL_STACKS);
        assertEquals(Integer.MAX_VALUE,
                worldDrops.stream().mapToLong(ItemStack::getCount).sum() + retained);
        assertEquals(owner, com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.getUuid(carrierData, "Owner"));

        XianqiaoInterfaceBlockEntity replaced = new XianqiaoInterfaceBlockEntity(
                BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        replaced.loadAdditionalLegacy(carrierData, registries);
        CompoundTag replayed = new CompoundTag();
        replaced.saveAdditionalLegacy(replayed, registries);
        assertEquals(Integer.MAX_VALUE, cachedItemAmount(replayed, 0),
                "the carrier payload must restore once with no loss or duplicate materialization");
    }

    private static XianqiaoInterfaceInventory maximumLimitInventory(TerminalItemStorage storage) {
        return new XianqiaoInterfaceInventory(
                storage, null, () -> true, () -> {}, () -> {},
                () -> new XianqiaoInterfaceLimits.Snapshot(Integer.MAX_VALUE, 16_000));
    }

    private static CompoundTag maximumCachedState(net.minecraft.world.item.Item item, long cached) {
        RejectingStorage storage = new RejectingStorage();
        XianqiaoInterfaceInventory inventory = maximumLimitInventory(storage);
        ItemStack identity = new ItemStack(item);
        storage.put(identity, cached);
        assertTrue(inventory.setTarget(0, identity));
        assertTrue(inventory.setTargetAmount(0, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE,
                inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        CompoundTag state = new CompoundTag();
        inventory.saveState(state, registries);
        return state;
    }

    private static long cachedItemAmount(CompoundTag tag, int slot) {
        ListTag slots = tag.getListOrEmpty("ItemSlots");
        for (int index = 0; index < slots.size(); index++) {
            CompoundTag entry = slots.getCompoundOrEmpty(index);
            if (entry.getIntOr("Slot", 0) == slot) return entry.getLongOr("Cached", 0L);
        }
        return 0L;
    }

    private static final class RejectingStorage implements TerminalItemStorage {
        private final Map<TerminalEntryKey, Long> amounts = new LinkedHashMap<>();

        void put(ItemStack stack, long amount) {
            amounts.put(TerminalEntryKey.of(stack), amount);
        }

        @Override
        public long revision() {
            return 0L;
        }

        @Override
        public List<StorageItemSummary> snapshot() {
            List<StorageItemSummary> result = new ArrayList<>();
            amounts.forEach((key, amount) -> result.add(new StorageItemSummary(key.prototype(), amount)));
            return result;
        }

        @Override
        public long insert(TerminalEntryKey key, long amount, TerminalStorageAction action) {
            return 0L;
        }

        @Override
        public long extract(TerminalEntryKey key, long amount, TerminalStorageAction action) {
            long available = amounts.getOrDefault(key, 0L);
            long extracted = Math.min(Math.max(0L, amount), available);
            if (action != null && action.executes() && extracted > 0L) {
                long remaining = available - extracted;
                if (remaining == 0L) amounts.remove(key);
                else amounts.put(key, remaining);
            }
            return extracted;
        }
    }
}
