package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure-logic contract for the advanced-ruin container scheduler (no world required). */
final class AdvancedRuinSchedulerContractTest {

    private static AdvancedRuinScheduler.Target target(BlockPos pos) {
        return new AdvancedRuinScheduler.Target(pos, new ItemStackHandler(9));
    }

    @Test
    void sortOrdersByManhattanDistanceWithTieBreak() {
        BlockPos origin = BlockPos.ZERO;
        List<AdvancedRuinScheduler.Target> input = new ArrayList<>();
        input.add(target(new BlockPos(2, 0, 0)));  // distance 2
        input.add(target(new BlockPos(0, 0, 1)));  // distance 1
        input.add(target(new BlockPos(1, 0, 0)));  // distance 1
        List<AdvancedRuinScheduler.Target> near = AdvancedRuinScheduler.sort(input, origin, false);
        assertEquals(new BlockPos(0, 0, 1), near.get(0).pos());
        assertEquals(new BlockPos(1, 0, 0), near.get(1).pos());
        assertEquals(new BlockPos(2, 0, 0), near.get(2).pos());

        List<AdvancedRuinScheduler.Target> far = AdvancedRuinScheduler.sort(input, origin, true);
        assertEquals(new BlockPos(2, 0, 0), far.get(0).pos());
    }

    @Test
    void enumerateStaysInsideBoxExclusiveAndExcludesOrigin() {
        BlockPos origin = new BlockPos(5, 5, 5);
        List<BlockPos> positions = AdvancedRuinScheduler.enumerate(origin, 1, 1, 1, 2, 2, 2);
        assertEquals(8, positions.size(), "2^3 positions strictly inside the box");
        for (BlockPos pos : positions) {
            assertFalse(pos.equals(origin));
            assertTrue(pos.getX() >= 6 && pos.getX() <= 7, "x inside offset..offset+size-1");
            assertTrue(pos.getY() >= 6 && pos.getY() <= 7, "y inside offset..offset+size-1");
            assertTrue(pos.getZ() >= 6 && pos.getZ() <= 7, "z inside offset..offset+size-1");
        }
    }

    @Test
    void enumerateBoxTouchingOriginStillExcludesOrigin() {
        BlockPos origin = new BlockPos(0, 0, 0);
        List<BlockPos> positions = AdvancedRuinScheduler.enumerate(origin, -1, -1, -1, 3, 3, 3);
        assertEquals(26, positions.size(), "3^3 - 1: origin excluded");
        assertFalse(positions.stream().anyMatch(origin::equals));
    }

    @Test
    void enabledFacesDecodesMaskInOrdinalOrder() {
        assertTrue(AdvancedRuinScheduler.enabledFaces(0).isEmpty(), "all off = no interaction");
        List<net.minecraft.core.Direction> faces = AdvancedRuinScheduler.enabledFaces(
                (1 << net.minecraft.core.Direction.DOWN.ordinal()) | (1 << net.minecraft.core.Direction.UP.ordinal()));
        assertEquals(2, faces.size());
        assertEquals(net.minecraft.core.Direction.DOWN, faces.get(0));
        assertEquals(net.minecraft.core.Direction.UP, faces.get(1));
        assertEquals(6, AdvancedRuinScheduler.enabledFaces(AdvancedRuinScheduler.ALL_FACES).size());
    }

    @Test
    void collectPullsContainerItemsIntoOwnInventory() {
        ItemStackHandler own = new ItemStackHandler(54);
        ItemStackHandler container = new ItemStackHandler(9);
        container.setStackInSlot(0, new ItemStack(Items.DIAMOND, 4));
        AdvancedRuinScheduler.Target t = new AdvancedRuinScheduler.Target(new BlockPos(1, 0, 0), container);
        // Item-by-item: one diamond moved into the ruin's own inventory.
        AdvancedRuinScheduler.collect(own, List.of(t), false, true, stack -> true);
        assertFalse(own.getStackInSlot(0).isEmpty());
        assertEquals(3, container.getStackInSlot(0).getCount());
    }

    @Test
    void collectForcePollBlocksOnMissingHandler() {
        ItemStackHandler own = new ItemStackHandler(54);
        ItemStackHandler container = new ItemStackHandler(9);
        container.setStackInSlot(0, new ItemStack(Items.DIAMOND, 1));
        AdvancedRuinScheduler.Target missing = new AdvancedRuinScheduler.Target(new BlockPos(1, 0, 0), null);
        AdvancedRuinScheduler.Target present = new AdvancedRuinScheduler.Target(new BlockPos(2, 0, 0), container);
        // Force-poll: the null handler comes first and blocks the whole pass.
        AdvancedRuinScheduler.collect(own, List.of(missing, present), true, true, stack -> true);
        assertTrue(own.getStackInSlot(0).isEmpty());
        assertEquals(1, container.getStackInSlot(0).getCount());
    }

