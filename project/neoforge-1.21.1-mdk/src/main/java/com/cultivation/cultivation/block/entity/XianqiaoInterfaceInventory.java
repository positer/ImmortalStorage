package com.cultivation.cultivation.block.entity;

import com.cultivation.cultivation.api.storage.terminal.StorageItemSummary;
import com.cultivation.cultivation.api.storage.terminal.TerminalEntryKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalFluidKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalFluidStorage;
import com.cultivation.cultivation.api.storage.terminal.TerminalItemStorage;
import com.cultivation.cultivation.api.storage.terminal.TerminalStorageAction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Nine independent mixed-resource interface slots.
 *
 * <p>Each slot owns one item or fluid identity plus long-valued desired and
 * cached amounts. NeoForge stack/int limits are applied only by the item and
 * fluid capability adapters. Per-slot limits come from the server common
 * config through {@link XianqiaoInterfaceLimits}.</p>
 */
public final class XianqiaoInterfaceInventory implements BulkItemInsertTarget {
    public static final int SLOT_COUNT = 9;
    /**
     * Hard upper bound for entity-producing removal work. Any item cache that
     * cannot be represented completely inside the remaining budget stays in
     * the dropped interface block's compact long-valued payload instead.
     */
    public static final int MAX_MATERIALIZED_REMOVAL_STACKS = 64;
    private static final String ITEM_SLOTS_TAG = "ItemSlots";
    /** Legacy pre-128 format; retained read-only for world migration. */
    private static final String TARGETS_TAG = "Targets";
    private static final String BUFFERS_TAG = "Buffers";
    private static final String FLUID_SLOTS_TAG = "FluidSlots";

    private static final TerminalFluidStorage NO_FLUID_STORAGE = new TerminalFluidStorage() {
        @Override public long revision() { return 0L; }
        @Override public Map<TerminalFluidKey, Long> snapshot() { return Map.of(); }
        @Override public long insert(TerminalFluidKey key, long amountMb, TerminalStorageAction action) { return 0L; }
        @Override public long extract(TerminalFluidKey key, long amountMb, TerminalStorageAction action) { return 0L; }
    };

    private final TerminalItemStorage itemStorage;
    private final TerminalFluidStorage fluidStorage;
    private final BooleanSupplier accessAllowed;
    private final Runnable onChanged;
    private final Runnable onConfigurationChanged;
    private final Supplier<XianqiaoInterfaceLimits.Snapshot> limitSupplier;
    private final ResourceSlot[] resources = new ResourceSlot[SLOT_COUNT];
    private int nextRefillSlot;

    /** Bounded result of settling item caches during block removal. */
    public record ItemRemovalSettlement(List<ItemStack> materializedDrops, long retainedAmount) {
        public ItemRemovalSettlement {
            materializedDrops = materializedDrops == null ? List.of() : List.copyOf(materializedDrops);
            if (retainedAmount < 0L) throw new IllegalArgumentException("retained amount must be non-negative");
        }
    }

    public XianqiaoInterfaceInventory(TerminalItemStorage storage, BooleanSupplier accessAllowed) {
        this(storage, NO_FLUID_STORAGE, accessAllowed, () -> {}, () -> {});
    }

    public XianqiaoInterfaceInventory(
            TerminalItemStorage storage, BooleanSupplier accessAllowed, Runnable onChanged) {
        this(storage, NO_FLUID_STORAGE, accessAllowed, onChanged, () -> {});
    }

    public XianqiaoInterfaceInventory(
            TerminalItemStorage storage, BooleanSupplier accessAllowed,
            Runnable onChanged, Runnable onConfigurationChanged) {
        this(storage, NO_FLUID_STORAGE, accessAllowed, onChanged, onConfigurationChanged);
    }

    public XianqiaoInterfaceInventory(
            TerminalItemStorage itemStorage, TerminalFluidStorage fluidStorage,
            BooleanSupplier accessAllowed, Runnable onChanged, Runnable onConfigurationChanged) {
        this(itemStorage, fluidStorage, accessAllowed, onChanged,
                onConfigurationChanged, XianqiaoInterfaceLimits::current);
    }

