package com.immortalstorage.immortalstorage.network.storage;

import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ItemHandlerTransferTransactionTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void executeRefusalRestoresTheSourceAndDoesNotAdvance() {
        ItemStackHandler source = new ItemStackHandler(1);
        source.setStackInSlot(0, new ItemStack(Items.DIAMOND, 64));

        var result = ItemHandlerTransferTransaction.moveSlot(
                source, 0, Integer.MAX_VALUE,
                (stack, simulate) -> simulate ? ItemStack.EMPTY : stack.copy(),
                ignored -> {});

        assertEquals(64, result.extracted());
        assertEquals(0, result.inserted());
        assertEquals(64, result.restored());
        assertEquals(64, source.getStackInSlot(0).getCount());
    }

    @Test
    void partialSourceExecutionMovesOnlyTheItemsActuallyExtracted() {
        PartialExtractHandler source = new PartialExtractHandler(64, 17, true);
        ItemStackHandler destination = new ItemStackHandler(1);

        var result = ItemHandlerTransferTransaction.moveSlot(
                source, 0, Integer.MAX_VALUE,
                (stack, simulate) -> destination.insertItem(0, stack, simulate),
                ignored -> {});

        assertEquals(17, result.extracted());
        assertEquals(17, result.inserted());
        assertEquals(47, source.getStackInSlot(0).getCount());
        assertEquals(17, destination.getStackInSlot(0).getCount());
    }

    @Test
    void unrecoverableRemainderIsReportedToOverflowWithoutLoss() {
        PartialExtractHandler source = new PartialExtractHandler(64, 64, false);
        AtomicInteger overflow = new AtomicInteger();

        var result = ItemHandlerTransferTransaction.moveSlot(
                source, 0, Integer.MAX_VALUE,
                (stack, simulate) -> simulate ? ItemStack.EMPTY : stack.copy(),
                stack -> overflow.addAndGet(stack.getCount()));

        assertEquals(64, result.extracted());
        assertEquals(0, result.inserted());
        assertEquals(0, result.restored());
        assertEquals(64, result.overflowed());
        assertEquals(64, overflow.get());
    }

    @Test
    void targetExceptionBeforeMutationRestoresTheExtractedChunk() {
        ItemStackHandler source = new ItemStackHandler(1);
        source.setStackInSlot(0, new ItemStack(Items.DIAMOND, 32));
        AtomicInteger calls = new AtomicInteger();

        var result = ItemHandlerTransferTransaction.moveSlot(
                source, 0, Integer.MAX_VALUE,
                (stack, simulate) -> {
                    if (calls.incrementAndGet() >= 3 && !simulate) throw new IllegalStateException("rejected");
                    return ItemStack.EMPTY;
                },
                ignored -> {});

        assertEquals(0, result.inserted());
        assertEquals(32, result.restored());
        assertEquals(32, source.getStackInSlot(0).getCount());
    }

    private static final class PartialExtractHandler implements IItemHandler {
        private ItemStack stack;
        private final int executeLimit;
        private final boolean acceptsInsert;

        private PartialExtractHandler(int count, int executeLimit, boolean acceptsInsert) {
            this.stack = new ItemStack(Items.DIAMOND, count);
            this.executeLimit = executeLimit;
            this.acceptsInsert = acceptsInsert;
        }

        @Override public int getSlots() { return 1; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return stack.copy(); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack offered, boolean simulate) {
            if (!acceptsInsert) return offered.copy();
            if (!simulate) {
                if (stack.isEmpty()) stack = offered.copy();
                else stack.grow(offered.getCount());
            }
            return ItemStack.EMPTY;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            int taken = Math.min(stack.getCount(), simulate ? amount : Math.min(amount, executeLimit));
            if (taken <= 0) return ItemStack.EMPTY;
            ItemStack result = stack.copyWithCount(taken);
            if (!simulate) stack.shrink(taken);
            return result;
        }
        @Override public int getSlotLimit(int slot) { return Integer.MAX_VALUE; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack offered) { return acceptsInsert; }
    }
}
