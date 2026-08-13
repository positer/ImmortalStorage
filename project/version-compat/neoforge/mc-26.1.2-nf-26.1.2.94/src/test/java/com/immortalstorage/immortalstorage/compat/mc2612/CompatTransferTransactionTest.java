package com.immortalstorage.immortalstorage.compat.mc2612;

import com.immortalstorage.immortalstorage.compat.CompatTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompatTransferTransactionTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        CompatTestBootstrap.bootstrap();
    }

    @Test
    void abortedAe2ProbeDoesNotMutateLegacyInventory() {
        MutableSlot legacy = new MutableSlot(new ItemStack(Items.IRON_INGOT, 8));
        var handler = CompatTransfer.item(legacy);

        try (var transaction = Transaction.openRoot()) {
            assertEquals(5, handler.extract(0, ItemResource.of(Items.IRON_INGOT), 5, transaction));
            assertEquals(8, legacy.stack.getCount(), "legacy mutation must wait for root commit");
        }

        assertEquals(8, legacy.stack.getCount(), "aborted simulation must not consume items");
        assertEquals(0, legacy.executeExtractCalls);
    }

    @Test
    void committedTransactionMutatesExactlyOnce() {
        MutableSlot legacy = new MutableSlot(new ItemStack(Items.IRON_INGOT, 8));
        var handler = CompatTransfer.item(legacy);

        try (var transaction = Transaction.openRoot()) {
            assertEquals(5, handler.extract(0, ItemResource.of(Items.IRON_INGOT), 5, transaction));
            transaction.commit();
        }

        assertEquals(3, legacy.stack.getCount());
        assertEquals(1, legacy.executeExtractCalls);
    }

    @Test
    void requestedResourceMustMatchDynamicSlotContents() {
        MutableSlot legacy = new MutableSlot(new ItemStack(Items.IRON_INGOT, 8));
        var handler = CompatTransfer.item(legacy);

        try (var transaction = Transaction.openRoot()) {
            assertEquals(0, handler.extract(0, ItemResource.of(Items.GOLD_INGOT), 5, transaction));
            transaction.commit();
        }

        assertEquals(8, legacy.stack.getCount());
        assertEquals(Items.IRON_INGOT, legacy.stack.getItem());
        assertEquals(0, legacy.executeExtractCalls);
    }

    @Test
    void committedInsertIsDeferredAndAppliedOnce() {
        MutableSlot legacy = new MutableSlot(ItemStack.EMPTY);
        var handler = CompatTransfer.item(legacy);

        try (var transaction = Transaction.openRoot()) {
            assertEquals(7, handler.insert(0, ItemResource.of(Items.DIAMOND), 7, transaction));
            assertEquals(0, legacy.stack.getCount());
            transaction.commit();
        }

        assertEquals(7, legacy.stack.getCount());
        assertEquals(Items.DIAMOND, legacy.stack.getItem());
        assertEquals(1, legacy.executeInsertCalls);
    }

    @Test
    void repeatedInsertInOneTransactionNeverOverPromisesCapacity() {
        MutableSlot legacy = new MutableSlot(ItemStack.EMPTY);
        var handler = CompatTransfer.item(legacy);

        try (var transaction = Transaction.openRoot()) {
            assertEquals(40, handler.insert(0, ItemResource.of(Items.DIAMOND), 40, transaction));
            assertEquals(24, handler.insert(0, ItemResource.of(Items.DIAMOND), 40, transaction));
            transaction.commit();
        }

        assertEquals(64, legacy.stack.getCount());
        assertEquals(2, legacy.executeInsertCalls);
    }

    private static final class MutableSlot implements IItemHandler {
        private ItemStack stack;
        private int executeInsertCalls;
        private int executeExtractCalls;

        private MutableSlot(ItemStack stack) {
            this.stack = stack.copy();
        }

        @Override public int getSlots() { return 1; }

        @Override public @NotNull ItemStack getStackInSlot(int slot) {
            return slot == 0 ? stack.copy() : ItemStack.EMPTY;
        }

        @Override public @NotNull ItemStack insertItem(
                int slot, @NotNull ItemStack offered, boolean simulate) {
            if (slot != 0 || offered.isEmpty()) return offered;
            if (!stack.isEmpty() && !ItemStack.isSameItemSameComponents(stack, offered)) return offered;
            int accepted = Math.min(offered.getCount(), 64 - stack.getCount());
            if (!simulate && accepted > 0) {
                executeInsertCalls++;
                if (stack.isEmpty()) stack = offered.copyWithCount(accepted);
                else stack.grow(accepted);
            }
            return accepted == offered.getCount() ? ItemStack.EMPTY
                    : offered.copyWithCount(offered.getCount() - accepted);
        }

        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0 || stack.isEmpty()) return ItemStack.EMPTY;
            int extracted = Math.min(amount, stack.getCount());
            ItemStack result = stack.copyWithCount(extracted);
            if (!simulate) {
                executeExtractCalls++;
                stack.shrink(extracted);
            }
            return result;
        }

        @Override public int getSlotLimit(int slot) { return slot == 0 ? 64 : 0; }

        @Override public boolean isItemValid(int slot, @NotNull ItemStack offered) {
            return slot == 0 && !offered.isEmpty();
        }
    }
}