    XianqiaoInterfaceInventory(
            TerminalItemStorage itemStorage, TerminalFluidStorage fluidStorage,
            BooleanSupplier accessAllowed, Runnable onChanged, Runnable onConfigurationChanged,
            Supplier<XianqiaoInterfaceLimits.Snapshot> limitSupplier) {
        if (itemStorage == null) throw new IllegalArgumentException("terminal item storage is required");
        this.itemStorage = itemStorage;
        this.fluidStorage = fluidStorage == null ? NO_FLUID_STORAGE : fluidStorage;
        this.accessAllowed = accessAllowed == null ? () -> false : accessAllowed;
        this.onChanged = onChanged == null ? () -> {} : onChanged;
        this.onConfigurationChanged = onConfigurationChanged == null ? () -> {} : onConfigurationChanged;
        this.limitSupplier = limitSupplier == null
                ? XianqiaoInterfaceLimits::current : limitSupplier;
        Arrays.setAll(resources, ignored -> new ResourceSlot());
    }

    public synchronized ItemStack getTarget(int slot) {
        checkSlot(slot);
        ResourceSlot resource = resources[slot];
        return isAvailable() && resource.itemKey != null
                ? resource.itemKey.prototype().copyWithCount((int) resource.desired) : ItemStack.EMPTY;
    }

    public synchronized ItemStack getBufferedStack(int slot) {
        checkSlot(slot);
        ResourceSlot resource = resources[slot];
        return isAvailable() && resource.itemKey != null && resource.cached > 0L
                ? resource.itemKey.prototype().copyWithCount((int) resource.cached) : ItemStack.EMPTY;
    }

    public synchronized FluidStack getFluidTarget(int slot) {
        checkSlot(slot);
        ResourceSlot resource = resources[slot];
        return isAvailable() && resource.fluidKey != null
                ? resource.fluidKey.prototype().copyWithAmount((int) resource.desired) : FluidStack.EMPTY;
    }

    public synchronized FluidStack getBufferedFluid(int slot) {
        checkSlot(slot);
        ResourceSlot resource = resources[slot];
        return isAvailable() && resource.fluidKey != null && resource.cached > 0L
                ? resource.fluidKey.prototype().copyWithAmount((int) resource.cached) : FluidStack.EMPTY;
    }

    public synchronized int getOutputFaceMask(int slot) {
        checkSlot(slot);
        return resources[slot].outputFaceMask;
    }

    public synchronized boolean isOutputFaceEnabled(int slot, Direction side) {
        checkSlot(slot);
        return side != null && (resources[slot].outputFaceMask & (1 << side.ordinal())) != 0;
    }

    /** One slot's mask is independent from the six global face modes. */
    public synchronized boolean setOutputFaceEnabled(int slot, Direction side, boolean enabled) {
        checkSlot(slot);
        if (!isAvailable() || side == null || resources[slot].empty()) return false;
        int bit = 1 << side.ordinal();
        int previous = resources[slot].outputFaceMask;
        int updated = enabled ? previous | bit : previous & ~bit;
        if (updated == previous) return true;
        resources[slot].outputFaceMask = updated;
        changedConfiguration();
        return true;
    }

    /** Configures an item identity; empty clears whichever resource occupied the slot. */
    public synchronized boolean setTarget(int slot, ItemStack requestedTarget) {
        checkSlot(slot);
        if (!isAvailable() || requestedTarget == null) return false;
        ItemStack replacement = normalizeItemTarget(requestedTarget);
        if (replacement == null) return false;
        ResourceSlot current = resources[slot];
        TerminalEntryKey replacementKey = replacement.isEmpty() ? null : TerminalEntryKey.of(replacement);
        long replacementAmount = replacement.isEmpty() ? 0L : replacement.getCount();
        boolean sameIdentity = current.itemKey != null && replacementKey != null
                && current.itemKey.equals(replacementKey);
        if (sameIdentity && current.desired == replacementAmount) return true;
        if (sameIdentity) {
            // AE-style plan change: the real cache is not thrown away when
            // only the requested amount changes. The scheduler resolves the
            // signed desired-cached delta on its next round.
            current.desired = replacementAmount;
            changedConfiguration();
            return true;
        }
        if (current.cached > 0L && !returnWholeBuffer(current)) return false;
        current.clear();
        if (replacementKey != null) current.setItem(replacementKey, replacementAmount, 0L);
        changedConfiguration();
        return true;
    }

    public synchronized boolean setTargetAmount(int slot, long requestedAmount) {
        checkSlot(slot);
        if (!isAvailable()) return false;
        ResourceSlot current = resources[slot];
        if (current.itemKey == null) return requestedAmount <= 0L && clearSlot(slot);
        long clamped = Math.min(itemTargetLimit(), Math.max(0L, requestedAmount));
        return setTarget(slot, clamped == 0L
                ? ItemStack.EMPTY : current.itemKey.prototype().copyWithCount((int) clamped));
    }

