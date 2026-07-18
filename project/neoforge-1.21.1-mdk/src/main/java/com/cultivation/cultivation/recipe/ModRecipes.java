package com.cultivation.cultivation.recipe;

import java.util.function.Supplier;

import com.cultivation.cultivation.CultivationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.SimpleCookingSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, CultivationMod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CultivationMod.MODID);

    public static final Supplier<RecipeType<AbstractCookingRecipe>> IMMORTAL_FURNACE_TYPE =
            RECIPE_TYPES.register("immortal_furnace", () -> new RecipeType<AbstractCookingRecipe>() {
                @Override
                public String toString() { return "cultivation:immortal_furnace"; }
            });

    public static final Supplier<RecipeSerializer<ImmortalFurnaceRecipe>> IMMORTAL_FURNACE_SERIALIZER =
            RECIPE_SERIALIZERS.register("immortal_furnace",
                    () -> new SimpleCookingSerializer<>(ImmortalFurnaceRecipe::new, 50));
    public static final Supplier<RecipeSerializer<YuanSubstitutionShapedRecipe>> YUAN_SUBSTITUTION_SHAPED_SERIALIZER =
            RECIPE_SERIALIZERS.register("yuan_substitution_shaped",
                    YuanSubstitutionShapedRecipe.Serializer::new);
    public static final Supplier<RecipeSerializer<YuanSubstitutionShapelessRecipe>> YUAN_SUBSTITUTION_SHAPELESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("yuan_substitution_shapeless",
                    YuanSubstitutionShapelessRecipe.Serializer::new);

    private ModRecipes() {}
}
