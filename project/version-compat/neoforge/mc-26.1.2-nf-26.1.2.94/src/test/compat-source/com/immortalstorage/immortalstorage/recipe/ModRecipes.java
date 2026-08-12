package com.immortalstorage.immortalstorage.recipe;

import java.util.function.Supplier;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Recipe registrations for the maintained NeoForge 26.1.2 profile. */
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ImmortalStorageMod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ImmortalStorageMod.MODID);

    public static final Supplier<RecipeType<AbstractCookingRecipe>> IMMORTAL_FURNACE_TYPE =
            RECIPE_TYPES.register("immortal_furnace", () -> new RecipeType<AbstractCookingRecipe>() {
                @Override
                public String toString() {
                    return "immortalstorage:immortal_furnace";
                }
            });

    public static final Supplier<RecipeSerializer<ImmortalFurnaceRecipe>> IMMORTAL_FURNACE_SERIALIZER =
            RECIPE_SERIALIZERS.register("immortal_furnace", () -> new RecipeSerializer<>(
                    AbstractCookingRecipe.cookingMapCodec(ImmortalFurnaceRecipe::new, 50),
                    AbstractCookingRecipe.cookingStreamCodec(ImmortalFurnaceRecipe::new)));

    public static final Supplier<RecipeSerializer<YuanSubstitutionShapedRecipe>> YUAN_SUBSTITUTION_SHAPED_SERIALIZER =
            RECIPE_SERIALIZERS.register("yuan_substitution_shaped",
                    YuanSubstitutionShapedRecipe.Serializer::create);
    public static final Supplier<RecipeSerializer<YuanSubstitutionShapelessRecipe>> YUAN_SUBSTITUTION_SHAPELESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("yuan_substitution_shapeless",
                    YuanSubstitutionShapelessRecipe.Serializer::create);

    private ModRecipes() {}
}
