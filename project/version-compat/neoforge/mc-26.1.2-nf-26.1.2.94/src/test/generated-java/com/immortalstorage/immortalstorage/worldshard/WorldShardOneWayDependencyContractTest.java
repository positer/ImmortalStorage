package com.immortalstorage.immortalstorage.worldshard;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldShardOneWayDependencyContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path MAIN = locateMainSources();

    @Test
    void minerStructureBeamAndRendererHaveZeroTreasureBasinReferences() throws IOException {
        for (String relative : new String[] {
                "block/custom/WorldShardMinerBlock.java",
                "block/entity/WorldShardMinerBlockEntity.java",
                "worldshard/WorldShardPyramid.java",
                "worldshard/WorldShardBeamPath.java",
                "client/render/WorldShardMinerRenderer.java"
        }) {
            String source = source(relative);
            assertFalse(source.contains("TreasureBasin"), relative);
            assertFalse(source.contains("TREASURE_BASIN"), relative);
            assertFalse(source.contains("treasure_basin"), relative);
        }
    }

    @Test
    void basinObservesOnlyPublicImmutableMinerFactsInOneDirection() throws IOException {
        String basin = source("block/entity/TreasureBasinBlockEntity.java");

        assertTrue(basin.contains("worldPosition.below()"));
        assertFalse(basin.contains("public @Nullable WorldShardMinerBlockEntity"),
                "the basin must not expose a public cross-machine handle");
        assertTrue(basin.contains("miner.hasActiveBeam()"));
        assertTrue(basin.contains("miner.getActiveMode()"));
        assertTrue(basin.contains("miner.getOwner()"));
        assertFalse(basin.contains("miner.getActiveLevel()"));
        assertFalse(basin.contains("miner.canGenerateOutputs"));
        assertFalse(basin.contains("miner.routeGenerated"));
        assertFalse(basin.contains("miner.getCacheHandler"));
        assertFalse(basin.contains("miner.getMinerCache"));
        assertFalse(basin.contains("miner.set"));
        assertFalse(basin.contains("miner.update"));
    }

    @Test
    void minerActivationDoesNotConsultTheOverheadBeamGeometry() throws IOException {
        String miner = source("block/entity/WorldShardMinerBlockEntity.java");
        assertFalse(miner.contains("WorldShardBeamPath"));
        assertTrue(miner.contains("WorldShardPyramid.scan"));
    }

    private static String source(String relative) throws IOException {
        return Files.readString(MAIN.resolve(relative));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage main sources from "
                + Path.of("").toAbsolutePath());
    }
}
