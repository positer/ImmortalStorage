package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldShardMinerGenerationTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll
    static void bootstrap() {
        Bootstrap.bootStrap();
    }

    @Test
    void finalWorldgenRebuildAdvancesGenerationWithoutReplacingDefinitions() {
        WorldShardMinerMode explicit = new WorldShardMinerMode(
                Identifier.fromNamespaceAndPath("test", "explicit"),
                WorldShardMinerActivation.forBlock(Blocks.IRON_BLOCK),
                Optional.of(Level.OVERWORLD.identifier()), Optional.empty(), 0xFFFFFFFF, 1.0D,
                Map.of(Identifier.withDefaultNamespace("diamond"), 7L));
        try {
            WorldShardMinerModes.install(List.of(explicit), RegistryAccess.EMPTY);
            long installed = WorldShardMinerModes.generation();

            WorldShardMinerModes.rebuildPools(RegistryAccess.EMPTY);

            assertTrue(WorldShardMinerModes.generation() > installed);
            assertEquals(explicit, WorldShardMinerModes.definitions().get(explicit.id()));
            assertTrue(WorldShardMinerModes.resolved(explicit.id()).orElseThrow().orePool()
                    .sampleBatch(net.minecraft.util.RandomSource.create(1L), 1).containsKey(Items.DIAMOND));
        } finally {
            WorldShardMinerModes.install(WorldShardMinerModes.builtinModes(), RegistryAccess.EMPTY);
        }
    }
}
