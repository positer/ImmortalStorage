package com.immortalstorage.immortalstorage.compat.mc2612;

import java.util.List;
import java.util.function.Consumer;

import com.immortalstorage.immortalstorage.recipe.ModRecipes;
import net.minecraft.client.multiplayer.ClientRecipeContainer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Client-side recipe cache for the 26.1 recipe sync boundary.
 *
 * <p>26.1's {@link ClientRecipeContainer} deliberately exposes only the
 * property-set/stonecutter view.  Full custom recipe collections arrive in
 * {@link RecipesReceivedEvent}, so integrations such as JEI must consume this
 * cache instead of casting {@code ClientLevel#recipeAccess()} to
 * {@code RecipeManager}.</p>
 */
public final class TargetClientRecipeCache {
    private static volatile List<RecipeHolder<AbstractCookingRecipe>> immortalFurnaceRecipes = List.of();
    private static volatile Consumer<List<RecipeHolder<AbstractCookingRecipe>>> jeiRefresh = ignored -> { };

    private TargetClientRecipeCache() {
    }

    @SubscribeEvent
    public static void recipesReceived(RecipesReceivedEvent event) {
        immortalFurnaceRecipes = List.copyOf(
                event.getRecipeMap().byType(ModRecipes.IMMORTAL_FURNACE_TYPE.get()));
        jeiRefresh.accept(immortalFurnaceRecipes);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        immortalFurnaceRecipes = List.of();
    }

    public static List<RecipeHolder<AbstractCookingRecipe>> immortalFurnaceRecipes() {
        return immortalFurnaceRecipes;
    }

    /**
     * Binds an optional recipe-viewer callback without loading JEI when JEI is
     * absent.  The current cache is pushed immediately so a viewer loaded after
     * the login packet still receives the synchronized recipes.
     */
    public static void bindJei(Consumer<List<RecipeHolder<AbstractCookingRecipe>>> callback) {
        jeiRefresh = callback == null ? ignored -> { } : callback;
        jeiRefresh.accept(immortalFurnaceRecipes);
    }

    public static void unbindJei() {
        jeiRefresh = ignored -> { };
    }
}
