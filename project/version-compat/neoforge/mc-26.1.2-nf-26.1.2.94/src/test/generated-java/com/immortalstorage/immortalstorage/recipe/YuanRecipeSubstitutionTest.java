package com.immortalstorage.immortalstorage.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.item.custom.ImmortalYuanItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class YuanRecipeSubstitutionTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final String YUAN_TAG = "immortalstorage:yuan_crafting_materials";

    @Test
    void craftingMaterialTagContainsTrueAndImmortalYuan() {
        JsonArray values = resource("data/immortalstorage/tags/item/yuan_crafting_materials.json")
                .getAsJsonArray("values");

        assertEquals(2, values.size());
        assertEquals("immortalstorage:true_yuan", values.get(0).getAsString());
        assertEquals("immortalstorage:immortal_yuan", values.get(1).getAsString());
    }

    @Test
    void everyTrueYuanRecipeUsesTheSharedSubstitutionTag() {
        JsonObject crude = recipe("crude_pill_embryo");
        assertEquals("immortalstorage:yuan_substitution_shapeless", crude.get("type").getAsString());
        assertTagIngredient("crude_pill_embryo", crude
                .getAsJsonArray("ingredients").get(0));

        JsonObject refined = recipe("refined_pill_embryo");
        assertEquals("immortalstorage:yuan_substitution_shaped", refined.get("type").getAsString());
        assertTagIngredient("refined_pill_embryo", refined
                .getAsJsonObject("key").get("Y"));

        JsonObject breakthrough = recipe("breakthrough_pill_embryo_yuan");
        assertEquals("immortalstorage:yuan_substitution_shaped", breakthrough.get("type").getAsString());
        assertTagIngredient("breakthrough_pill_embryo_yuan", breakthrough
                .getAsJsonObject("key").get("Y"));

        JsonObject immortalPill = recipe("immortal_pill_yuan");
        assertEquals("immortalstorage:yuan_substitution_shaped", immortalPill.get("type").getAsString());
        assertTagIngredient("immortal_pill_yuan", immortalPill
                .getAsJsonObject("key").get("Y"));
    }

    @Test
    void mixedTrueAndImmortalSlotsReturnOnlySubstitutionImmortalYuan() {
        ItemStack trueYuan = new ItemStack(ModItems.TRUE_YUAN.get(), 3);
        ItemStack immortalYuan = new ItemStack(ModItems.IMMORTAL_YUAN.get(), 7);
        CraftingInput input = CraftingInput.of(3, 1,
                List.of(trueYuan, immortalYuan, new ItemStack(Items.WHEAT)));

        NonNullList<ItemStack> remaining = YuanSubstitutionRecipeSupport.remainingItems(input);

        assertTrue(remaining.get(0).isEmpty(), "true yuan is a normally consumed ingredient");
        assertEquals(1, remaining.get(1).getCount());
        assertTrue(remaining.get(1).getItem() instanceof ImmortalYuanItem);
        assertTrue(remaining.get(2).isEmpty());
    }

    @Test
    void repeatedCraftsDoNotConsumeImmortalYuanUsedAsSubstitute() {
        ItemStack immortalSlot = new ItemStack(ModItems.IMMORTAL_YUAN.get(), 8);
        for (int craft = 0; craft < 32; craft++) {
            CraftingInput input = CraftingInput.of(2, 1,
                    List.of(immortalSlot.copy(), new ItemStack(Items.WHEAT)));
            ItemStack returned = YuanSubstitutionRecipeSupport.remainingItems(input).get(0);
            immortalSlot.shrink(1);
            immortalSlot.grow(returned.getCount());
        }
        assertEquals(8, immortalSlot.getCount());
    }

    @Test
    void recipesThatNativelyRequireImmortalYuanStillConsumeIt() {
        JsonObject nativeRecipe = recipe("immortal_pill_xian");
        assertEquals("minecraft:crafting_shaped", nativeRecipe.get("type").getAsString());
        assertEquals("immortalstorage:immortal_yuan", nativeRecipe.getAsJsonObject("key")
                .get("X").getAsString());

        ItemStack immortalYuan = new ItemStack(ModItems.IMMORTAL_YUAN.get());
        assertFalse(immortalYuan.getItem().getCraftingRemainder() != null && immortalYuan.getItem().getCraftingRemainder().count() > 0,
                "immortal yuan must not have a global crafting remainder");
    }

    @Test
    void substitutionRecipeHasNoCompetingConsumingImmortalVariant() {
        assertNull(YuanRecipeSubstitutionTest.class.getClassLoader().getResource(
                "data/immortalstorage/recipe/breakthrough_pill_embryo_xian.json"));
    }

    private static void assertTagIngredient(String recipeName, com.google.gson.JsonElement ingredient) {
        assertEquals("#" + YUAN_TAG, ingredient.getAsString(), recipeName);
    }

    private static JsonObject recipe(String name) {
        return resource("data/immortalstorage/recipe/" + name + ".json");
    }

    private static JsonObject resource(String path) {
        InputStream stream = YuanRecipeSubstitutionTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException error) {
            throw new AssertionError("could not read " + path, error);
        }
    }
}
