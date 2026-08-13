package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalVirtualEntry;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalRecipeTransferTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void shapedRecipesKeepTheirRowsAndInteriorEmptyCells() {
        assertEquals(0, TerminalRecipeTransfer.shapedTargetIndex(0, 2, 3));
        assertEquals(1, TerminalRecipeTransfer.shapedTargetIndex(1, 2, 3));
        assertEquals(3, TerminalRecipeTransfer.shapedTargetIndex(2, 2, 3));
        assertEquals(4, TerminalRecipeTransfer.shapedTargetIndex(3, 2, 3));

        assertEquals(0, TerminalRecipeTransfer.shapedTargetIndex(0, 3, 3));
        assertEquals(1, TerminalRecipeTransfer.shapedTargetIndex(1, 3, 3));
        assertEquals(2, TerminalRecipeTransfer.shapedTargetIndex(2, 3, 3));
        assertEquals(3, TerminalRecipeTransfer.shapedTargetIndex(3, 3, 3));
    }

    @Test
    void invalidShapedGeometryIsRejected() {
        assertEquals(-1, TerminalRecipeTransfer.shapedTargetIndex(-1, 2, 3));
        assertEquals(-1, TerminalRecipeTransfer.shapedTargetIndex(0, 0, 3));
        assertEquals(-1, TerminalRecipeTransfer.shapedTargetIndex(0, 2, 0));
    }

    @Test
    void virtualLongMaxIngredientCanBePlacedWithoutMaterializingItsBalance() {
        List<TerminalVirtualEntry> virtual = List.of(
                new TerminalVirtualEntry(new ItemStack(Items.DIAMOND), Long.MAX_VALUE));

        ItemStack selected = TerminalRecipeTransfer.selectConcreteStack(
                Ingredient.of(Items.DIAMOND), 64, virtual, List.of());
        ItemStack extracted = TerminalRecipeTransfer.extractExact(selected, 64, virtual, List.of());

        assertEquals(Items.DIAMOND, selected.getItem());
        assertEquals(64, extracted.getCount());
        assertEquals(Long.MAX_VALUE, virtual.getFirst().amount());
    }
}
