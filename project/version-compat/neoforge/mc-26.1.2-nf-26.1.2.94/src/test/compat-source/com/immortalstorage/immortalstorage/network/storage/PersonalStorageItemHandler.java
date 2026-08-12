package com.immortalstorage.immortalstorage.network.storage;

import com.immortalstorage.core.amount.LongAmountBridge;
import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Standard NeoForge item-handler bridge over the owner's currently active
 * personal storage (Kongqiao at stages 1-5, Xianqiao at stages 6+).
 *
 * The handler intentionally ignores requested insert slot and lets the backing
 * storage merge efficiently across the whole owner inventory, matching network
 * storage behavior rather than a fixed chest-slot contract.
 */
public final class PersonalStorageItemHandler implements IItemHandler {
    private final ImmortalStoragePlayerData data;
    private final Runnable onChanged;
    private final BooleanSupplier accessAllowed;
    private final boolean kongqiaoLayout;
    private final boolean infiniteImmortalYuanLayout;
    private final TerminalItemStorage combinedStorage;
    private long cachedGeneration = Long.MIN_VALUE;
    private final List<StorageItemSummary> cachedLogicalEntries = new ArrayList<>();

    private boolean isAvailable() {
        return data.getStage() >= 1
                && data.isStorageIsKongqiaoLegacy() == kongqiaoLayout
                && (kongqiaoLayout || data.isInfiniteImmortalYuan() == infiniteImmortalYuanLayout)
                && accessAllowed.getAsBoolean();
    }

    public PersonalStorageItemHandler(ImmortalStoragePlayerData data, net.minecraft.core.HolderLookup.Provider registryAccess, Runnable onChanged) {
        this(data, registryAccess, onChanged, () -> data != null && data.getStage() >= 1);
    }

    public PersonalStorageItemHandler(ImmortalStoragePlayerData data, net.minecraft.core.HolderLookup.Provider registryAccess,
                                      Runnable onChanged, BooleanSupplier accessAllowed) {
        this(data, registryAccess, onChanged, accessAllowed, null);
    }

    public PersonalStorageItemHandler(ImmortalStoragePlayerData data, net.minecraft.core.HolderLookup.Provider registryAccess,
                                      Runnable onChanged, BooleanSupplier accessAllowed,
                                      TerminalItemStorage combinedStorage) {
        if (data == null) throw new IllegalArgumentException("player data is required");
        this.data = data;
        this.onChanged = onChanged == null ? () -> {} : onChanged;
        this.accessAllowed = accessAllowed == null ? () -> false : accessAllowed;
        this.kongqiaoLayout = data.isStorageIsKongqiaoLegacy();
        this.infiniteImmortalYuanLayout = !kongqiaoLayout && data.isInfiniteImmortalYuan();
        this.combinedStorage = combinedStorage;
    }

    @Override
    public int getSlots() {
        if (!isAvailable()) return 0;
        int stored = kongqiaoLayout
                ? data.getKongqiaoMaxSlots() : logicalEntries().size();
        long exposed = (long) stored
                + (kongqiaoLayout ? 0L : 1L)
                + (infiniteImmortalYuanLayout ? 1L : 0L);
        return (int) Math.min(Integer.MAX_VALUE, exposed);
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (!isAvailable() || slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
        if (isVirtualImmortalYuanSlot(slot)) {
            ItemStack prototype = data.getInfiniteImmortalYuanPrototype();
            return prototype.isEmpty() ? ItemStack.EMPTY
                    : prototype.copyWithCount(LongAmountBridge.saturatingInt(data.getImmortalYuan()));
        }
        if (isImportSlot(slot)) return ItemStack.EMPTY;
        if (kongqiaoLayout) {
            return data.getKongqiaoItems().get(storedIndex(slot)).copy();
        }
        StorageItemSummary summary = logicalEntries().get(storedIndex(slot));
        if (summary == null) return ItemStack.EMPTY;
        return summary.prototype().copyWithCount(LongAmountBridge.saturatingInt(summary.amount()));
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isAvailable() || slot < 0 || slot >= getSlots()) return stack;
        if (data.isVirtualInfiniteImmortalYuanStack(stack)) return ItemStack.EMPTY;
        if (simulate) {
            return data.simulateInsertStack(stack);
        }
        ItemStack leftover = data.insertStack(stack.copy(), true);
        if (leftover.getCount() != stack.getCount()) {
            onChanged.run();
        }
        return leftover;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!isAvailable() || amount <= 0 || slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
        if (isVirtualImmortalYuanSlot(slot)) {
            ItemStack prototype = data.getInfiniteImmortalYuanPrototype();
            if (prototype.isEmpty()) return ItemStack.EMPTY;
            // The logical int view is MAX, but one IItemHandler call must still
            // return a legal ItemStack. Int-sized per-tick throughput is reached
            // by an adapter making repeated max-stack calls without decrement.
            int extracted = Math.min(amount, Math.max(1, prototype.getMaxStackSize()));
            return extracted <= 0 ? ItemStack.EMPTY : prototype.copyWithCount(extracted);
        }
        if (isImportSlot(slot)) return ItemStack.EMPTY;
        ItemStack visible = getStackInSlot(slot);
        if (visible.isEmpty()) return ItemStack.EMPTY;
        int request = Math.min(amount, Math.max(1, visible.getMaxStackSize()));
        ItemStack extracted = kongqiaoLayout
                ? data.extractPersonalStorageSlot(storedIndex(slot), request, simulate)
                : extractLogical(visible, request, simulate);
        if (!simulate && !extracted.isEmpty() && combinedStorage == null) onChanged.run();
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (!isAvailable() || slot < 0 || slot >= getSlots()) return 0;
        if (isVirtualImmortalYuanSlot(slot)) return Integer.MAX_VALUE;
        if (isImportSlot(slot)) return 64;
        if (!kongqiaoLayout) return Integer.MAX_VALUE;
        long limit = 64L * Math.max(1, data.getKongqiaoStackMultiplier());
        return (int) Math.min(Integer.MAX_VALUE, limit);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return isAvailable() && slot >= 0 && slot < getSlots() && !stack.isEmpty();
    }

