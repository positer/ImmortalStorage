package com.immortalstorage.immortalstorage.compat.beyonddimensions;

import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BeyondDimensionsStorageAdapterTest {
    @Test
    void itemSimulationAndExecutionUseOfficialRemainderSemanticsOnce() {
        UnifiedStorage storage = storage(10L, 8);
        AtomicInteger changes = new AtomicInteger();
        BeyondDimensionsItemStorage adapter = new BeyondDimensionsItemStorage(storage, changes::incrementAndGet);
        ItemStack stack = namedItem("alpha");
        TerminalEntryKey key = TerminalEntryKey.of(stack);

        assertEquals(10L, adapter.insert(key, 14L, TerminalStorageAction.SIMULATE));
        assertTrue(adapter.snapshot().isEmpty());
        assertEquals(10L, adapter.insert(key, 14L, TerminalStorageAction.EXECUTE));
        assertEquals(10L, adapter.snapshot().getFirst().amount());
        assertEquals(1, changes.get());

        assertEquals(6L, adapter.extract(key, 6L, TerminalStorageAction.SIMULATE));
        assertEquals(10L, adapter.snapshot().getFirst().amount());
        assertEquals(6L, adapter.extract(key, 6L, TerminalStorageAction.EXECUTE));
        assertEquals(4L, adapter.snapshot().getFirst().amount());
        assertEquals(2, changes.get());
    }

    @Test
    void itemIdentityPreservesCompleteDataComponents() {
        UnifiedStorage storage = storage(Long.MAX_VALUE, 8);
        BeyondDimensionsItemStorage adapter = new BeyondDimensionsItemStorage(storage, () -> {});
        ItemStack alpha = namedItem("alpha");
        ItemStack beta = namedItem("beta");

        assertEquals(3L, adapter.insert(TerminalEntryKey.of(alpha), 3L, TerminalStorageAction.EXECUTE));
        assertEquals(5L, adapter.insert(TerminalEntryKey.of(beta), 5L, TerminalStorageAction.EXECUTE));

        assertEquals(2, adapter.snapshot().size());
        assertTrue(adapter.snapshot().stream().anyMatch(summary ->
                summary.amount() == 3L && Component.literal("alpha").equals(
                        summary.prototype().get(DataComponents.CUSTOM_NAME))));
        assertTrue(adapter.snapshot().stream().anyMatch(summary ->
                summary.amount() == 5L && Component.literal("beta").equals(
                        summary.prototype().get(DataComponents.CUSTOM_NAME))));
    }

    @Test
    void fluidIdentityAndLongAmountsUseTheOfficialUnifiedStorage() {
        UnifiedStorage storage = storage(Long.MAX_VALUE, 8);
        BeyondDimensionsFluidStorage adapter = new BeyondDimensionsFluidStorage(storage, () -> {});
        FluidStack alpha = namedWater("alpha");
        FluidStack beta = namedWater("beta");

        assertEquals(4_000_000_000L, adapter.insert(
                TerminalFluidKey.of(alpha), 4_000_000_000L, TerminalStorageAction.SIMULATE));
        assertTrue(adapter.snapshot().isEmpty());
        assertEquals(4_000_000_000L, adapter.insert(
                TerminalFluidKey.of(alpha), 4_000_000_000L, TerminalStorageAction.EXECUTE));
        assertEquals(7_000L, adapter.insert(
                TerminalFluidKey.of(beta), 7_000L, TerminalStorageAction.EXECUTE));

        assertEquals(2, adapter.snapshot().size());
        assertEquals(4_000_000_000L, adapter.snapshot().get(TerminalFluidKey.of(alpha)));
        assertEquals(7_000L, adapter.snapshot().get(TerminalFluidKey.of(beta)));
        assertEquals(2_000_000_000L, adapter.extract(
                TerminalFluidKey.of(alpha), 2_000_000_000L, TerminalStorageAction.SIMULATE));
        assertEquals(4_000_000_000L, adapter.snapshot().get(TerminalFluidKey.of(alpha)));
    }

    private static UnifiedStorage storage(long capacity, int slots) {
        return new UnifiedStorage(null, AbstractUnorderedStackHandler.UiTimestampPolicy.NONE, capacity, slots);
    }

    private static ItemStack namedItem(String name) {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static FluidStack namedWater(String name) {
        FluidStack stack = new FluidStack(Fluids.WATER, 1);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }
}
