package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Shared deterministic geometry for the build-mode preview and server commit. */
public final class SpiritStaffBuildPlan {
    private static final int MIN_SCAN_BUDGET = 64;
    private static final int SCAN_MULTIPLIER = 16;

    public static List<BlockPos> create(
            LevelReader level, BlockPos clicked, Direction face, BlockState template, int limit) {
        if (level == null || template == null) return List.of();
        return create(clicked, face, limit,
                pos -> sameBlock(level.getBlockState(pos), template),
                pos -> !level.isOutsideBuildHeight(pos) && level.getBlockState(pos).canBeReplaced());
    }

    /** Connected same-state layer used by Construction-Wand-style destruction. */
    public static List<BlockPos> removalLayer(
            LevelReader level, BlockPos clicked, Direction face, BlockState template, int limit) {
        if (level == null || clicked == null || face == null || template == null || limit <= 0) return List.of();
        return removalLayer(clicked, face, limit, pos -> sameBlock(level.getBlockState(pos), template));
    }

    static List<BlockPos> removalLayer(
            BlockPos clicked, Direction face, int limit, Predicate<BlockPos> sourceMatches) {
        if (clicked == null || face == null || sourceMatches == null || limit <= 0) return List.of();
        int scanBudget = Math.max(MIN_SCAN_BUDGET, Math.multiplyExact(Math.min(limit, 4096), SCAN_MULTIPLIER));
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> result = new ArrayList<>(limit);
        queue.add(clicked.immutable());
        while (!queue.isEmpty() && visited.size() < scanBudget && result.size() < limit) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos) || !sourceMatches.test(pos)) continue;
            result.add(pos);
            for (Direction neighbor : Direction.values()) {
                if (neighbor.getAxis() != face.getAxis()) queue.addLast(pos.relative(neighbor).immutable());
            }
        }
        return List.copyOf(result);
    }

    /**
     * Construction-Wand-style placement job shared by preview and commit.
     * Geometry, replaceability, survival and collision checks are identical on
     * both sides; the final server commit still re-runs BlockItem.place so
     * protection events and late world changes remain authoritative.
     */
    public static List<BlockPos> create(
            Level level, Player player, InteractionHand hand, BlockPos clicked, Direction face,
            BlockState supportingTemplate, ItemStack placementTemplate, int limit) {
        if (level == null || player == null || hand == null || placementTemplate == null
                || !(placementTemplate.getItem() instanceof BlockItem blockItem)) {
            return List.of();
        }
        return create(clicked, face, limit,
                pos -> level.hasChunkAt(pos)
                        && sameBlock(level.getBlockState(pos), supportingTemplate),
                target -> canPlace(level, player, hand, target, face, placementTemplate, blockItem));
    }

    /**
     * Construction surfaces are identified by block identity, not by every
     * mutable state property. A chest joining another chest, a stair changing
     * shape, or a fence reconnecting therefore remains part of the same layer.
     */
    static boolean sameBlock(BlockState current, BlockState template) {
        return current != null && template != null && current.getBlock() == template.getBlock();
    }

    public static BlockPlaceContext placementContext(
            Level level, Player player, InteractionHand hand, BlockPos target,
            Direction face, ItemStack placementTemplate) {
        BlockPos support = target.relative(face.getOpposite());
        Vec3 hitLocation = Vec3.atCenterOf(support).add(
                face.getStepX() * 0.5D, face.getStepY() * 0.5D, face.getStepZ() * 0.5D);
        return new BlockPlaceContext(level, player, hand, placementTemplate.copyWithCount(1),
                new BlockHitResult(hitLocation, face, support, false));
    }

    private static boolean canPlace(
            Level level, Player player, InteractionHand hand, BlockPos target, Direction face,
            ItemStack placementTemplate, BlockItem blockItem) {
        if (level.isOutsideBuildHeight(target) || !level.hasChunkAt(target)
                || !level.getWorldBorder().isWithinBounds(target)
                || !level.mayInteract(player, target)
                || !com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayer.canInteractWithBlock(player, target, 1.0D)) {
            return false;
        }
        BlockPlaceContext context = placementContext(
                level, player, hand, target, face, placementTemplate);
        if (!context.canPlace()) return false;
        BlockState placedState = blockItem.getBlock().getStateForPlacement(context);
        return placedState != null
                && placedState.canSurvive(level, target)
                && level.isUnobstructed(placedState, target, CollisionContext.of(player));
    }

    static List<BlockPos> create(
            BlockPos clicked, Direction face, int limit,
            Predicate<BlockPos> sourceMatches, Predicate<BlockPos> targetAccepts) {
        if (clicked == null || face == null || sourceMatches == null || targetAccepts == null || limit <= 0) {
            return List.of();
        }

        int scanBudget = Math.max(MIN_SCAN_BUDGET, Math.multiplyExact(Math.min(limit, 4096), SCAN_MULTIPLIER));
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> targets = new HashSet<>();
        List<BlockPos> result = new ArrayList<>(limit);
        queue.add(clicked.immutable());

        while (!queue.isEmpty() && visited.size() < scanBudget && result.size() < limit) {
            BlockPos source = queue.removeFirst();
            if (!visited.add(source) || !sourceMatches.test(source)) continue;

            BlockPos target = source.relative(face).immutable();
            if (targets.add(target) && targetAccepts.test(target)) result.add(target);

            for (Direction neighbor : Direction.values()) {
                if (neighbor.getAxis() != face.getAxis()) queue.addLast(source.relative(neighbor).immutable());
            }
        }
        return List.copyOf(result);
    }

    private SpiritStaffBuildPlan() {}
}
