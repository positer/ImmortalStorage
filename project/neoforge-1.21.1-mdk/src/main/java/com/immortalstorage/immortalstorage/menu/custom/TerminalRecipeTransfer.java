package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalVirtualEntry;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/** Shared server-only recipe placement for terminal storage and player inventory. */
final class TerminalRecipeTransfer {
    static boolean place(AbstractContainerMenu menu, Player actor, ImmortalStoragePlayerData data,
                         CraftingContainer grid, RecipeHolder<CraftingRecipe> recipe,
                         int requestedSets, long expectedRevision, long currentRevision, boolean xianqiao) {
        if (!(actor instanceof ServerPlayer serverPlayer) || expectedRevision != currentRevision
                || !recipe.value().canCraftInDimensions(3, 3)) return false;

        List<Ingredient> layout = recipe.value().getIngredients();
        if (layout.stream().noneMatch(ingredient -> !ingredient.isEmpty()) || layout.size() > grid.getContainerSize()) {
            return false;
        }

        State initial = State.capture(serverPlayer, data, grid, xianqiao);
        int sets = Math.max(1, Math.min(requestedSets, maximumIngredientStackSize(layout)));
        Plan selected = buildPlan(initial, recipe.value(), layout, sets, grid.getWidth());
        if (selected == null) return false;

        data.replaceStorage(xianqiao, selected.storage());
        replace(serverPlayer.getInventory().items, selected.inventory());
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            grid.setItem(slot, selected.grid().get(slot).copy());
        }
        menu.slotsChanged(grid);
        menu.broadcastChanges();
        return true;
    }

    private static Plan buildPlan(State initial, CraftingRecipe recipe, List<Ingredient> layout,
                                  int sets, int gridWidth) {
        List<ItemStack> oldGrid = copies(initial.grid());
        List<ItemStack> storage = copies(initial.storage());
        List<ItemStack> inventory = copies(initial.inventory());
        List<ItemStack> placed = emptyStacks(initial.grid().size());
        int shapedWidth = recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped
                ? shaped.getWidth() : gridWidth;

        int shapelessTarget = 0;
        for (int sourceIndex = 0; sourceIndex < layout.size(); sourceIndex++) {
            Ingredient ingredient = layout.get(sourceIndex);
            if (ingredient.isEmpty()) continue;
            ItemStack selected = selectConcreteStack(
                    ingredient, sets, initial.virtualEntries(), oldGrid, storage, inventory);
            if (selected.isEmpty()) return null;
            ItemStack extracted = extractExact(
                    selected, sets, initial.virtualEntries(), oldGrid, storage, inventory);
            if (extracted.getCount() != sets) return null;
            int targetIndex = recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe
                    ? shapedTargetIndex(sourceIndex, shapedWidth, gridWidth)
                    : shapelessTarget++;
            if (targetIndex < 0 || targetIndex >= placed.size()) return null;
            placed.set(targetIndex, extracted);
        }

        for (ItemStack remainder : oldGrid) {
            if (remainder.isEmpty()) continue;
            if (matchesVirtualEntry(remainder, initial.virtualEntries())) continue;
            ItemStack leftover = insertIntoStacks(storage, remainder, initial.xianqiao()
                    ? Integer.MAX_VALUE : initial.kongqiaoSlots(), initial.xianqiao(),
                    initial.xianqiao() ? 1 : initial.kongqiaoStackMultiplier());
            if (!leftover.isEmpty()) {
                leftover = insertIntoStacks(inventory, leftover, inventory.size(), false, 1);
            }
            if (!leftover.isEmpty()) return null;
        }
        return new Plan(placed, storage, inventory);
    }

    static ItemStack selectConcreteStack(Ingredient ingredient, int amount,
                                         List<TerminalVirtualEntry> virtualEntries,
                                         List<ItemStack>... sources) {
        for (TerminalVirtualEntry virtualEntry : virtualEntries) {
            if (virtualEntry.amount() >= amount && ingredient.test(virtualEntry.prototype())) {
                return virtualEntry.prototype();
            }
        }
        for (List<ItemStack> source : sources) {
            for (ItemStack candidate : source) {
                if (candidate.isEmpty() || !ingredient.test(candidate)) continue;
                long available = 0L;
                for (List<ItemStack> countSource : sources) {
                    for (ItemStack stack : countSource) {
                        if (ItemStack.isSameItemSameComponents(candidate, stack)) {
                            available = Long.MAX_VALUE - available < stack.getCount()
                                    ? Long.MAX_VALUE : available + stack.getCount();
                            if (available >= amount) return candidate.copyWithCount(1);
                        }
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    static ItemStack extractExact(ItemStack template, int amount,
                                  List<TerminalVirtualEntry> virtualEntries,
                                  List<ItemStack>... sources) {
        for (TerminalVirtualEntry virtualEntry : virtualEntries) {
            if (virtualEntry.amount() >= amount
                    && ItemStack.isSameItemSameComponents(template, virtualEntry.prototype())) {
                return template.copyWithCount(amount);
            }
        }
        ItemStack result = template.copyWithCount(0);
        int remaining = amount;
        for (List<ItemStack> source : sources) {
            for (ItemStack stack : source) {
                if (remaining <= 0) break;
                if (!ItemStack.isSameItemSameComponents(template, stack)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                result.grow(take);
                remaining -= take;
            }
        }
        return result;
    }

    private static boolean matchesVirtualEntry(ItemStack stack, List<TerminalVirtualEntry> virtualEntries) {
        for (TerminalVirtualEntry virtualEntry : virtualEntries) {
            if (ItemStack.isSameItemSameComponents(stack, virtualEntry.prototype())) return true;
        }
        return false;
    }

    private static ItemStack insertIntoStacks(List<ItemStack> target, ItemStack input, int slotLimit,
                                              boolean expandable, int stackMultiplier) {
        ItemStack remaining = input.copy();
        int existingSlots = Math.min(slotLimit, target.size());
        for (int slot = 0; slot < existingSlots && !remaining.isEmpty(); slot++) {
            ItemStack current = target.get(slot);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, remaining)) {
                int limit = multipliedStackLimit(current, stackMultiplier);
                int moved = Math.min(Math.max(0, limit - current.getCount()), remaining.getCount());
                current.grow(moved);
                remaining.shrink(moved);
            }
        }
        for (int slot = 0; slot < existingSlots && !remaining.isEmpty(); slot++) {
            if (!target.get(slot).isEmpty()) continue;
            int moved = Math.min(multipliedStackLimit(remaining, stackMultiplier), remaining.getCount());
            target.set(slot, remaining.copyWithCount(moved));
            remaining.shrink(moved);
        }
        while (expandable && !remaining.isEmpty()) {
            int moved = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            target.add(remaining.copyWithCount(moved));
            remaining.shrink(moved);
        }
        return remaining;
    }

    private static int multipliedStackLimit(ItemStack stack, int multiplier) {
        long limit = (long) Math.max(1, stack.getMaxStackSize()) * Math.max(1, multiplier);
        return (int) Math.min(Integer.MAX_VALUE, limit);
    }

    private static int maximumIngredientStackSize(List<Ingredient> layout) {
        return layout.stream().filter(ingredient -> !ingredient.isEmpty())
                .mapToInt(ingredient -> java.util.Arrays.stream(ingredient.getItems())
                        .mapToInt(ItemStack::getMaxStackSize).max().orElse(1))
                .min().orElse(1);
    }

    static int shapedTargetIndex(int sourceIndex, int recipeWidth, int gridWidth) {
        if (sourceIndex < 0 || recipeWidth <= 0 || gridWidth <= 0) return -1;
        return sourceIndex / recipeWidth * gridWidth + sourceIndex % recipeWidth;
    }

    private static List<ItemStack> emptyStacks(int size) {
        List<ItemStack> result = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) result.add(ItemStack.EMPTY);
        return result;
    }

    private static List<ItemStack> copies(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static void replace(List<ItemStack> target, List<ItemStack> source) {
        for (int slot = 0; slot < target.size(); slot++) {
            target.set(slot, slot < source.size() ? source.get(slot).copy() : ItemStack.EMPTY);
        }
    }

    private record State(List<ItemStack> grid, List<ItemStack> storage, List<ItemStack> inventory,
                         List<TerminalVirtualEntry> virtualEntries,
                         boolean xianqiao, int kongqiaoSlots, int kongqiaoStackMultiplier) {
        static State capture(ServerPlayer player, ImmortalStoragePlayerData data, CraftingContainer grid,
                             boolean xianqiao) {
            List<ItemStack> gridSnapshot = new ArrayList<>(grid.getContainerSize());
            for (int slot = 0; slot < grid.getContainerSize(); slot++) gridSnapshot.add(grid.getItem(slot).copy());
            return new State(gridSnapshot, data.snapshotStorage(xianqiao), copies(player.getInventory().items),
                    xianqiao ? data.getVirtualTerminalEntries() : List.of(),
                    xianqiao, data.getKongqiaoMaxSlots(), data.getKongqiaoStackMultiplier());
        }
    }

    private record Plan(List<ItemStack> grid, List<ItemStack> storage, List<ItemStack> inventory) {}

    private TerminalRecipeTransfer() {}
}
