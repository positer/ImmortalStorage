package com.immortalstorage.immortalstorage.api.storage.terminal;

import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalRecipeAvailabilityTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void longMaxLogicalSourceIsCheckedWithoutExpandingIntoItemStacks() {
        var source = new CraftingTransferTarget.TransferIngredient(
                new ItemStack(Items.DIAMOND), Long.MAX_VALUE);
        List<Ingredient> nineDiamonds = java.util.stream.IntStream.range(0, 9)
                .mapToObj(ignored -> Ingredient.of(Items.DIAMOND)).toList();

        assertTimeoutPreemptively(Duration.ofSeconds(1), () ->
                assertTrue(TerminalRecipeAvailability.hasIngredients(
                        List.of(source), List.of(), nineDiamonds)));
    }

    @Test
    void logicalAndPhysicalCountsAreConsumedExactlyOnce() {
        var oneLogical = new CraftingTransferTarget.TransferIngredient(
                new ItemStack(Items.IRON_INGOT), 1L);
        List<Ingredient> threeIron = List.of(
                Ingredient.of(Items.IRON_INGOT),
                Ingredient.of(Items.IRON_INGOT),
                Ingredient.of(Items.IRON_INGOT));

        assertTrue(TerminalRecipeAvailability.hasIngredients(
                List.of(oneLogical), List.of(new ItemStack(Items.IRON_INGOT, 2)), threeIron));
        assertFalse(TerminalRecipeAvailability.hasIngredients(
                List.of(oneLogical), List.of(new ItemStack(Items.IRON_INGOT, 1)), threeIron));
    }
}
