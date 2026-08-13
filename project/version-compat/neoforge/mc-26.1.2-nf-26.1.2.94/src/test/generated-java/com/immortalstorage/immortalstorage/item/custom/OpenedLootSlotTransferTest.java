package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class OpenedLootSlotTransferTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void fullTargetLeavesTheOpenedPageUnchanged() {
        Source source = new Source(new ItemStack(Items.DIAMOND, 64), 64);
        var result = OpenedLootSlotTransfer.move(source, (stack, simulate) -> stack.copy());

        assertEquals(0, result.extracted());
        assertEquals(64, source.stack.getCount());
    }

    @Test
    void executionAcceptingLessThanSimulationRestoresTheExactRemainder() {
        Source source = new Source(new ItemStack(Items.DIAMOND, 64), 64);
        var result = OpenedLootSlotTransfer.move(source, (stack, simulate) ->
                simulate ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - 17));

        assertEquals(64, result.extracted());
        assertEquals(17, result.committed());
        assertEquals(47, result.restored());
        assertEquals(47, source.stack.getCount());
    }

    @Test
    void pageExtractingLessThanPlannedCommitsOnlyTheExtractedStack() {
        Source source = new Source(new ItemStack(Items.EMERALD, 64), 9);
        var result = OpenedLootSlotTransfer.move(source, (stack, simulate) -> ItemStack.EMPTY);

        assertEquals(9, result.extracted());
        assertEquals(9, result.committed());
        assertEquals(55, source.stack.getCount());
    }

    @Test
    void invalidComponentRemainderIsRejectedBeforeConservationCanDrift() {
        Source source = new Source(new ItemStack(Items.PAPER, 4), 4);
        ItemStack renamed = new ItemStack(Items.PAPER, 2);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("changed"));

        assertThrows(IllegalStateException.class,
                () -> OpenedLootSlotTransfer.move(source, (stack, simulate) -> renamed));
        assertEquals(4, source.stack.getCount());
    }

    private static final class Source implements OpenedLootSlotTransfer.Source {
        private ItemStack stack;
        private final int executionLimit;

        private Source(ItemStack stack, int executionLimit) {
            this.stack = stack;
            this.executionLimit = executionLimit;
        }

        @Override public ItemStack peek() { return stack.copy(); }

        @Override public ItemStack extract(int amount) {
            int moved = Math.min(stack.getCount(), Math.min(amount, executionLimit));
            if (moved <= 0) return ItemStack.EMPTY;
            ItemStack extracted = stack.copyWithCount(moved);
            stack.shrink(moved);
            return extracted;
        }

        @Override public void restore(ItemStack restored) {
            if (stack.isEmpty()) stack = restored.copy();
            else stack.grow(restored.getCount());
        }
    }
}
