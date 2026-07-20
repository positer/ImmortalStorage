package com.immortalstorage.immortalstorage.menu.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalAutofillContractTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void craftingRefillRequestsOnlyTheConsumedAmountAndPreservesRemainders() {
        SimpleContainer crafting = new SimpleContainer(2);
        crafting.setItem(0, new ItemStack(Items.IRON_INGOT, 3));
        crafting.setItem(1, new ItemStack(Items.BUCKET));
        List<ItemStack> before = TerminalMenuSupport.snapshotCrafting(crafting);
        crafting.setItem(0, new ItemStack(Items.IRON_INGOT, 2));
        crafting.setItem(1, new ItemStack(Items.WATER_BUCKET));
        AtomicInteger requested = new AtomicInteger();

        TerminalMenuSupport.refillCraftingAfterTake(crafting, before, true,
                (prototype, amount, match) -> {
                    requested.addAndGet(amount);
                    return prototype.copyWithCount(amount);
                });

        assertEquals(3, crafting.getItem(0).getCount());
        assertTrue(crafting.getItem(1).is(Items.WATER_BUCKET));
        assertEquals(1, requested.get());
    }

    @Test
    void componentToggleAppliesOnlyWhenTheSlotIsEmpty() {
        SimpleContainer crafting = new SimpleContainer(1);
        ItemStack named = new ItemStack(Items.PAPER);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("template"));
        crafting.setItem(0, named);
        List<ItemStack> before = TerminalMenuSupport.snapshotCrafting(crafting);
        crafting.setItem(0, ItemStack.EMPTY);

        TerminalMenuSupport.refillCraftingAfterTake(crafting, before, false,
                (prototype, amount, match) -> new ItemStack(Items.PAPER, amount));

        assertTrue(crafting.getItem(0).is(Items.PAPER));
        assertTrue(crafting.getItem(0).get(DataComponents.CUSTOM_NAME) == null);
    }
}
