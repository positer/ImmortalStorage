package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class XianqiaoInterfaceMixedResourceTest {
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
    void oneSchedulingRoundFillsEveryIndependentItemAndFluidCache() {
        FakeItems items = new FakeItems();
        FakeFluids fluids = new FakeFluids();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(
                items, fluids, () -> true, () -> {}, () -> {});
        ItemStack diamonds = new ItemStack(Items.DIAMOND);
        FluidStack water = new FluidStack(Fluids.WATER, 1);
        items.put(diamonds, 200L);
        fluids.put(water, 10_000L);

        assertTrue(inventory.setTarget(0, diamonds.copyWithCount(64)));
        assertTrue(inventory.setTarget(1, diamonds.copyWithCount(32)));
        assertTrue(inventory.setFluidTarget(2, water.copyWithAmount(4_000)));
        assertTrue(inventory.setFluidTarget(3, water.copyWithAmount(1_500)));

        assertEquals(5_596L, inventory.replenishAllSlots(TerminalStorageAction.EXECUTE));
        assertEquals(64, inventory.getBufferedStack(0).getCount());
        assertEquals(32, inventory.getBufferedStack(1).getCount());
        assertEquals(4_000, inventory.getBufferedFluid(2).getAmount());
        assertEquals(1_500, inventory.getBufferedFluid(3).getAmount());
        assertEquals(104L, items.amount(diamonds));
        assertEquals(4_500L, fluids.amount(water));
    }

    @Test
    void resourceReplacementReturnsTheOldCacheBeforeRefillingTheNewIdentity() {
        FakeItems items = new FakeItems();
        FakeFluids fluids = new FakeFluids();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(
                items, fluids, () -> true, () -> {}, () -> {});
        FluidStack water = new FluidStack(Fluids.WATER, 4_000);
        ItemStack emeralds = new ItemStack(Items.EMERALD, 12);
        fluids.put(water, 4_000L);
        items.put(emeralds, 12L);

        assertTrue(inventory.setFluidTarget(4, water));
        assertEquals(4_000, inventory.replenishSlot(4, TerminalStorageAction.EXECUTE));
        assertEquals(0L, fluids.amount(water));

        assertTrue(inventory.setTarget(4, emeralds));
        assertEquals(4_000L, fluids.amount(water));
        assertTrue(inventory.getBufferedFluid(4).isEmpty());
        assertEquals(12, inventory.replenishSlot(4, TerminalStorageAction.EXECUTE));
        assertEquals(12, inventory.getBufferedStack(4).getCount());
    }

    @Test
    void fluidPipeAccessIgnoresActiveModesButExtractionKeepsSlotFaceMask() {
        FakeItems items = new FakeItems();
        FakeFluids fluids = new FakeFluids();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(
                items, fluids, () -> true, () -> {}, () -> {});
        FluidStack water = new FluidStack(Fluids.WATER, 1_000);
        fluids.put(water, 2_000L);
        assertTrue(inventory.setFluidTarget(0, water));
        assertEquals(1_000, inventory.replenishAllSlots(TerminalStorageAction.EXECUTE));

        assertTrue(inventory.setOutputFaceEnabled(0, net.minecraft.core.Direction.EAST, true));
        IFluidHandler stable = new XianqiaoInterfaceSidedFluidHandler(
                new XianqiaoInterfaceFluidInventory(inventory), inventory,
                net.minecraft.core.Direction.EAST);
        assertEquals(500, stable.drain(500, IFluidHandler.FluidAction.EXECUTE).getAmount());
        IFluidHandler blocked = new XianqiaoInterfaceSidedFluidHandler(
                new XianqiaoInterfaceFluidInventory(inventory), inventory,
                net.minecraft.core.Direction.WEST);
        assertEquals(500, blocked.fill(water.copyWithAmount(500), IFluidHandler.FluidAction.EXECUTE));
        assertEquals(1_000L, fluids.amount(water));
        assertTrue(blocked.drain(500, IFluidHandler.FluidAction.EXECUTE).isEmpty());
    }

    @Test
    void directAmountsAreServerClampedByResourceKind() {
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(
                new FakeItems(), new FakeFluids(), () -> true, () -> {}, () -> {});
        assertTrue(inventory.setTarget(0, new ItemStack(Items.ENDER_PEARL)));
        assertTrue(inventory.setFluidTarget(1, new FluidStack(Fluids.WATER, 1)));

        assertTrue(inventory.setTargetAmount(0, Long.MAX_VALUE));
        assertTrue(inventory.setFluidTargetAmount(1, Long.MAX_VALUE));
        assertEquals(128, inventory.getTarget(0).getCount());
        assertEquals(16_000, inventory.getFluidTarget(1).getAmount());
    }

    @Test
    void sameFluidTargetDecreaseReturnsOnlyExcessAndRetriesRejectedRemainder() {
        FakeFluids fluids = new FakeFluids();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(
                new FakeItems(), fluids, () -> true, () -> {}, () -> {});
        FluidStack water = new FluidStack(Fluids.WATER, 1);
        fluids.put(water, 16_000L);
        assertTrue(inventory.setFluidTarget(0, water.copyWithAmount(16_000)));
        assertEquals(16_000, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));

        fluids.setInsertCapacity(3_000L);
        assertTrue(inventory.setFluidTargetAmount(0, 4_000L));
        assertEquals(16_000, inventory.getBufferedFluid(0).getAmount());
        assertEquals(3_000, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        assertEquals(13_000, inventory.getBufferedFluid(0).getAmount());
        assertEquals(3_000L, fluids.amount(water));

        fluids.setInsertCapacity(0L);
        assertEquals(0, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        assertEquals(13_000, inventory.getBufferedFluid(0).getAmount());

        fluids.setInsertCapacity(Long.MAX_VALUE);
        assertEquals(9_000, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        assertEquals(4_000, inventory.getBufferedFluid(0).getAmount());
        assertEquals(12_000L, fluids.amount(water));
    }

    @Test
    void hotLimitReductionCreatesRetryableItemAndFluidExcessPlans() {
        FakeItems items = new FakeItems();
        FakeFluids fluids = new FakeFluids();
        AtomicReference<XianqiaoInterfaceLimits.Snapshot> limits = new AtomicReference<>(
                new XianqiaoInterfaceLimits.Snapshot(128, 16_000));
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(
                items, fluids, () -> true, () -> {}, () -> {}, limits::get);
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        FluidStack water = new FluidStack(Fluids.WATER, 1);
        items.put(diamonds, 128L);
        fluids.put(water, 16_000L);
        assertTrue(inventory.setTarget(0, diamonds.copyWithCount(128)));
        assertTrue(inventory.setFluidTarget(1, water.copyWithAmount(16_000)));
        assertEquals(16_128L, inventory.replenishAllSlots(TerminalStorageAction.EXECUTE));

        items.setInsertCapacity(0L);
        fluids.setInsertCapacity(0L);
        limits.set(new XianqiaoInterfaceLimits.Snapshot(32, 4_000));
        assertEquals(0, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        assertEquals(32, inventory.getTarget(0).getCount());
        assertEquals(4_000, inventory.getFluidTarget(1).getAmount());
        assertEquals(128, inventory.getBufferedStack(0).getCount());
        assertEquals(16_000, inventory.getBufferedFluid(1).getAmount());

        items.setInsertCapacity(Long.MAX_VALUE);
        fluids.setInsertCapacity(Long.MAX_VALUE);
        assertEquals(96, inventory.replenishSlot(0, TerminalStorageAction.EXECUTE));
        assertEquals(12_000, inventory.replenishSlot(1, TerminalStorageAction.EXECUTE));
        assertEquals(32, inventory.getBufferedStack(0).getCount());
        assertEquals(4_000, inventory.getBufferedFluid(1).getAmount());
    }

    @Test
    void nbtRoundTripPreservesSlotIndicesAndDuplicateFluidIdentities() {
        FakeItems items = new FakeItems();
        FakeFluids fluids = new FakeFluids();
        XianqiaoInterfaceInventory original = new XianqiaoInterfaceInventory(
                items, fluids, () -> true, () -> {}, () -> {});
        FluidStack water = new FluidStack(Fluids.WATER, 1);
        fluids.put(water, 8_000L);
        assertTrue(original.setFluidTarget(2, water.copyWithAmount(4_000)));
        assertTrue(original.setFluidTarget(7, water.copyWithAmount(2_000)));
        assertEquals(6_000L, original.replenishAllSlots(TerminalStorageAction.EXECUTE));

        CompoundTag saved = new CompoundTag();
        original.saveState(saved, registries);
        CompoundTag invalidIndex = saved.getListOrEmpty("FluidSlots")
                .getCompoundOrEmpty(0).copy();
        invalidIndex.putInt("Slot", 99);
        saved.getListOrEmpty("FluidSlots").add(invalidIndex);
        XianqiaoInterfaceInventory restored = new XianqiaoInterfaceInventory(
                new FakeItems(), new FakeFluids(), () -> true, () -> {}, () -> {});
        restored.loadState(saved, registries);

        assertEquals(4_000, restored.getFluidTarget(2).getAmount());
        assertEquals(4_000, restored.getBufferedFluid(2).getAmount());
        assertEquals(2_000, restored.getFluidTarget(7).getAmount());
        assertEquals(2_000, restored.getBufferedFluid(7).getAmount());
        assertTrue(restored.getFluidTarget(0).isEmpty());
        assertTrue(restored.getFluidTarget(8).isEmpty());
    }

    @Test
    void overStackTargetsPersistAsCountOneIdentityAndConfigDownshiftKeepsRealCaches() {
        FakeItems items = new FakeItems();
        FakeFluids fluids = new FakeFluids();
        ItemStack pearls = new ItemStack(Items.ENDER_PEARL);
        FluidStack water = new FluidStack(Fluids.WATER, 1);
        items.put(pearls, 128L);
        fluids.put(water, 16_000L);
        XianqiaoInterfaceInventory original = new XianqiaoInterfaceInventory(
                items, fluids, () -> true, () -> {}, () -> {},
                () -> new XianqiaoInterfaceLimits.Snapshot(128, 16_000));

        assertTrue(original.setTarget(0, pearls.copyWithCount(128)));
        assertTrue(original.setFluidTarget(1, water.copyWithAmount(16_000)));
        assertEquals(16_128L, original.replenishAllSlots(TerminalStorageAction.EXECUTE));

        CompoundTag saved = new CompoundTag();
        original.saveState(saved, registries);
        CompoundTag itemEntry = saved.getListOrEmpty("ItemSlots").getCompoundOrEmpty(0);
        ItemStack encodedIdentity = com.immortalstorage.immortalstorage.compat.mc2612.CompatCodec.parseItemStack(
                registries, itemEntry.getCompoundOrEmpty("Item"));
        assertEquals(1, encodedIdentity.getCount(),
                "ItemStack codec state is identity-only; desired/cached longs own amount 128");
        assertEquals(128L, itemEntry.getLongOr("Desired", 0L));
        assertEquals(128L, itemEntry.getLongOr("Cached", 0L));

        XianqiaoInterfaceInventory restored = new XianqiaoInterfaceInventory(
                new FakeItems(), new FakeFluids(), () -> true, () -> {}, () -> {},
                () -> new XianqiaoInterfaceLimits.Snapshot(32, 4_000));
        restored.loadState(saved, registries);

        assertEquals(32, restored.getTarget(0).getCount(),
                "current server config clamps the desired item target on load");
        assertEquals(4_000, restored.getFluidTarget(1).getAmount(),
                "current server config clamps the desired fluid target on load");
        assertEquals(128, restored.getBufferedStack(0).getCount(),
                "a config downshift must not silently destroy already cached items");
        assertEquals(16_000, restored.getBufferedFluid(1).getAmount(),
                "a config downshift must not silently destroy already cached fluid");
    }

    @Test
    void removalReturnsBothResourceKindsWithoutLoss() {
        FakeItems items = new FakeItems();
        FakeFluids fluids = new FakeFluids();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(
                items, fluids, () -> true, () -> {}, () -> {});
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 20);
        FluidStack water = new FluidStack(Fluids.WATER, 2_000);
        items.put(diamonds, 20L);
        fluids.put(water, 2_000L);
        assertTrue(inventory.setTarget(0, diamonds));
        assertTrue(inventory.setFluidTarget(1, water));
        assertEquals(2_020L, inventory.replenishAllSlots(TerminalStorageAction.EXECUTE));

        inventory.returnFluidBuffersAndRetainRemainders();
        assertTrue(inventory.returnBuffersAndCollectRemainders().isEmpty());

        assertEquals(20L, items.amount(diamonds));
        assertEquals(2_000L, fluids.amount(water));
        assertTrue(inventory.getBufferedStack(0).isEmpty());
        assertTrue(inventory.getBufferedFluid(1).isEmpty());
    }

    @Test
    void unreturnedFluidRemainderSurvivesInsideDroppedBlockState() {
        FakeFluids fluids = new FakeFluids();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(
                new FakeItems(), fluids, () -> true, () -> {}, () -> {});
        FluidStack water = new FluidStack(Fluids.WATER, 2_000);
        fluids.put(water, 2_000L);
        assertTrue(inventory.setFluidTarget(5, water));
        assertEquals(2_000L, inventory.replenishAllSlots(TerminalStorageAction.EXECUTE));
        fluids.setInsertCapacity(0L);

        inventory.returnFluidBuffersAndRetainRemainders();
        assertEquals(2_000, inventory.getBufferedFluid(5).getAmount());
        CompoundTag saved = new CompoundTag();
        inventory.saveState(saved, registries);
        XianqiaoInterfaceInventory restored = new XianqiaoInterfaceInventory(
                new FakeItems(), new FakeFluids(), () -> true, () -> {}, () -> {});
        restored.loadState(saved, registries);
        assertEquals(2_000, restored.getBufferedFluid(5).getAmount());
    }

    @Test
    void failedMultiSlotRemovalKeepsEveryUncommittedAmountRetryable() {
        FakeItems items = new FakeItems();
        AtomicInteger dirty = new AtomicInteger();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(
                items, new FakeFluids(), () -> true, dirty::incrementAndGet, () -> {});
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 10);
        ItemStack emeralds = new ItemStack(Items.EMERALD, 10);
        items.put(diamonds, 10L);
        items.put(emeralds, 10L);
        assertTrue(inventory.setTarget(0, diamonds));
        assertTrue(inventory.setTarget(1, emeralds));
        assertEquals(20L, inventory.replenishAllSlots(TerminalStorageAction.EXECUTE));
        items.throwOnInsert(emeralds);
        dirty.set(0);

        assertThrows(IllegalStateException.class, inventory::returnBuffersAndCollectRemainders);
        assertEquals(10L, items.amount(diamonds));
        assertEquals(10, inventory.getBufferedStack(1).getCount());
        assertTrue(dirty.get() > 0,
                "the first committed decrement must be persisted before a later slot throws");

        items.clearInsertFailure();
        assertTrue(inventory.returnBuffersAndCollectRemainders().isEmpty());
        assertEquals(10L, items.amount(emeralds));
        assertTrue(inventory.getBufferedStack(1).isEmpty());
    }

    private static final class FakeItems implements TerminalItemStorage {
        private final Map<TerminalEntryKey, Long> amounts = new LinkedHashMap<>();
        private long revision;
        private TerminalEntryKey failedInsert;
        private long insertCapacity = Long.MAX_VALUE;

        void put(ItemStack stack, long amount) { amounts.put(TerminalEntryKey.of(stack), amount); }
        long amount(ItemStack stack) { return amounts.getOrDefault(TerminalEntryKey.of(stack), 0L); }
        void throwOnInsert(ItemStack stack) { failedInsert = TerminalEntryKey.of(stack); }
        void clearInsertFailure() { failedInsert = null; }
        void setInsertCapacity(long amount) { insertCapacity = Math.max(0L, amount); }
        @Override public long revision() { return revision; }
        @Override public List<StorageItemSummary> snapshot() { return List.of(); }
        @Override public long insert(TerminalEntryKey key, long amount, TerminalStorageAction action) {
            if (amount <= 0L) return 0L;
            if (key.equals(failedInsert)) throw new IllegalStateException("synthetic storage failure");
            long accepted = Math.min(amount, insertCapacity);
            if (action.executes() && accepted > 0L) {
                amounts.merge(key, accepted, Long::sum);
                revision++;
            }
            return accepted;
        }
        @Override public long extract(TerminalEntryKey key, long amount, TerminalStorageAction action) {
            long moved = Math.min(amount, amounts.getOrDefault(key, 0L));
            if (action.executes() && moved > 0L) {
                amounts.put(key, amounts.get(key) - moved);
                revision++;
            }
            return moved;
        }
    }

    private static final class FakeFluids implements TerminalFluidStorage {
        private final Map<TerminalFluidKey, Long> amounts = new LinkedHashMap<>();
        private long revision;
        private long insertCapacity = Long.MAX_VALUE;

        void put(FluidStack stack, long amount) { amounts.put(TerminalFluidKey.of(stack), amount); }
        long amount(FluidStack stack) { return amounts.getOrDefault(TerminalFluidKey.of(stack), 0L); }
        void setInsertCapacity(long amount) { insertCapacity = Math.max(0L, amount); }
        @Override public long revision() { return revision; }
        @Override public Map<TerminalFluidKey, Long> snapshot() { return Map.copyOf(amounts); }
        @Override public long insert(TerminalFluidKey key, long amount, TerminalStorageAction action) {
            if (amount <= 0L) return 0L;
            long accepted = Math.min(amount, insertCapacity);
            if (action.executes() && accepted > 0L) { amounts.merge(key, accepted, Long::sum); revision++; }
            return accepted;
        }
        @Override public long extract(TerminalFluidKey key, long amount, TerminalStorageAction action) {
            long moved = Math.min(amount, amounts.getOrDefault(key, 0L));
            if (action.executes() && moved > 0L) {
                amounts.put(key, amounts.get(key) - moved);
                revision++;
            }
            return moved;
        }
    }
}
