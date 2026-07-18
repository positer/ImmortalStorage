package com.cultivation.cultivation.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpiritStaffBuildPlanTest {
    @Test
    void removalLayerStaysOnTheClickedPlaneAndHonorsTheLimit() {
        List<BlockPos> plan = SpiritStaffBuildPlan.removalLayer(
                BlockPos.ZERO, Direction.SOUTH, 6,
                pos -> pos.getZ() == 0 && Math.abs(pos.getX()) <= 2 && Math.abs(pos.getY()) <= 2);
        assertEquals(6, plan.size());
        assertTrue(plan.stream().allMatch(pos -> pos.getZ() == 0));
    }
    @Test
    void followsOnlyConnectedSourcesInTheClickedFacePlane() {
        BlockPos origin = new BlockPos(12, 64, -7);
        BlockPos north = origin.north();
        BlockPos northEast = north.east();
        Set<BlockPos> matchingSources = Set.of(
                origin,
                north,
                northEast,
                origin.above(),
                origin.south(4));

        List<BlockPos> plan = SpiritStaffBuildPlan.create(
                origin, Direction.UP, 16, matchingSources::contains, ignored -> true);

        assertEquals(List.of(origin.above(), north.above(), northEast.above()), plan);
        assertFalse(plan.contains(origin.above(2)),
                "the BFS must not traverse along the clicked face axis");
        assertFalse(plan.contains(origin.south(4).above()),
                "the BFS must not include disconnected matching blocks");
    }

    @Test
    void aRejectedTargetDoesNotPreventTraversalAcrossItsMatchingSource() {
        BlockPos origin = BlockPos.ZERO;
        BlockPos north = origin.north();
        BlockPos farNorth = north.north();
        Set<BlockPos> matchingSources = Set.of(origin, north, farNorth);

        List<BlockPos> plan = SpiritStaffBuildPlan.create(
                origin,
                Direction.EAST,
                16,
                matchingSources::contains,
                target -> !target.equals(origin.east()));

        assertEquals(List.of(north.east(), farNorth.east()), plan,
                "replaceability filters placement targets, not BFS connectivity");
    }

    @Test
    void configuredLimitCapsAStableDuplicateFreeBreadthFirstPlan() {
        BlockPos origin = new BlockPos(3, 80, 5);
        Set<BlockPos> matchingSources = Set.of(
                origin,
                origin.north(),
                origin.south(),
                origin.west(),
                origin.east(),
                origin.north().west(),
                origin.north().east());

        List<BlockPos> first = SpiritStaffBuildPlan.create(
                origin, Direction.UP, 3, matchingSources::contains, ignored -> true);
        List<BlockPos> second = SpiritStaffBuildPlan.create(
                origin, Direction.UP, 3, matchingSources::contains, ignored -> true);

        assertEquals(3, first.size());
        assertEquals(origin.above(), first.getFirst(), "the clicked block is the BFS root");
        assertEquals(first, second, "client preview and server commit need deterministic ordering");
        assertEquals(first.size(), new HashSet<>(first).size(), "one target may only appear once");
        assertTrue(first.stream().allMatch(target -> matchingSources.contains(target.below())));
    }

    @Test
    void nonPositiveLimitAndInvalidInputsProduceNoTargets() {
        BlockPos origin = BlockPos.ZERO;

        assertTrue(SpiritStaffBuildPlan.create(
                origin, Direction.UP, 0, ignored -> true, ignored -> true).isEmpty());
        assertTrue(SpiritStaffBuildPlan.create(
                null, Direction.UP, 64, ignored -> true, ignored -> true).isEmpty());
        assertTrue(SpiritStaffBuildPlan.create(
                origin, null, 64, ignored -> true, ignored -> true).isEmpty());
    }
}