    public synchronized boolean setFluidTarget(int slot, FluidStack requestedTarget) {
        checkSlot(slot);
        if (!isAvailable() || requestedTarget == null) return false;
        FluidStack replacement = normalizeFluidTarget(requestedTarget);
        if (replacement == null) return false;
        ResourceSlot current = resources[slot];
        TerminalFluidKey replacementKey = replacement.isEmpty() ? null : TerminalFluidKey.of(replacement);
        long replacementAmount = replacement.isEmpty() ? 0L : replacement.getAmount();
        boolean sameIdentity = current.fluidKey != null && replacementKey != null
                && current.fluidKey.equals(replacementKey);
        if (sameIdentity && current.desired == replacementAmount) return true;
        if (sameIdentity) {
            current.desired = replacementAmount;
            changedConfiguration();
            return true;
        }
        if (current.cached > 0L && !returnWholeBuffer(current)) return false;
        current.clear();
        if (replacementKey != null) current.setFluid(replacementKey, replacementAmount, 0L);
        changedConfiguration();
        return true;
    }

    public synchronized boolean setFluidTargetAmount(int slot, long requestedAmount) {
        checkSlot(slot);
        if (!isAvailable()) return false;
        ResourceSlot current = resources[slot];
        if (current.fluidKey == null) return requestedAmount <= 0L && clearSlot(slot);
        long clamped = Math.min(fluidTargetLimitMb(), Math.max(0L, requestedAmount));
        return setFluidTarget(slot, clamped == 0L
                ? FluidStack.EMPTY : current.fluidKey.prototype().copyWithAmount((int) clamped));
    }

    public synchronized boolean clearSlot(int slot) {
        checkSlot(slot);
        if (!isAvailable()) return false;
        ResourceSlot current = resources[slot];
        if (current.empty()) return true;
        if (current.cached > 0L && !returnWholeBuffer(current)) return false;
        current.clear();
        changedConfiguration();
        return true;
    }

    /** Resolves one signed AE-style cache plan. */
    public synchronized int replenishSlot(int slot, TerminalStorageAction action) {
        checkSlot(slot);
        if (!isAvailable() || action == null) return 0;
        reconcileConfiguredLimits();
        ResourceSlot resource = resources[slot];
        long delta = resource.desired - resource.cached;
        if (delta == 0L) return 0;
        long requested = Math.abs(delta);
        long moved;
        if (resource.itemKey != null) {
            synchronized (itemStorage) {
                if (!isAvailable()) return 0;
                moved = delta > 0L
                        ? itemStorage.extract(resource.itemKey, requested, action)
                        : itemStorage.insert(resource.itemKey, requested, action);
            }
        } else if (resource.fluidKey != null) {
            synchronized (fluidStorage) {
                if (!isAvailable()) return 0;
                moved = delta > 0L
                        ? fluidStorage.extract(resource.fluidKey, requested, action)
                        : fluidStorage.insert(resource.fluidKey, requested, action);
            }
        } else {
            return 0;
        }
        checkTransfer(moved, requested, delta > 0L ? "cache refill" : "cache excess return");
        if (action.executes() && moved > 0L) {
            resource.cached += delta > 0L ? moved : -moved;
            onChanged.run();
        }
        return (int) moved;
    }

    /** One server tick is one AE-style scheduling round over all nine slots. */
    public synchronized long replenishAllSlots(TerminalStorageAction action) {
        if (!isAvailable() || action == null) return 0L;
        long total = 0L;
        for (int slot = 0; slot < SLOT_COUNT; slot++) total += replenishSlot(slot, action);
        return total;
    }

    /** Compatibility entry point retained for deterministic unit tests. */
    public synchronized int replenishNextSlot(TerminalStorageAction action) {
        if (!isAvailable() || action == null) return 0;
        int slot = nextRefillSlot;
        int moved = replenishSlot(slot, action);
        if (action.executes()) nextRefillSlot = (nextRefillSlot + 1) % SLOT_COUNT;
        return moved;
    }

    @Override public synchronized int getSlots() { return isAvailable() ? SLOT_COUNT : 0; }

    @Override
    public synchronized @NotNull ItemStack getStackInSlot(int slot) {
        return validSlot(slot) ? getBufferedStack(slot) : ItemStack.EMPTY;
    }

