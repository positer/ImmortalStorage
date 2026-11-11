package com.immortalstorage.immortalstorage.player;

import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Moves real, component-identical stock; never inserts a clone of the picked prototype. */
public final class XianqiaoPickItem {
    private XianqiaoPickItem() {}

    public static boolean moveToHand(ImmortalStoragePlayerData data, Container inventory,
                                     int selected, ItemStack target) {
        if (data.getStage() < 6 || selected < 0 || selected >= 9
                || inventory.getContainerSize() < 36 || target == null || target.isEmpty()) return false;
        if (ItemStack.isSameItemSameComponents(inventory.getItem(selected), target)) return true;
        TerminalEntryKey key = TerminalEntryKey.of(target);
        int amount = (int) data.extractXianqiaoItem(key, target.getMaxStackSize(), TerminalStorageAction.SIMULATE);
        if (amount <= 0) return false;
        return data.batchXianqiaoMutations(() -> {
            int extracted = (int) data.extractXianqiaoItem(key, amount, TerminalStorageAction.EXECUTE);
            if (extracted <= 0) return false;
            ItemStack previous = inventory.getItem(selected);
            inventory.setItem(selected, ItemStack.EMPTY);
            // Merge before using empty slots; armor/offhand are never relocation destinations.
            for (int slot = 0; slot < 36 && !previous.isEmpty(); slot++) {
                if (slot == selected) continue;
                ItemStack existing = inventory.getItem(slot);
                if (!ItemStack.isSameItemSameComponents(existing, previous)) continue;
                int moved = Math.min(previous.getCount(), Math.max(0,
                        Math.min(existing.getMaxStackSize(), inventory.getMaxStackSize()) - existing.getCount()));
                existing.grow(moved);
                previous.shrink(moved);
            }
            for (int slot = 0; slot < 36 && !previous.isEmpty(); slot++) {
                if (slot == selected || !inventory.getItem(slot).isEmpty()) continue;
                int moved = Math.min(previous.getCount(),
                        Math.min(previous.getMaxStackSize(), inventory.getMaxStackSize()));
                inventory.setItem(slot, previous.copyWithCount(moved));
                previous.shrink(moved);
            }
            // This stock already belongs to the player: do not apply a deposit cap twice.
            if (!previous.isEmpty()) {
                ItemStack remainder = data.insertStackFromPlayerInventory(previous);
                if (!remainder.isEmpty()) throw new IllegalStateException("Xianqiao relocation rejected owned stock");
            }
            inventory.setItem(selected, target.copyWithCount(extracted));
            inventory.setChanged();
            return true;
        });
    }
}