    @Test
    void collectPollSkipContinuesPastMissingHandler() {
        ItemStackHandler own = new ItemStackHandler(54);
        ItemStackHandler container = new ItemStackHandler(9);
        container.setStackInSlot(0, new ItemStack(Items.DIAMOND, 1));
        AdvancedRuinScheduler.Target missing = new AdvancedRuinScheduler.Target(new BlockPos(1, 0, 0), null);
        AdvancedRuinScheduler.Target present = new AdvancedRuinScheduler.Target(new BlockPos(2, 0, 0), container);
        AdvancedRuinScheduler.collect(own, List.of(missing, present), false, true, stack -> true);
        assertFalse(own.getStackInSlot(0).isEmpty());
    }

    @Test
    void ejectDistributesOwnInventoryAcrossContainers() {
        ItemStackHandler own = new ItemStackHandler(54);
        own.setStackInSlot(0, new ItemStack(Items.EMERALD, 3));
        ItemStackHandler c1 = new ItemStackHandler(9);
        ItemStackHandler c2 = new ItemStackHandler(9);
        List<AdvancedRuinScheduler.Target> targets = List.of(
                new AdvancedRuinScheduler.Target(new BlockPos(1, 0, 0), c1),
                new AdvancedRuinScheduler.Target(new BlockPos(2, 0, 0), c2));
        int[] cursor = {0};
        // Item-by-item + poll-skip: three emeralds spread across c1/c2.
        AdvancedRuinScheduler.eject(own, targets, false, true, stack -> true, cursor);
        assertTrue(own.getStackInSlot(0).isEmpty(), "own inventory drained");
        int total = c1.getStackInSlot(0).getCount() + c2.getStackInSlot(0).getCount();
        assertEquals(3, total);
        assertTrue(c1.getStackInSlot(0).getCount() >= 1 && c2.getStackInSlot(0).getCount() >= 1);
    }

    @Test
    void ejectGroupByGroupKeepsUnacceptedInOwnInventory() {
        ItemStackHandler own = new ItemStackHandler(54);
        own.setStackInSlot(0, new ItemStack(Items.EMERALD, 2));
        ItemStackHandler full = new ItemStackHandler(1);
        full.setStackInSlot(0, new ItemStack(Items.STONE, 64)); // no room
        AdvancedRuinScheduler.Target t = new AdvancedRuinScheduler.Target(new BlockPos(1, 0, 0), full);
        int[] cursor = {0};
        // Group-by-group + poll-skip: container cannot accept, group stays in own inventory (no world drop).
        AdvancedRuinScheduler.eject(own, List.of(t), false, false, stack -> true, cursor);
        assertFalse(own.getStackInSlot(0).isEmpty(), "leftover stays in own inventory");
    }

    @Test
    void ejectHonorsTheConfiguredFilter() {
        ItemStackHandler own = new ItemStackHandler(54);
        own.setStackInSlot(0, new ItemStack(Items.EMERALD, 2));
        ItemStackHandler target = new ItemStackHandler(9);

        AdvancedRuinScheduler.eject(own,
                List.of(new AdvancedRuinScheduler.Target(new BlockPos(1, 0, 0), target)),
                false, true, stack -> stack.is(Items.DIAMOND), new int[]{0});

        assertEquals(2, own.getStackInSlot(0).getCount(), "filtered stack remains in the ruin");
        assertTrue(target.getStackInSlot(0).isEmpty(), "filtered stack is not exported");
    }

    @Test
    void ejectUsesAllTargetSlotsInsteadOfOnlySlotZero() {
        ItemStackHandler own = new ItemStackHandler(54);
        own.setStackInSlot(0, new ItemStack(Items.EMERALD, 1));
        ItemStackHandler target = new ItemStackHandler(2);
        target.setStackInSlot(0, new ItemStack(Items.STONE, 64));

        AdvancedRuinScheduler.eject(own,
                List.of(new AdvancedRuinScheduler.Target(new BlockPos(1, 0, 0), target)),
                false, false, stack -> true, new int[]{0});

        assertTrue(own.getStackInSlot(0).isEmpty(), "item reaches a later target slot");
        assertEquals(Items.EMERALD, target.getStackInSlot(1).getItem());
    }
}
