package com.cultivation.cultivation.network.storage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.Objects;
import java.util.function.Consumer;

/** Conservation-first transfer from an int-sized external handler into an arbitrary target. */
public final class ItemHandlerTransferTransaction {
    private ItemHandlerTransferTransaction() {}

    public static Result moveSlot(IItemHandler source, int slot, int maxAmount,
                                  InsertTarget target, Consumer<ItemStack> overflowSink) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Consumer<ItemStack> overflow = overflowSink == null ? ignored -> {} : overflowSink;
        if (slot < 0 || slot >= source.getSlots() || maxAmount <= 0) return Result.EMPTY;

        ItemStack sourceSimulation = source.extractItem(slot, maxAmount, true);
        if (sourceSimulation.isEmpty()) return Result.EMPTY;
        ItemStack targetSimulationRemainder = target.insert(sourceSimulation.copy(), true);
        int planned = accepted(sourceSimulation, targetSimulationRemainder);
        if (planned <= 0) return Result.EMPTY;

        ItemStack extracted = source.extractItem(slot, planned, false);
        if (extracted.isEmpty()) return new Result(planned, 0, 0, 0, 0);

        // Re-simulate the exact stack actually returned by the external source;
        // it may have changed identity or quantity since the first simulation.
        int inserted = 0;
        try {
            ItemStack exactSimulationRemainder = target.insert(extracted.copy(), true);
            int exactPlanned = accepted(extracted, exactSimulationRemainder);
            if (exactPlanned > 0) {
                ItemStack offer = extracted.copyWithCount(exactPlanned);
                ItemStack executionRemainder = target.insert(offer, false);
                inserted = accepted(offer, executionRemainder);
            }
        } catch (RuntimeException targetFailure) {
            // The built-in target used by the staff is failure-atomic. Restore
            // the already extracted source stack if an adapter rejects before
            // commit instead of leaving it detached from both inventories.
            inserted = 0;
        }

        int toRestoreCount = extracted.getCount() - inserted;
        int restored = 0;
        int overflowed = 0;
        if (toRestoreCount > 0) {
            ItemStack toRestore = extracted.copyWithCount(toRestoreCount);
            ItemStack restoreRemainder = ItemHandlerHelper.insertItemStacked(source, toRestore, false);
            restored = toRestoreCount - (restoreRemainder.isEmpty() ? 0 : restoreRemainder.getCount());
            if (!restoreRemainder.isEmpty()) {
                overflowed = restoreRemainder.getCount();
                overflow.accept(restoreRemainder.copy());
            }
        }
        return new Result(planned, extracted.getCount(), inserted, restored, overflowed);
    }

    private static int accepted(ItemStack offered, ItemStack remainder) {
        if (offered == null || offered.isEmpty()) return 0;
        if (remainder == null || remainder.isEmpty()) return offered.getCount();
        return Math.max(0, offered.getCount() - Math.min(offered.getCount(), remainder.getCount()));
    }

    @FunctionalInterface
    public interface InsertTarget {
        ItemStack insert(ItemStack stack, boolean simulate);
    }

    public record Result(int planned, int extracted, int inserted, int restored, int overflowed) {
        public static final Result EMPTY = new Result(0, 0, 0, 0, 0);

        public Result {
            if (planned < 0 || extracted < 0 || inserted < 0 || restored < 0 || overflowed < 0
                    || inserted + restored + overflowed != extracted) {
                throw new IllegalArgumentException("item transfer result violates conservation");
            }
        }

        public boolean changedSource() {
            return extracted > 0;
        }
    }
}
