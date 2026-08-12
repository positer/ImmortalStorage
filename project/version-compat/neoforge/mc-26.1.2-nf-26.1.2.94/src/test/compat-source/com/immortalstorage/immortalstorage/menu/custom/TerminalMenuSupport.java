package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class TerminalMenuSupport {
    @FunctionalInterface
    interface CraftingExtractor {
        ItemStack extract(ItemStack prototype, int amount, boolean matchComponents);
    }

    static void returnCraftingItems(AbstractContainerMenu menu, Player player, Container crafting,
                                    ImmortalStoragePlayerData data, boolean xianqiao) {
        if (!(player instanceof ServerPlayer) || crafting == null) return;
        for (int i = 0; i < crafting.getContainerSize(); i++) {
            ItemStack stack = crafting.removeItemNoUpdate(i);
            if (stack.isEmpty()) continue;
            ItemStack leftover = xianqiao
                    ? insertXianqiao(data, stack)
                    : insertKongqiao(data, stack);
            if (!leftover.isEmpty()) {
                player.getInventory().placeItemBackInInventory(leftover);
            }
        }
        menu.broadcastChanges();
    }

    static ItemStack insertXianqiao(ImmortalStoragePlayerData data, ItemStack stack) {
        return data.insertStackFromPlayerInventory(stack.copy());
    }

    static ItemStack insertKongqiao(ImmortalStoragePlayerData data, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int remaining = stack.getCount();
        int slots = data.getKongqiaoMaxSlots();
        for (int i = 0; i < slots && remaining > 0; i++) {
            ItemStack current = data.getKongqiaoItems().get(i);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, stack)) {
                int moved = Math.min(Math.max(0,
                        data.getKongqiaoStackLimit(current) - current.getCount()), remaining);
                current.grow(moved);
                remaining -= moved;
            }
        }
        for (int i = 0; i < slots && remaining > 0; i++) {
            if (data.getKongqiaoItems().get(i).isEmpty()) {
                int moved = Math.min(data.getKongqiaoStackLimit(stack), remaining);
                data.setKongqiaoSlot(i, stack.copyWithCount(moved));
                remaining -= moved;
            }
        }
        return remaining == 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
    }

    static List<ItemStack> snapshotCrafting(Container crafting) {
        java.util.ArrayList<ItemStack> snapshot = new java.util.ArrayList<>(crafting.getContainerSize());
        for (int slot = 0; slot < crafting.getContainerSize(); slot++) {
            snapshot.add(crafting.getItem(slot).copy());
        }
        return List.copyOf(snapshot);
    }

    static void refillCraftingAfterTake(
            Container crafting, List<ItemStack> before, boolean matchComponents,
            CraftingExtractor extractor) {
        if (crafting == null || before == null || extractor == null) return;
        int slots = Math.min(crafting.getContainerSize(), before.size());
        for (int slot = 0; slot < slots; slot++) {
            ItemStack template = before.get(slot);
            if (template == null || template.isEmpty()) continue;
            ItemStack current = crafting.getItem(slot);
            if (!current.isEmpty() && !sameForRefill(template, current, matchComponents)) {
                continue; // Preserve buckets, tools and every other recipe remainder.
            }
            int missing = template.getCount() - (current.isEmpty() ? 0 : current.getCount());
            if (missing <= 0) continue;
            ItemStack extracted = extractor.extract(
                    template, missing, matchComponents || !current.isEmpty());
            if (extracted == null || extracted.isEmpty()) continue;
            if (current.isEmpty()) {
                crafting.setItem(slot, extracted.copyWithCount(Math.min(missing, extracted.getCount())));
            } else if (ItemStack.isSameItemSameComponents(current, extracted)) {
                current.grow(Math.min(missing, extracted.getCount()));
            }
        }
    }

    private static boolean sameForRefill(ItemStack expected, ItemStack actual, boolean matchComponents) {
        return matchComponents
                ? ItemStack.isSameItemSameComponents(expected, actual)
                : expected.is(actual.getItem());
    }

    private TerminalMenuSupport() {}
}
