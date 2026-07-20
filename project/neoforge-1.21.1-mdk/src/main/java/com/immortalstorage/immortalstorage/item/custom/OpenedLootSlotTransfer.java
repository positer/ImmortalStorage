package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Conservation-first transfer from one opened player-menu slot into personal storage. */
public final class OpenedLootSlotTransfer {
    private OpenedLootSlotTransfer() {}

    public static Result move(Source source, InsertTarget target) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");

        ItemStack visible = source.peek();
        if (visible == null || visible.isEmpty()) return Result.EMPTY;
        int planned = accepted(visible, target.insert(visible.copy(), true));
        if (planned <= 0) return Result.EMPTY;

        ItemStack extracted = source.extract(planned);
        if (extracted == null || extracted.isEmpty()) return new Result(planned, 0, 0, 0);

        int exactPlan = accepted(extracted, target.insert(extracted.copy(), true));
        int committed = 0;
        if (exactPlan > 0) {
            ItemStack offer = extracted.copyWithCount(exactPlan);
            committed = accepted(offer, target.insert(offer, false));
        }

        int restore = extracted.getCount() - committed;
        if (restore > 0) source.restore(extracted.copyWithCount(restore));
        return new Result(planned, extracted.getCount(), committed, restore);
    }

    private static int accepted(ItemStack offered, ItemStack remainder) {
        if (offered == null || offered.isEmpty()) return 0;
        if (remainder == null || remainder.isEmpty()) return offered.getCount();
        if (!ItemStack.isSameItemSameComponents(offered, remainder)
                || remainder.getCount() < 0 || remainder.getCount() > offered.getCount()) {
            throw new IllegalStateException("Personal storage returned an invalid insertion remainder");
        }
        return offered.getCount() - remainder.getCount();
    }

    public interface Source {
        ItemStack peek();
        ItemStack extract(int amount);
        void restore(ItemStack stack);
    }

    @FunctionalInterface
    public interface InsertTarget {
        ItemStack insert(ItemStack stack, boolean simulate);
    }

    public record Result(int planned, int extracted, int committed, int restored) {
        public static final Result EMPTY = new Result(0, 0, 0, 0);

        public Result {
            if (planned < 0 || extracted < 0 || committed < 0 || restored < 0
                    || committed + restored != extracted) {
                throw new IllegalArgumentException("Opened loot transfer violates conservation");
            }
        }
    }
}
