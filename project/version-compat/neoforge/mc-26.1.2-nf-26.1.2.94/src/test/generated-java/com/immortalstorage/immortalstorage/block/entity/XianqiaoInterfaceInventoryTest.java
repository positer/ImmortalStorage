package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceInventoryTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void hasNineTargetsAndNineRealBuffersAndKeepsCompleteComponentsDistinct() {
        FakeTerminalStorage storage = new FakeTerminalStorage();
        AtomicBoolean live = new AtomicBoolean(true);
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(storage, live::get);
        ItemStack plainIron = new ItemStack(Items.IRON_INGOT, 6);
        ItemStack namedIron = new ItemStack(Items.IRON_INGOT, 4);
        namedIron.set(DataComponents.CUSTOM_NAME, Component.literal("bound identity"));
        storage.put(plainIron, 30L);
        storage.put(namedIron, 7L);

        assertEquals(9, inventory.getSlots());
        assertTrue(inventory.setTarget(0, plainIron));
        assertTrue(inventory.setTarget(1, namedIron));
        assertEquals(6, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        assertEquals(4, inventory.replenishSlot(1, TerminalStorageAction.EXECUTE));

        assertStack(inventory.getBufferedStack(0), plainIron, 6);
        assertStack(inventory.getBufferedStack(1), namedIron, 4);
        assertFalse(inventory.getBufferedStack(0).has(DataComponents.CUSTOM_NAME));
        assertEquals("bound identity", inventory.getBufferedStack(1)
                .get(DataComponents.CUSTOM_NAME).getString());
        assertEquals(24L, storage.amount(plainIron));
        assertEquals(3L, storage.amount(namedIron));

        ItemStack oversized = new ItemStack(Items.ENDER_PEARL, 17);
        assertTrue(inventory.setTarget(2, oversized));
        assertStack(inventory.getTarget(2), oversized, 17);
    }

    @Test
    void replenishesOneSlotAtATimeAndSimulationMatchesExecutionWithoutMutation() {
        FakeTerminalStorage storage = new FakeTerminalStorage();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(storage, () -> true);
        ItemStack diamondTarget = new ItemStack(Items.DIAMOND, 5);
        ItemStack emeraldTarget = new ItemStack(Items.EMERALD, 3);
        storage.put(diamondTarget, 20L);
        storage.put(emeraldTarget, 20L);
        assertTrue(inventory.setTarget(0, diamondTarget));
        assertTrue(inventory.setTarget(1, emeraldTarget));

        assertEquals(5, inventory.replenishNextSlot(TerminalStorageAction.SIMULATE));
        assertTrue(inventory.getBufferedStack(0).isEmpty());
        assertEquals(20L, storage.amount(diamondTarget));

        assertEquals(5, inventory.replenishNextSlot(TerminalStorageAction.EXECUTE));
        assertStack(inventory.getBufferedStack(0), diamondTarget, 5);
        assertTrue(inventory.getBufferedStack(1).isEmpty(), "one call must not refill every slot");
        assertEquals(3, inventory.replenishNextSlot(TerminalStorageAction.EXECUTE));
        assertStack(inventory.getBufferedStack(1), emeraldTarget, 3);
    }

    @Test
    void directAmountInputIsClampedPerTargetSlotByTheServerBackend() throws Exception {
        FakeTerminalStorage storage = new FakeTerminalStorage();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(storage, () -> true);
        assertTrue(inventory.setTarget(0, new ItemStack(Items.DIAMOND, 1)));
        assertTrue(inventory.setTarget(1, new ItemStack(Items.ENDER_PEARL, 1)));

        var setter = XianqiaoInterfaceInventory.class
                .getDeclaredMethod("setTargetAmount", int.class, long.class);
        setter.setAccessible(true);

        assertTrue((Boolean) setter.invoke(inventory, 0, Long.MAX_VALUE));
        assertTrue((Boolean) setter.invoke(inventory, 1, Long.MAX_VALUE));
        assertEquals(128, inventory.getTarget(0).getCount());
        assertEquals(128, inventory.getTarget(1).getCount());

        assertTrue((Boolean) setter.invoke(inventory, 0, Long.MIN_VALUE));
        assertTrue(inventory.getTarget(0).isEmpty(),
                "zero-or-negative amounts clear the selected target after settling its real buffer");
        assertEquals(128, inventory.getTarget(1).getCount(),
                "one target's amount never consumes or rewrites another target's allowance");
    }

    @Test
    void duplicateIdentitiesMaintainIndependentRealBufferLimitsWhoseTotalIsTheirSum() {
        FakeTerminalStorage storage = new FakeTerminalStorage();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(storage, () -> true);
        ItemStack firstLimit = new ItemStack(Items.DIAMOND, 64);
        ItemStack secondLimit = new ItemStack(Items.DIAMOND, 32);
        storage.put(firstLimit, 200L);

        assertTrue(inventory.setTarget(0, firstLimit));
        assertTrue(inventory.setTarget(1, secondLimit));
        assertEquals(64, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        assertEquals(32, inventory.replenishSlot(1, TerminalStorageAction.EXECUTE));

        assertStack(inventory.getBufferedStack(0), firstLimit, 64);
        assertStack(inventory.getBufferedStack(1), secondLimit, 32);
        assertEquals(96, inventory.getBufferedStack(0).getCount()
                + inventory.getBufferedStack(1).getCount());
        assertEquals(104L, storage.amount(firstLimit));
    }

    @Test
    void sameItemTargetDecreaseKeepsCacheAndReturnsOnlyExcessWithRetry() {
        FakeTerminalStorage storage = new FakeTerminalStorage();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(storage, () -> true);
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        storage.put(diamonds, 128L);
        assertTrue(inventory.setTarget(0, diamonds.copyWithCount(128)));
        assertEquals(128, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        assertEquals(0L, storage.amount(diamonds));

        storage.setInsertCapacity(20L);
        assertTrue(inventory.setTargetAmount(0, 32L));
        assertStack(inventory.getTarget(0), diamonds, 32);
        assertStack(inventory.getBufferedStack(0), diamonds, 128);
        assertEquals(20, inventory.replenishSlot(0, TerminalStorageAction.SIMULATE));
        assertStack(inventory.getBufferedStack(0), diamonds, 128);

        assertEquals(20, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        assertStack(inventory.getBufferedStack(0), diamonds, 108);
        assertEquals(20L, storage.amount(diamonds));

        storage.setInsertCapacity(0L);
        assertEquals(0, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        assertStack(inventory.getBufferedStack(0), diamonds, 108);

        storage.setInsertCapacity(Long.MAX_VALUE);
        assertEquals(76, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        assertStack(inventory.getBufferedStack(0), diamonds, 32);
        assertEquals(96L, storage.amount(diamonds));
    }

    @Test
    void rejectedRemovalOf128ItemsProducesTwoLegalStacksWithoutLoss() {
        FakeTerminalStorage storage = new FakeTerminalStorage();
        AtomicInteger dirty = new AtomicInteger();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(
                storage, () -> true, dirty::incrementAndGet);
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        storage.put(diamonds, 128L);
        assertTrue(inventory.setTarget(0, diamonds.copyWithCount(128)));
        assertEquals(128, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        storage.setInsertCapacity(0L);
        dirty.set(0);

        List<ItemStack> remainders = inventory.returnBuffersAndCollectRemainders();

        assertEquals(List.of(64, 64), remainders.stream().map(ItemStack::getCount).toList());
        assertEquals(128, remainders.stream().mapToInt(ItemStack::getCount).sum());
        assertTrue(remainders.stream().allMatch(stack -> stack.getCount()
                <= Math.min(stack.getMaxStackSize(), 99)));
        assertTrue(inventory.getBufferedStack(0).isEmpty());
        assertTrue(dirty.get() > 0, "materialized removal remainders must be persisted immediately");
    }

    @Test
    void externalHandlerUploadsDirectlyButExtractsOnlyFromTheRealBuffer() {
        FakeTerminalStorage storage = new FakeTerminalStorage();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(storage, () -> true);
        ItemStack target = new ItemStack(Items.GOLD_INGOT, 8);
        storage.put(target, 20L);
        assertTrue(inventory.setTarget(3, target));
        assertEquals(8, inventory.replenishSlot(3, TerminalStorageAction.EXECUTE));
        assertEquals(12L, storage.amount(target));

        ItemStack incoming = new ItemStack(Items.GOLD_INGOT, 5);
        ItemStack simulatedRemainder = inventory.insertItem(7, incoming, true);
        assertTrue(simulatedRemainder.isEmpty());
        assertEquals(12L, storage.amount(target));
        assertStack(inventory.getBufferedStack(3), target, 8);

        ItemStack executedRemainder = inventory.insertItem(7, incoming, false);
        assertTrue(executedRemainder.isEmpty());
        assertEquals(17L, storage.amount(target));
        assertStack(inventory.getBufferedStack(3), target, 8);

        ItemStack simulatedExtract = inventory.extractItem(3, 6, true);
        assertStack(simulatedExtract, target, 6);
        assertStack(inventory.getBufferedStack(3), target, 8);
        assertEquals(17L, storage.amount(target));

        ItemStack executedExtract = inventory.extractItem(3, 6, false);
        assertStack(executedExtract, target, 6);
        assertStack(inventory.getBufferedStack(3), target, 2);
        assertEquals(17L, storage.amount(target), "external extraction must not bypass the buffer");
    }

    @Test
    void replacingOrClearingAConfiguredSlotRetainsTheAtomicWholeReturnGate() {
        FakeTerminalStorage storage = new FakeTerminalStorage();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(storage, () -> true);
        ItemStack diamondEight = new ItemStack(Items.DIAMOND, 8);
        ItemStack emeraldThree = new ItemStack(Items.EMERALD, 3);
        storage.put(diamondEight, 8L);
        assertTrue(inventory.setTarget(0, diamondEight));
        assertEquals(8, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        assertEquals(0L, storage.amount(diamondEight));

        storage.setInsertCapacity(7L);
        assertFalse(inventory.setTarget(0, emeraldThree));
        assertStack(inventory.getTarget(0), diamondEight, 8);
        assertStack(inventory.getBufferedStack(0), diamondEight, 8);
        assertEquals(0L, storage.amount(diamondEight));

        storage.setInsertCapacity(Long.MAX_VALUE);
        assertTrue(inventory.setTarget(0, emeraldThree));
        assertStack(inventory.getTarget(0), emeraldThree, 3);
        assertTrue(inventory.getBufferedStack(0).isEmpty());
        assertEquals(8L, storage.amount(diamondEight));

        storage.put(emeraldThree, 3L);
        assertEquals(3, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        storage.setInsertCapacity(2L);
        assertFalse(inventory.setTarget(0, ItemStack.EMPTY));
        assertStack(inventory.getTarget(0), emeraldThree, 3);
        assertStack(inventory.getBufferedStack(0), emeraldThree, 3);

        storage.setInsertCapacity(Long.MAX_VALUE);
        assertTrue(inventory.setTarget(0, ItemStack.EMPTY));
        assertTrue(inventory.getTarget(0).isEmpty());
        assertTrue(inventory.getBufferedStack(0).isEmpty());
        assertEquals(3L, storage.amount(emeraldThree));
    }

    @Test
    void unexpectedPartialCommitIsCompensatedAndLeavesConfigurationAndBufferUnchanged() {
        FakeTerminalStorage storage = new FakeTerminalStorage();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(storage, () -> true);
        ItemStack oldTarget = new ItemStack(Items.REDSTONE, 9);
        ItemStack newTarget = new ItemStack(Items.LAPIS_LAZULI, 4);
        storage.put(oldTarget, 9L);
        assertTrue(inventory.setTarget(0, oldTarget));
        assertEquals(9, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        storage.partialNextExecutedInsert();

        assertFalse(inventory.setTarget(0, newTarget));
        assertStack(inventory.getTarget(0), oldTarget, 9);
        assertStack(inventory.getBufferedStack(0), oldTarget, 9);
        assertEquals(0L, storage.amount(oldTarget), "the partial commit must be rolled back");
    }

    @Test
    void previouslyAcquiredHandlerFailsClosedImmediatelyWhenLiveAccessIsRevoked() {
        FakeTerminalStorage storage = new FakeTerminalStorage();
        AtomicBoolean live = new AtomicBoolean(true);
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(storage, live::get);
        ItemStack target = new ItemStack(Items.QUARTZ, 6);
        storage.put(target, 20L);
        assertTrue(inventory.setTarget(0, target));
        assertEquals(6, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        long amountBeforeRevocation = storage.amount(target);

        live.set(false);

        assertEquals(0, inventory.getSlots());
        assertTrue(inventory.getTarget(0).isEmpty());
        assertTrue(inventory.getBufferedStack(0).isEmpty());
        assertFalse(inventory.setTarget(0, new ItemStack(Items.DIAMOND, 1)));
        assertEquals(0, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        ItemStack rejected = inventory.insertItem(0, new ItemStack(Items.QUARTZ, 3), false);
        assertEquals(3, rejected.getCount());
        assertTrue(inventory.extractItem(0, 3, false).isEmpty());
        assertEquals(amountBeforeRevocation, storage.amount(target));

        live.set(true);
        assertEquals(9, inventory.getSlots());
        assertStack(inventory.getTarget(0), target, 6);
        assertStack(inventory.getBufferedStack(0), target, 6);
    }

    private static void assertStack(ItemStack actual, ItemStack expectedIdentity, int expectedCount) {
        assertTrue(ItemStack.isSameItemSameComponents(actual, expectedIdentity),
                () -> "identity mismatch: expected=" + expectedIdentity + ", actual=" + actual);
        assertEquals(expectedCount, actual.getCount());
    }

    private static final class FakeTerminalStorage implements TerminalItemStorage {
        private final Map<TerminalEntryKey, Long> amounts = new LinkedHashMap<>();
        private long revision;
        private long insertCapacity = Long.MAX_VALUE;
        private boolean partialNextExecutedInsert;

        void put(ItemStack identity, long amount) {
            amounts.put(TerminalEntryKey.of(identity), amount);
        }

        long amount(ItemStack identity) {
            return amounts.getOrDefault(TerminalEntryKey.of(identity), 0L);
        }

        void setInsertCapacity(long insertCapacity) {
            this.insertCapacity = Math.max(0L, insertCapacity);
        }

        void partialNextExecutedInsert() {
            partialNextExecutedInsert = true;
        }

        @Override
        public long revision() {
            return revision;
        }

        @Override
        public List<StorageItemSummary> snapshot() {
            List<StorageItemSummary> result = new ArrayList<>();
            amounts.forEach((key, amount) -> {
                if (amount > 0L) result.add(new StorageItemSummary(key.prototype(), amount));
            });
            return List.copyOf(result);
        }

        @Override
        public long insert(TerminalEntryKey key, long amount, TerminalStorageAction action) {
            if (key == null || amount <= 0L || action == null) return 0L;
            long accepted = Math.min(amount, insertCapacity);
            if (action.executes() && partialNextExecutedInsert) {
                partialNextExecutedInsert = false;
                accepted = Math.max(0L, accepted - 1L);
            }
            if (action.executes() && accepted > 0L) {
                amounts.merge(key, accepted, FakeTerminalStorage::saturatingAdd);
                revision++;
            }
            return accepted;
        }

        @Override
        public long extract(TerminalEntryKey key, long amount, TerminalStorageAction action) {
            if (key == null || amount <= 0L || action == null) return 0L;
            long available = amounts.getOrDefault(key, 0L);
            long extracted = Math.min(amount, available);
            if (action.executes() && extracted > 0L) {
                long remaining = available - extracted;
                if (remaining == 0L) amounts.remove(key);
                else amounts.put(key, remaining);
                revision++;
            }
            return extracted;
        }

        private static long saturatingAdd(long left, long right) {
            if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
            return left + right;
        }
    }
}
