package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Shared, best-effort item-cache scheduler for the three output machines. */
final class MachineOutputScheduler {
    private MachineOutputScheduler() {}

    /**
     * Merges machine-owned output into internal slots without applying the
     * external automation insertion filter. Output-only handlers deliberately
     * reject {@link IItemHandler#insertItem}; production must still be able to
     * populate those slots while preserving stack components and slot limits.
     */
    static ItemStack insertIntoInternalSlots(
            ItemStackHandler target, int firstSlot, int lastSlot, ItemStack offered) {
        if (target == null || offered == null || offered.isEmpty()) {
            return offered == null ? ItemStack.EMPTY : offered;
        }
        int startSlot = Math.max(0, firstSlot);
        int endSlot = Math.min(lastSlot, target.getSlots());
        ItemStack remaining = offered.copy();
        for (int slot = startSlot; slot < endSlot && !remaining.isEmpty(); slot++) {
            ItemStack stored = target.getStackInSlot(slot);
            if (stored.isEmpty()) {
                int limit = Math.min(target.getSlotLimit(slot), remaining.getMaxStackSize());
                int moved = Math.min(limit, remaining.getCount());
                if (moved <= 0) continue;
                target.setStackInSlot(slot, remaining.copyWithCount(moved));
                remaining.shrink(moved);
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(stored, remaining)) continue;
            int limit = Math.min(target.getSlotLimit(slot), stored.getMaxStackSize());
            int moved = Math.min(remaining.getCount(), Math.max(0, limit - stored.getCount()));
            if (moved <= 0) continue;
            ItemStack merged = stored.copy();
            merged.grow(moved);
            target.setStackInSlot(slot, merged);
            remaining.shrink(moved);
        }
        return remaining;
    }

    static boolean pushItemsToFaces(
            ServerLevel level, BlockPos pos, boolean enabled, boolean[] outputFaces,
            IItemHandler source, int firstSlot, int lastSlot) {
        return pushItemsToFaces(level, pos, enabled, outputFaces, source,
                firstSlot, lastSlot, Direction.values());
    }

    static boolean pushItemsToFaces(
            ServerLevel level, BlockPos pos, boolean enabled, boolean[] outputFaces,
            IItemHandler source, int firstSlot, int lastSlot, Direction... allowedSides) {
        if (!enabled || source == null || outputFaces == null) return false;
        int startSlot = Math.max(0, firstSlot);
        int endSlot = Math.min(lastSlot, source.getSlots());
        if (startSlot >= endSlot || !hasItems(source, startSlot, endSlot)) return false;
        boolean changed = false;
        for (Direction side : allowedSides) {
            if (side == null) continue;
            if (!outputFaces[side.ordinal()]) continue;
            // Once the cache is empty there is no useful capability lookup on
            // the remaining faces. A later logical tick will retry them.
            if (!hasItems(source, startSlot, endSlot)) break;
            IItemHandler target = com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.itemHandler(level.getCapability(
                    Capabilities.Item.BLOCK, pos.relative(side), side.getOpposite()));
            if (target == null) continue;
            for (int slot = startSlot; slot < endSlot; slot++) {
                while (true) {
                    ItemStack offered = source.extractItem(slot, Integer.MAX_VALUE, true);
                    if (offered.isEmpty()) break;
                    ItemStack simulatedRemainder = ItemHandlerHelper.insertItemStacked(
                            target, offered.copy(), true);
                    int accepted = offered.getCount() - simulatedRemainder.getCount();
                    if (accepted <= 0) break;
                    ItemStack extracted = source.extractItem(slot, accepted, false);
                    if (extracted.isEmpty()) break;
                    ItemStack executionRemainder = ItemHandlerHelper.insertItemStacked(
                            target, extracted.copy(), false);
                    int committed = extracted.getCount() - executionRemainder.getCount();
                    if (!executionRemainder.isEmpty()) {
                        ItemStack restoreRemainder = source.insertItem(
                                slot, executionRemainder.copy(), false);
                        if (!restoreRemainder.isEmpty()) {
                            net.minecraft.world.level.block.Block.popResource(
                                    level, pos, restoreRemainder);
                        }
                    }
                    if (committed <= 0) break;
                    changed = true;
                }
            }
        }
        return changed;
    }

    static ItemStack pushItemToFaces(
            ServerLevel level, BlockPos pos, boolean enabled, boolean[] outputFaces,
            ItemStack stack) {
        if (!enabled || stack == null || stack.isEmpty() || outputFaces == null) {
            return stack == null ? ItemStack.EMPTY : stack;
        }
        ItemStack remainder = stack;
        for (Direction side : Direction.values()) {
            if (!outputFaces[side.ordinal()] || remainder.isEmpty()) continue;
            IItemHandler target = com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.itemHandler(level.getCapability(
                    Capabilities.Item.BLOCK, pos.relative(side), side.getOpposite()));
            if (target != null) {
                remainder = ItemHandlerHelper.insertItemStacked(target, remainder, false);
            }
        }
        return remainder;
    }

    static ItemStack simulateItemToFaces(
            ServerLevel level, BlockPos pos, boolean enabled, boolean[] outputFaces,
            ItemStack stack) {
        if (!enabled || stack == null || stack.isEmpty() || outputFaces == null) {
            return stack == null ? ItemStack.EMPTY : stack;
        }
        ItemStack remainder = stack;
        for (Direction side : Direction.values()) {
            if (!outputFaces[side.ordinal()] || remainder.isEmpty()) continue;
            IItemHandler target = com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.itemHandler(level.getCapability(
                    Capabilities.Item.BLOCK, pos.relative(side), side.getOpposite()));
            if (target != null) {
                remainder = ItemHandlerHelper.insertItemStacked(target, remainder, true);
            }
        }
        return remainder;
    }

    static boolean flushItemsToXianqiao(
            IItemHandler source, int firstSlot, int lastSlot,
            PersonalStorageEndpoint endpoint) {
        if (source == null || endpoint == null) return false;
        int startSlot = Math.max(0, firstSlot);
        int endSlot = Math.min(lastSlot, source.getSlots());
        if (startSlot >= endSlot || !hasItems(source, startSlot, endSlot)) return false;
        boolean changed = false;
        for (int slot = startSlot; slot < endSlot; slot++) {
            while (true) {
                ItemStack offered = source.extractItem(slot, Integer.MAX_VALUE, true);
                if (offered.isEmpty()) break;
                ItemStack simulatedRemainder = endpoint.insert(offered.copy(), true);
                int accepted = offered.getCount() - simulatedRemainder.getCount();
                if (accepted <= 0) break;
                ItemStack extracted = source.extractItem(slot, accepted, false);
                if (extracted.isEmpty()) break;
                ItemStack executionRemainder = endpoint.insert(extracted.copy(), false);
                int committed = extracted.getCount() - executionRemainder.getCount();
                if (!executionRemainder.isEmpty()) {
                    ItemStack restored = source.insertItem(slot, executionRemainder.copy(), false);
                    if (!restored.isEmpty()) break;
                }
                if (committed <= 0) break;
                changed = true;
            }
        }
        return changed;
    }

    private static boolean hasItems(IItemHandler source, int startSlot, int endSlot) {
        for (int slot = startSlot; slot < endSlot; slot++) {
            if (!source.extractItem(slot, Integer.MAX_VALUE, true).isEmpty()) return true;
        }
        return false;
    }
}