    @Override
    public synchronized @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!validSlot(slot) || stack.isEmpty() || !isAvailable()) return stack;
        long accepted = insertBulk(stack, stack.getCount(), simulate);
        checkTransfer(accepted, stack.getCount(), "item upload");
        if (accepted == stack.getCount()) return ItemStack.EMPTY;
        ItemStack remainder = stack.copy();
        remainder.shrink((int) accepted);
        return remainder;
    }

    @Override
    public synchronized long insertBulk(ItemStack prototype, long amount, boolean simulate) {
        if (prototype == null || prototype.isEmpty() || amount <= 0L || !isAvailable()) return 0L;
        TerminalStorageAction action = simulate ? TerminalStorageAction.SIMULATE : TerminalStorageAction.EXECUTE;
        long accepted;
        synchronized (itemStorage) {
            if (!isAvailable()) return 0L;
            accepted = itemStorage.insert(TerminalEntryKey.of(prototype), amount, action);
        }
        checkTransfer(accepted, amount, "bulk item upload");
        return accepted;
    }

    @Override
    public synchronized @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!validSlot(slot) || amount <= 0 || !isAvailable()) return ItemStack.EMPTY;
        ResourceSlot resource = resources[slot];
        if (resource.itemKey == null || resource.cached <= 0L) return ItemStack.EMPTY;
        int extracted = (int) Math.min(amount, resource.cached);
        ItemStack result = resource.itemKey.prototype().copyWithCount(extracted);
        if (!simulate) {
            resource.cached -= extracted;
            onChanged.run();
        }
        return result;
    }

    /**
     * Transaction compensation used only after a menu extracted first but a
     * player slot accepted less than simulated. It can restore only the exact
     * configured identity and never exceed that slot's desired real cache.
     */
    public synchronized @NotNull ItemStack restoreExtractedItem(int slot, @NotNull ItemStack stack) {
        if (!validSlot(slot) || stack.isEmpty() || !isAvailable()) return stack;
        ResourceSlot resource = resources[slot];
        if (resource.itemKey == null || !resource.itemKey.matches(stack)) return stack;
        long room = Math.max(0L, resource.desired - resource.cached);
        int restored = (int) Math.min(room, stack.getCount());
        if (restored <= 0) return stack;
        resource.cached += restored;
        onChanged.run();
        if (restored == stack.getCount()) return ItemStack.EMPTY;
        ItemStack remainder = stack.copy();
        remainder.shrink(restored);
        return remainder;
    }

    public synchronized FluidStack restoreExtractedFluid(int slot, FluidStack stack) {
        if (!validSlot(slot) || stack == null || stack.isEmpty() || !isAvailable()) return stack;
        ResourceSlot resource = resources[slot];
        if (resource.fluidKey == null || !resource.fluidKey.equals(TerminalFluidKey.of(stack))) return stack;
        long room = Math.max(0L, resource.desired - resource.cached);
        int restored = (int) Math.min(room, stack.getAmount());
        if (restored <= 0) return stack;
        resource.cached += restored;
        onChanged.run();
        return restored == stack.getAmount() ? FluidStack.EMPTY
                : stack.copyWithAmount(stack.getAmount() - restored);
    }

    public synchronized int findItemSlotForActiveTransfer(ItemStack stack, Direction side) {
        if (stack == null || stack.isEmpty() || side == null) return -1;
        for (int slot = 0; slot < resources.length; slot++) {
            ResourceSlot resource = resources[slot];
            if (isOutputFaceEnabled(slot, side) && resource.itemKey != null
                    && resource.itemKey.matches(stack)) return slot;
        }
        return -1;
    }

    public synchronized int findFluidSlotForActiveTransfer(FluidStack stack, Direction side) {
        if (stack == null || stack.isEmpty() || side == null) return -1;
        TerminalFluidKey key = TerminalFluidKey.of(stack);
        for (int slot = 0; slot < resources.length; slot++) {
            ResourceSlot resource = resources[slot];
            if (isOutputFaceEnabled(slot, side) && key.equals(resource.fluidKey)) return slot;
        }
        return -1;
    }

    public synchronized long insertItemIntoCache(int slot, ItemStack stack, boolean simulate) {
        if (!validSlot(slot) || stack == null || stack.isEmpty() || !isAvailable()) return 0L;
        ResourceSlot resource = resources[slot];
        if (resource.itemKey == null || !resource.itemKey.matches(stack)) return 0L;
        long accepted = Math.min(stack.getCount(), Math.max(0L, resource.desired - resource.cached));
        if (!simulate && accepted > 0L) {
            resource.cached += accepted;
            onChanged.run();
        }
        return accepted;
    }

    public synchronized long insertFluidIntoCache(int slot, FluidStack stack, boolean simulate) {
        if (!validSlot(slot) || stack == null || stack.isEmpty() || !isAvailable()) return 0L;
        ResourceSlot resource = resources[slot];
        if (resource.fluidKey == null || !resource.fluidKey.equals(TerminalFluidKey.of(stack))) return 0L;
        long accepted = Math.min(stack.getAmount(), Math.max(0L, resource.desired - resource.cached));
        if (!simulate && accepted > 0L) {
            resource.cached += accepted;
            onChanged.run();
        }
        return accepted;
    }

    public synchronized long fluidCacheRoom(int slot) {
        if (!validSlot(slot)) return 0L;
        ResourceSlot resource = resources[slot];
        return resource.fluidKey == null ? 0L : Math.max(0L, resource.desired - resource.cached);
    }

    @Override
    public synchronized int getSlotLimit(int slot) {
        if (!validSlot(slot) || !isAvailable()) return 0;
        return itemTargetLimit();
    }

    @Override
    public synchronized boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return validSlot(slot) && isAvailable() && !stack.isEmpty();
    }

    public synchronized int getItemTargetLimit() {
        return itemTargetLimit();
    }

    public synchronized int getFluidTargetLimitMb() {
        return fluidTargetLimitMb();
    }

    public synchronized long insertFluidBulk(FluidStack prototype, long amountMb, boolean simulate) {
        if (prototype == null || prototype.isEmpty() || amountMb <= 0L || !isAvailable()) return 0L;
        TerminalStorageAction action = simulate ? TerminalStorageAction.SIMULATE : TerminalStorageAction.EXECUTE;
        long accepted;
        synchronized (fluidStorage) {
            if (!isAvailable()) return 0L;
            accepted = fluidStorage.insert(TerminalFluidKey.of(prototype), amountMb, action);
        }
        checkTransfer(accepted, amountMb, "bulk fluid upload");
        return accepted;
    }

    public synchronized FluidStack drainFluidFromSlot(int slot, int amountMb, boolean simulate) {
        if (!validSlot(slot) || amountMb <= 0 || !isAvailable()) return FluidStack.EMPTY;
        ResourceSlot resource = resources[slot];
        if (resource.fluidKey == null || resource.cached <= 0L) return FluidStack.EMPTY;
        int extracted = (int) Math.min(amountMb, resource.cached);
        FluidStack result = resource.fluidKey.prototype().copyWithAmount(extracted);
        if (!simulate) {
            resource.cached -= extracted;
            onChanged.run();
        }
        return result;
    }

    /** Drains matching duplicate slots in order while returning one legal int FluidStack. */
    public synchronized FluidStack drainFluid(FluidStack requested, int amountMb, boolean simulate) {
        if (requested == null || requested.isEmpty() || amountMb <= 0 || !isAvailable()) return FluidStack.EMPTY;
        TerminalFluidKey key = TerminalFluidKey.of(requested);
        int remaining = amountMb;
        int moved = 0;
        for (ResourceSlot resource : resources) {
            if (remaining == 0) break;
            if (resource.fluidKey == null || !resource.fluidKey.equals(key) || resource.cached <= 0L) continue;
            int part = (int) Math.min(remaining, resource.cached);
            moved += part;
            remaining -= part;
            if (!simulate) resource.cached -= part;
        }
        if (!simulate && moved > 0) onChanged.run();
        return moved == 0 ? FluidStack.EMPTY : key.prototype().copyWithAmount(moved);
    }

    public synchronized void saveState(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag == null || registries == null) return;
        saveItemSlots(tag, registries);
        tag.remove(TARGETS_TAG);
        tag.remove(BUFFERS_TAG);
        ListTag fluids = new ListTag();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ResourceSlot resource = resources[slot];
            if (resource.fluidKey == null) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", slot);
            Tag encoded = resource.fluidKey.prototype().saveOptional(registries);
            entry.put("Fluid", encoded instanceof CompoundTag compound ? compound.copy() : new CompoundTag());
            entry.putLong("Desired", resource.desired);
            entry.putLong("Cached", resource.cached);
            entry.putInt("OutputFaces", resource.outputFaceMask);
            fluids.add(entry);
        }
        if (fluids.isEmpty()) tag.remove(FLUID_SLOTS_TAG);
        else tag.put(FLUID_SLOTS_TAG, fluids);
    }

    public synchronized void loadState(CompoundTag tag, HolderLookup.Provider registries) {
        Arrays.setAll(resources, ignored -> new ResourceSlot());
        nextRefillSlot = 0;
        if (tag == null || registries == null) return;
        if (tag.contains(ITEM_SLOTS_TAG, Tag.TAG_LIST)) loadItemSlots(tag, registries);
        else loadLegacyItemStacks(tag, registries);
        if (tag.contains(FLUID_SLOTS_TAG, Tag.TAG_LIST)) {
            ListTag fluids = tag.getList(FLUID_SLOTS_TAG, Tag.TAG_COMPOUND);
            for (int index = 0; index < fluids.size(); index++) {
                CompoundTag entry = fluids.getCompound(index);
                int slot = entry.getInt("Slot");
                if (!validSlot(slot) || !entry.contains("Fluid")) continue;
                FluidStack prototype = FluidStack.parseOptional(registries, entry.getCompound("Fluid"));
                if (prototype.isEmpty()) continue;
                long desired = Math.min(fluidTargetLimitMb(), Math.max(0L, entry.getLong("Desired")));
                if (desired <= 0L) continue;
                long cached = Math.min(Integer.MAX_VALUE, Math.max(0L, entry.getLong("Cached")));
                resources[slot].setFluid(TerminalFluidKey.of(prototype), desired, cached);
                resources[slot].outputFaceMask = sanitizeOutputFaceMask(entry.getInt("OutputFaces"));
            }
        }
    }

    public synchronized ItemRemovalSettlement settleItemBuffersForRemoval() {
        List<ItemStack> remainders = new ArrayList<>(MAX_MATERIALIZED_REMOVAL_STACKS);
        for (ResourceSlot resource : resources) {
            if (resource.itemKey == null || resource.cached <= 0L) continue;
            long committed = bestEffortReturnItem(resource.itemKey, resource.cached);
            if (committed > 0L) {
                resource.cached -= committed;
                // Persist each committed decrement before touching another
                // slot. A later storage exception must not resurrect it.
                onChanged.run();
            }
        }
        // Only clear and materialize remainders after every storage call has
        // completed. If a later slot throws, all uncommitted amounts remain
        // cached and a removal retry is exact and idempotent. A slot is
        // materialized only when its complete remainder fits in the global
        // entity budget; partial materialization would make carrier replay and
        // duplicate prevention harder to audit.
        int remainingStackBudget = MAX_MATERIALIZED_REMOVAL_STACKS;
        long retainedAmount = 0L;
        for (ResourceSlot resource : resources) {
            if (resource.itemKey == null || resource.cached <= 0L) continue;
            ItemStack prototype = resource.itemKey.prototype().copyWithCount(1);
            int legalStackSize = Math.max(1, Math.min(prototype.getMaxStackSize(), 99));
            long remaining = resource.cached;
            long requiredStacks = ((remaining - 1L) / legalStackSize) + 1L;
            if (requiredStacks > remainingStackBudget) {
                retainedAmount += remaining;
                continue;
            }
            while (remaining > 0L) {
                int emitted = (int) Math.min(remaining, legalStackSize);
                remainders.add(prototype.copyWithCount(emitted));
                remaining -= emitted;
            }
            remainingStackBudget -= (int) requiredStacks;
            resource.cached = 0L;
            onChanged.run();
        }
        return new ItemRemovalSettlement(remainders, retainedAmount);
    }

    /** Compatibility facade for callers that only consume materialized drops. */
    public synchronized List<ItemStack> returnBuffersAndCollectRemainders() {
        return settleItemBuffersForRemoval().materializedDrops();
    }

    /** Returns fluid caches where possible and keeps exact remainders for the dropped block item. */
    public synchronized void returnFluidBuffersAndRetainRemainders() {
        for (ResourceSlot resource : resources) {
            if (resource.fluidKey == null || resource.cached <= 0L) continue;
            long committed = bestEffortReturnFluid(resource.fluidKey, resource.cached);
            if (committed > 0L) {
                resource.cached -= committed;
                onChanged.run();
            }
        }
    }

    private boolean returnWholeBuffer(ResourceSlot resource) {
        if (resource.cached <= 0L) return true;
        if (resource.itemKey != null) return atomicReturnItem(resource);
        if (resource.fluidKey != null) return atomicReturnFluid(resource);
        return true;
    }

    private boolean atomicReturnItem(ResourceSlot resource) {
        long amount = resource.cached;
        synchronized (itemStorage) {
            if (!isAvailable()) return false;
            long simulated = itemStorage.insert(resource.itemKey, amount, TerminalStorageAction.SIMULATE);
            if (simulated != amount || !isAvailable()) return false;
            long committed = itemStorage.insert(resource.itemKey, amount, TerminalStorageAction.EXECUTE);
            checkTransfer(committed, amount, "item cache return");
            if (committed == amount) {
                resource.cached = 0L;
                onChanged.run();
                return true;
            }
            if (committed > 0L) {
                long rolledBack = itemStorage.extract(resource.itemKey, committed, TerminalStorageAction.EXECUTE);
                if (rolledBack != committed) throw new IllegalStateException("item cache return compensation failed");
            }
            return false;
        }
    }

    private boolean atomicReturnFluid(ResourceSlot resource) {
        long amount = resource.cached;
        synchronized (fluidStorage) {
            if (!isAvailable()) return false;
            long simulated = fluidStorage.insert(resource.fluidKey, amount, TerminalStorageAction.SIMULATE);
            if (simulated != amount || !isAvailable()) return false;
            long committed = fluidStorage.insert(resource.fluidKey, amount, TerminalStorageAction.EXECUTE);
            checkTransfer(committed, amount, "fluid cache return");
            if (committed == amount) {
                resource.cached = 0L;
                onChanged.run();
                return true;
            }
            if (committed > 0L) {
                long rolledBack = fluidStorage.extract(resource.fluidKey, committed, TerminalStorageAction.EXECUTE);
                if (rolledBack != committed) throw new IllegalStateException("fluid cache return compensation failed");
            }
            return false;
        }
    }

    private long bestEffortReturnItem(TerminalEntryKey key, long amount) {
        synchronized (itemStorage) {
            long simulated = itemStorage.insert(key, amount, TerminalStorageAction.SIMULATE);
            checkTransfer(simulated, amount, "simulated removal item return");
            if (simulated <= 0L) return 0L;
            long committed = itemStorage.insert(key, simulated, TerminalStorageAction.EXECUTE);
            checkTransfer(committed, simulated, "committed removal item return");
            return committed;
        }
    }

    private long bestEffortReturnFluid(TerminalFluidKey key, long amount) {
        synchronized (fluidStorage) {
            long simulated = fluidStorage.insert(key, amount, TerminalStorageAction.SIMULATE);
            checkTransfer(simulated, amount, "simulated removal fluid return");
            if (simulated <= 0L) return 0L;
            long committed = fluidStorage.insert(key, simulated, TerminalStorageAction.EXECUTE);
            checkTransfer(committed, simulated, "committed removal fluid return");
            return committed;
        }
    }

    /** Stores identity at count one; desired/cached amounts are independent longs. */
    private void saveItemSlots(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ResourceSlot resource = resources[slot];
            if (resource.itemKey == null) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", slot);
            entry.put("Item", resource.itemKey.prototype().copyWithCount(1).saveOptional(registries));
            entry.putLong("Desired", resource.desired);
            entry.putLong("Cached", resource.cached);
            entry.putInt("OutputFaces", resource.outputFaceMask);
            entries.add(entry);
        }
        if (entries.isEmpty()) tag.remove(ITEM_SLOTS_TAG);
        else tag.put(ITEM_SLOTS_TAG, entries);
    }

    private void loadItemSlots(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = tag.getList(ITEM_SLOTS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            int slot = entry.getInt("Slot");
            if (!validSlot(slot) || !entry.contains("Item")) continue;
            ItemStack prototype = ItemStack.parseOptional(registries, entry.getCompound("Item"));
            if (prototype.isEmpty()) continue;
            long desired = Math.min(itemTargetLimit(), Math.max(0L, entry.getLong("Desired")));
            if (desired <= 0L) continue;
            long cached = Math.min(Integer.MAX_VALUE, Math.max(0L, entry.getLong("Cached")));
            resources[slot].setItem(TerminalEntryKey.of(prototype.copyWithCount(1)), desired, cached);
            resources[slot].outputFaceMask = sanitizeOutputFaceMask(entry.getInt("OutputFaces"));
        }
    }

    private void loadLegacyItemStacks(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack[] targets = loadItemStacks(tag, TARGETS_TAG, registries);
        ItemStack[] buffers = loadItemStacks(tag, BUFFERS_TAG, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack target = targets[slot];
            if (target.isEmpty()) continue;
            int desired = Math.min(itemTargetLimit(), Math.max(0, target.getCount()));
            if (desired <= 0) continue;
            long cached = !buffers[slot].isEmpty()
                    && ItemStack.isSameItemSameComponents(target, buffers[slot])
                    ? Math.max(0, buffers[slot].getCount()) : 0L;
            resources[slot].setItem(TerminalEntryKey.of(target.copyWithCount(1)), desired, cached);
        }
    }

    private static ItemStack[] loadItemStacks(
            CompoundTag tag, String key, HolderLookup.Provider registries) {
        ItemStack[] result = new ItemStack[SLOT_COUNT];
        Arrays.fill(result, ItemStack.EMPTY);
        if (!tag.contains(key, Tag.TAG_LIST)) return result;
        ListTag entries = tag.getList(key, Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            int slot = entry.getInt("Slot");
            if (!validSlot(slot) || !entry.contains("Stack")) continue;
            result[slot] = ItemStack.parseOptional(registries, entry.getCompound("Stack"));
        }
        return result;
    }

    private void changedConfiguration() {
        onChanged.run();
        onConfigurationChanged.run();
    }

    private boolean isAvailable() {
        return accessAllowed.getAsBoolean();
    }

    private ItemStack normalizeItemTarget(ItemStack requested) {
        if (requested.isEmpty()) return ItemStack.EMPTY;
        int amount = requested.getCount();
        if (amount <= 0 || amount > itemTargetLimit()) return null;
        return requested.copyWithCount(amount);
    }

    private FluidStack normalizeFluidTarget(FluidStack requested) {
        if (requested.isEmpty()) return FluidStack.EMPTY;
        int amount = requested.getAmount();
        if (amount <= 0 || amount > fluidTargetLimitMb()) return null;
        return requested.copyWithAmount(amount);
    }

    private int itemTargetLimit() {
        return limits().itemTargetLimit();
    }

    private int fluidTargetLimitMb() {
        return limits().fluidTargetLimitMb();
    }

    private XianqiaoInterfaceLimits.Snapshot limits() {
        XianqiaoInterfaceLimits.Snapshot limits = limitSupplier.get();
        return limits == null ? XianqiaoInterfaceLimits.defaults() : limits;
    }

    /**
     * Hot config reloads clamp desired targets while retaining any real excess
     * cache. The signed scheduler plan returns that excess on this same round;
     * partial or zero acceptance leaves the exact remainder for later rounds.
     */
    private void reconcileConfiguredLimits() {
        XianqiaoInterfaceLimits.Snapshot limits = limits();
        boolean changed = false;
        for (ResourceSlot resource : resources) {
            int maximum = resource.itemKey != null
                    ? limits.itemTargetLimit()
                    : resource.fluidKey != null ? limits.fluidTargetLimitMb() : 0;
            if (maximum > 0 && resource.desired > maximum) {
                resource.desired = maximum;
                changed = true;
            }
        }
        if (changed) changedConfiguration();
    }

    private static void checkTransfer(long amount, long requested, String operation) {
        if (amount < 0L || amount > requested) {
            throw new IllegalStateException("Terminal storage returned invalid " + operation
                    + " amount " + amount + " for request " + requested);
        }
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static int sanitizeOutputFaceMask(int bits) {
        return bits & ((1 << Direction.values().length) - 1);
    }

    private static void checkSlot(int slot) {
        if (!validSlot(slot)) throw new IndexOutOfBoundsException(
                "Xianqiao Interface slot " + slot + " is outside 0.." + (SLOT_COUNT - 1));
    }

    private static final class ResourceSlot {
        private TerminalEntryKey itemKey;
        private TerminalFluidKey fluidKey;
        private long desired;
        private long cached;
        private int outputFaceMask;

        private boolean empty() { return itemKey == null && fluidKey == null; }

        private void setItem(TerminalEntryKey key, long desired, long cached) {
            this.itemKey = key;
            this.fluidKey = null;
            this.desired = desired;
            this.cached = cached;
        }

        private void setFluid(TerminalFluidKey key, long desired, long cached) {
            this.itemKey = null;
            this.fluidKey = key;
            this.desired = desired;
            this.cached = cached;
        }

        private void clear() {
            itemKey = null;
            fluidKey = null;
            desired = 0L;
            cached = 0L;
            outputFaceMask = 0;
        }
    }
}
