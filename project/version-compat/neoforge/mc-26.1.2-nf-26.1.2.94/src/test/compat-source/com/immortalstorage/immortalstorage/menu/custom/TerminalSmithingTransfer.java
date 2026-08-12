package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.api.storage.terminal.CraftingTransferTarget.TransferIngredient;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Atomic server-side placement for JEI/EMI smithing fills. */
final class TerminalSmithingTransfer {
    static boolean place(ServerPlayer player, EmbeddedSmithingBackend backend,
                         RecipeHolder<SmithingRecipe> recipe, long expectedRevision, long currentRevision,
                         List<TransferIngredient> storage,
                         TerminalMenuSupport.CraftingExtractor storageExtractor) {
        if (expectedRevision != currentRevision) return false;
        List<Available> available = new ArrayList<>();
        for (TransferIngredient entry : storage) available.add(new Available(entry.stack(), entry.amount()));
        for (ItemStack stack : com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory.items(player)) if (!stack.isEmpty()) available.add(new Available(stack, stack.getCount()));
        for (int slot = 0; slot < backend.inputs.getContainerSize(); slot++) {
            ItemStack stack = backend.inputs.getItem(slot);
            if (!stack.isEmpty()) available.add(new Available(stack, stack.getCount()));
        }
        SmithingRecipe value = recipe.value();
        ItemStack template = reserve(available, stack -> value.templateIngredient().map(ingredient -> ingredient.test(stack)).orElse(false));
        ItemStack base = reserve(available, stack -> value.baseIngredient().test(stack));
        ItemStack addition = reserve(available, stack -> value.additionIngredient().map(ingredient -> ingredient.test(stack)).orElse(false));
        if (template.isEmpty() || base.isEmpty() || addition.isEmpty()) return false;

        backend.returnInputs();
        ItemStack[] selected = {template, base, addition};
        for (int slot = 0; slot < selected.length; slot++) {
            ItemStack acquired = storageExtractor.extract(selected[slot], 1, true);
            if (acquired.isEmpty()) acquired = takeInventory(player, selected[slot]);
            if (acquired.isEmpty()) return false; // Preflight above makes this unreachable on the server thread.
            backend.inputs.setItem(slot, acquired.copyWithCount(1));
        }
        backend.refreshResult();
        return true;
    }

    private static ItemStack reserve(List<Available> available, Predicate<ItemStack> predicate) {
        for (Available entry : available) {
            if (entry.amount <= 0 || !predicate.test(entry.stack)) continue;
            entry.amount--;
            return entry.stack.copyWithCount(1);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack takeInventory(ServerPlayer player, ItemStack prototype) {
        for (ItemStack stack : com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory.items(player)) {
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, prototype)) {
                stack.shrink(1);
                return prototype.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }

    private static final class Available {
        final ItemStack stack;
        long amount;
        Available(ItemStack stack, long amount) { this.stack = stack.copyWithCount(1); this.amount = amount; }
    }

    private TerminalSmithingTransfer() {}
}