    private boolean isVirtualImmortalYuanSlot(int slot) {
        return infiniteImmortalYuanLayout && slot == 0;
    }

    private boolean isImportSlot(int slot) {
        return !kongqiaoLayout && slot == (infiniteImmortalYuanLayout ? 1 : 0);
    }

    private int storedIndex(int exposedSlot) {
        if (kongqiaoLayout) return exposedSlot;
        return exposedSlot - (infiniteImmortalYuanLayout ? 2 : 1);
    }

    /**
     * Per-handler index over the data-owned revision cache. Managers may create
     * short-lived capability wrappers, but the expensive physical scan remains
     * shared by {@link ImmortalStoragePlayerData} and happens once per revision.
     */
    private List<StorageItemSummary> logicalEntries() {
        long generation = combinedStorage == null
                ? data.getXianqiaoStorageGeneration() : combinedStorage.revision();
        if (cachedGeneration == generation) return cachedLogicalEntries;
        List<StorageItemSummary> source = combinedStorage == null
                ? data.getXianqiaoItemSummary() : combinedStorage.snapshot();
        LinkedHashMap<TerminalEntryKey, StorageItemSummary> current = new LinkedHashMap<>();
        for (StorageItemSummary summary : source) {
            if (infiniteImmortalYuanLayout
                    && data.isVirtualInfiniteImmortalYuanStack(summary.prototype())) continue;
            current.put(TerminalEntryKey.of(summary.prototype()), summary);
        }
        for (int slot = 0; slot < cachedLogicalEntries.size(); slot++) {
            StorageItemSummary previous = cachedLogicalEntries.get(slot);
            if (previous == null) continue;
            TerminalEntryKey key = TerminalEntryKey.of(previous.prototype());
            StorageItemSummary replacement = current.remove(key);
            cachedLogicalEntries.set(slot, replacement);
        }
        ArrayDeque<Integer> emptySlots = new ArrayDeque<>();
        for (int slot = 0; slot < cachedLogicalEntries.size(); slot++) {
            if (cachedLogicalEntries.get(slot) == null) emptySlots.addLast(slot);
        }
        for (Map.Entry<TerminalEntryKey, StorageItemSummary> entry : current.entrySet()) {
            Integer empty = emptySlots.pollFirst();
            if (empty == null) {
                cachedLogicalEntries.add(entry.getValue());
            } else {
                cachedLogicalEntries.set(empty, entry.getValue());
            }
        }
        while (!cachedLogicalEntries.isEmpty()
                && cachedLogicalEntries.get(cachedLogicalEntries.size() - 1) == null) {
            cachedLogicalEntries.remove(cachedLogicalEntries.size() - 1);
        }
        cachedGeneration = generation;
        return cachedLogicalEntries;
    }

    private ItemStack extractLogical(ItemStack visible, int request, boolean simulate) {
        if (combinedStorage == null) {
            return simulate
                    ? data.simulateExtractStack(visible.copyWithCount(1), request)
                    : data.extractStack(visible.copyWithCount(1), request);
        }
        long extracted = combinedStorage.extract(TerminalEntryKey.of(visible), request,
                simulate ? TerminalStorageAction.SIMULATE : TerminalStorageAction.EXECUTE);
        return extracted <= 0L ? ItemStack.EMPTY
                : visible.copyWithCount((int) Math.min(request, extracted));
    }
}
