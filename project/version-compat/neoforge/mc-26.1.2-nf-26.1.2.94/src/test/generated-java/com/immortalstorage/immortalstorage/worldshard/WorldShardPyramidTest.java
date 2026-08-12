package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldShardPyramidTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final BlockPos ORIGIN = new BlockPos(0, 10, 0);

    @BeforeAll
    static void bootstrap() {
        Bootstrap.bootStrap();
    }

    @Test
    void homogeneousVanillaBeaconGeometryActivatesHighestCompleteLevel() {
        Map<BlockPos, BlockState> states = new HashMap<>();
        fillLayer(states, 1, Blocks.DIAMOND_BLOCK.defaultBlockState());
        fillLayer(states, 2, Blocks.DIAMOND_BLOCK.defaultBlockState());

        WorldShardPyramid.Result result = WorldShardPyramid.scan(
                ORIGIN, pos -> states.getOrDefault(pos, Blocks.STONE.defaultBlockState()),
                WorldShardMinerModes.builtinModes());

        assertEquals(2, result.level());
        assertEquals(Identifier.fromNamespaceAndPath("immortalstorage", "overworld"),
                result.mode().orElseThrow().id());
    }

    @Test
    void mixedActivationMaterialsRejectThePyramidInsteadOfChoosingOneMode() {
        Map<BlockPos, BlockState> states = new HashMap<>();
        fillLayer(states, 1, Blocks.DIAMOND_BLOCK.defaultBlockState());
        states.put(ORIGIN.offset(1, -1, 1), Blocks.ANCIENT_DEBRIS.defaultBlockState());

        WorldShardPyramid.Result result = WorldShardPyramid.scan(
                ORIGIN, pos -> states.getOrDefault(pos, Blocks.STONE.defaultBlockState()),
                WorldShardMinerModes.builtinModes());

        assertEquals(0, result.level());
        assertTrue(result.mode().isEmpty());
    }

    @Test
    void invalidLaterLayerPreservesTheCompletedLowerLevel() {
        Map<BlockPos, BlockState> states = new HashMap<>();
        fillLayer(states, 1, Blocks.DIAMOND_BLOCK.defaultBlockState());
        fillLayer(states, 2, Blocks.ANCIENT_DEBRIS.defaultBlockState());

        WorldShardPyramid.Result result = WorldShardPyramid.scan(
                ORIGIN, pos -> states.getOrDefault(pos, Blocks.STONE.defaultBlockState()),
                WorldShardMinerModes.builtinModes());

        assertEquals(1, result.level());
        assertEquals(WorldShardMinerModes.OVERWORLD, result.mode().orElseThrow().id());
    }

    @Test
    void partialLaterLayerAlsoPreservesTheCompletedLowerLevel() {
        Map<BlockPos, BlockState> states = new HashMap<>();
        fillLayer(states, 1, Blocks.DIAMOND_BLOCK.defaultBlockState());
        states.put(ORIGIN.offset(0, -2, 0), Blocks.DIAMOND_BLOCK.defaultBlockState());

        WorldShardPyramid.Result result = WorldShardPyramid.scan(
                ORIGIN, pos -> states.getOrDefault(pos, Blocks.STONE.defaultBlockState()),
                WorldShardMinerModes.builtinModes());

        assertEquals(1, result.level());
        assertEquals(WorldShardMinerModes.OVERWORLD, result.mode().orElseThrow().id());
    }

    private static void fillLayer(Map<BlockPos, BlockState> states, int layer, BlockState state) {
        for (int x = -layer; x <= layer; x++) {
            for (int z = -layer; z <= layer; z++) {
                states.put(ORIGIN.offset(x, -layer, z), state);
            }
        }
    }
}
