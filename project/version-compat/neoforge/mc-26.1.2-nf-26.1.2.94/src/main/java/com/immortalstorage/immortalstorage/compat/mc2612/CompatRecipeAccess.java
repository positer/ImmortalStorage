package com.immortalstorage.immortalstorage.compat.mc2612;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** Official 26.1 RecipeManager/RecipeHolder adapter. */
public final class CompatRecipeAccess {
    private CompatRecipeAccess() {
    }

    public static Optional<RecipeHolder<?>> byKey(RecipeManager manager, Identifier id) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
        return manager.byKey(key);
    }

    @SuppressWarnings("unchecked")
    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>>
    getAllRecipesFor(RecipeManager manager, RecipeType<T> type) {
        List<RecipeHolder<T>> result = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (type.equals(holder.value().getType())) {
                result.add((RecipeHolder<T>) (RecipeHolder<?>) holder);
            }
        }
        return result;
    }

    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>>
    getRecipesFor(RecipeManager manager, RecipeType<T> type, I input, Level level) {
        List<RecipeHolder<T>> result = new ArrayList<>();
        for (RecipeHolder<T> holder : getAllRecipesFor(manager, type)) {
            if (holder.value().matches(input, level)) result.add(holder);
        }
        return result;
    }
}
