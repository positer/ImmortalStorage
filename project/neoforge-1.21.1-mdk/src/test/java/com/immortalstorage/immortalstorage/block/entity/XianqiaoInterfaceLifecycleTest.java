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
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceLifecycleTest {
    private static RegistryAccess.Frozen registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void targetsAndRealBuffersRoundTripWithoutLosingCompleteItemIdentity() {
        FakeStorage storage = new FakeStorage();
        XianqiaoInterfaceInventory original = new XianqiaoInterfaceInventory(storage, () -> true);
        ItemStack target = new ItemStack(Items.DIAMOND, 12);
        target.set(DataComponents.CUSTOM_NAME, Component.literal("configured identity"));
        storage.put(target, 12L);

        assertTrue(original.setTarget(4, target));
        assertEquals(12, original.replenishSlot(4, TerminalStorageAction.EXECUTE));
        CompoundTag saved = new CompoundTag();
        original.saveState(saved, registries);

        XianqiaoInterfaceInventory restored = new XianqiaoInterfaceInventory(new FakeStorage(), () -> true);
        restored.loadState(saved, registries);

        assertStack(restored.getTarget(4), target, 12);
        assertStack(restored.getBufferedStack(4), target, 12);
    }

    @Test
    void removalReturnsWhatStorageAcceptsAndDropsTheExactRemainderOnce() {
        FakeStorage storage = new FakeStorage();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(storage, () -> true);
        ItemStack target = new ItemStack(Items.EMERALD, 10);
        storage.put(target, 10L);
        assertTrue(inventory.setTarget(0, target));
        assertEquals(10, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        storage.setInsertCapacity(6L);

        List<ItemStack> firstDrops = inventory.returnBuffersAndCollectRemainders();
        List<ItemStack> secondDrops = inventory.returnBuffersAndCollectRemainders();

        assertEquals(1, firstDrops.size());
        assertStack(firstDrops.getFirst(), target, 4);
        assertEquals(6L, storage.amount(target));
        assertTrue(inventory.getBufferedStack(0).isEmpty());
        assertTrue(secondDrops.isEmpty(), "removal release must be idempotent");
    }

    @Test
    void removalStillDropsEveryRealBufferWhenNormalLiveAccessWasRevoked() {
        FakeStorage storage = new FakeStorage();
        boolean[] live = {true};
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(storage, () -> live[0]);
        ItemStack target = new ItemStack(Items.QUARTZ, 7);
        storage.put(target, 7L);
        assertTrue(inventory.setTarget(2, target));
        assertEquals(7, inventory.replenishSlot(2, TerminalStorageAction.EXECUTE));
        storage.setInsertCapacity(0L);
        live[0] = false;

        List<ItemStack> drops = inventory.returnBuffersAndCollectRemainders();

        assertEquals(1, drops.size());
        assertStack(drops.getFirst(), target, 7);
    }

    @Test
    void ownerBindingSurvivesBlockEntityNbtAndBoundItemsRejectAnotherPlayer() {
        UUID owner = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        XianqiaoInterfaceBlockEntity original = new XianqiaoInterfaceBlockEntity(
                BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());

        assertFalse(original.tryBindOwner(owner, 5));
        assertTrue(original.tryBindOwner(owner, 6));
        assertFalse(original.tryBindOwner(intruder, 10));

        CompoundTag saved = new CompoundTag();
        original.saveAdditional(saved, registries);
        XianqiaoInterfaceBlockEntity restored = new XianqiaoInterfaceBlockEntity(
                BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        restored.loadAdditional(saved, registries);

        assertEquals(owner, restored.getOwner());
        assertTrue(XianqiaoInterfaceBlockEntity.canUse(owner, 6, restored.getOwner()));
        assertFalse(XianqiaoInterfaceBlockEntity.canUse(intruder, 10, restored.getOwner()));

        ItemStack boundBlock = new ItemStack(Items.FURNACE);
        CompoundTag ownerData = new CompoundTag();
        ownerData.putUUID("Owner", owner);
        BlockItem.setBlockEntityData(boundBlock, BlockEntityType.FURNACE, ownerData);
        assertTrue(XianqiaoInterfaceBlockEntity.canPlaceStackFor(boundBlock, owner));
        assertFalse(XianqiaoInterfaceBlockEntity.canPlaceStackFor(boundBlock, intruder));
        assertTrue(XianqiaoInterfaceBlockEntity.canPlaceStackFor(new ItemStack(Items.FURNACE), intruder),
                "an unbound block item remains placeable and binds only after the stage gate succeeds");
    }

    @Test
    void blockItemDataKeepsOwnerAndTargetsButNeverCopiesRealBuffers() {
        UUID owner = UUID.randomUUID();
        FakeStorage storage = new FakeStorage();
        XianqiaoInterfaceInventory persistedInventory = new XianqiaoInterfaceInventory(storage, () -> true);
        ItemStack target = new ItemStack(Items.REDSTONE, 9);
        storage.put(target, 9L);
        assertTrue(persistedInventory.setTarget(3, target));
        assertEquals(9, persistedInventory.replenishSlot(3, TerminalStorageAction.EXECUTE));

        CompoundTag persisted = new CompoundTag();
        persisted.putUUID("Owner", owner);
        persistedInventory.saveState(persisted, registries);
        XianqiaoInterfaceBlockEntity entity = new XianqiaoInterfaceBlockEntity(
                BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        entity.loadAdditional(persisted, registries);

        ItemStack droppedBlock = new ItemStack(Items.FURNACE);
        entity.saveToItem(droppedBlock, registries);
        CompoundTag itemData = droppedBlock.getOrDefault(
                DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();

        assertEquals(owner, itemData.getUUID("Owner"));
        assertTrue(itemData.contains("ItemSlots"));
        assertEquals(0L, itemData.getList("ItemSlots", net.minecraft.nbt.Tag.TAG_COMPOUND)
                .getCompound(0).getLong("Cached"),
                "real item buffers drop separately and must be scrubbed from the block item");
        assertFalse(itemData.contains("Buffers"),
                "real buffers return to storage or drop separately and must never be copied into the block item");
    }

    private static void assertStack(ItemStack actual, ItemStack identity, int count) {
        assertTrue(ItemStack.isSameItemSameComponents(actual, identity));
        assertEquals(count, actual.getCount());
    }

    private static final class FakeStorage implements TerminalItemStorage {
        private final Map<TerminalEntryKey, Long> amounts = new LinkedHashMap<>();
        private long revision;
        private long insertCapacity = Long.MAX_VALUE;

        void put(ItemStack stack, long amount) {
            amounts.put(TerminalEntryKey.of(stack), amount);
        }

        long amount(ItemStack stack) {
            return amounts.getOrDefault(TerminalEntryKey.of(stack), 0L);
        }

        void setInsertCapacity(long capacity) {
            insertCapacity = Math.max(0L, capacity);
        }

        @Override
        public long revision() {
            return revision;
        }

        @Override
        public List<StorageItemSummary> snapshot() {
            List<StorageItemSummary> snapshot = new ArrayList<>();
            amounts.forEach((key, amount) -> {
                if (amount > 0L) snapshot.add(new StorageItemSummary(key.prototype(), amount));
            });
            return List.copyOf(snapshot);
        }

        @Override
        public long insert(TerminalEntryKey key, long amount, TerminalStorageAction action) {
            long accepted = Math.min(Math.max(0L, amount), insertCapacity);
            if (action != null && action.executes() && accepted > 0L) {
                amounts.merge(key, accepted, Long::sum);
                revision++;
            }
            return accepted;
        }

        @Override
        public long extract(TerminalEntryKey key, long amount, TerminalStorageAction action) {
            long available = amounts.getOrDefault(key, 0L);
            long extracted = Math.min(Math.max(0L, amount), available);
            if (action != null && action.executes() && extracted > 0L) {
                long remaining = available - extracted;
                if (remaining == 0L) amounts.remove(key);
                else amounts.put(key, remaining);
                revision++;
            }
            return extracted;
        }
    }
}
