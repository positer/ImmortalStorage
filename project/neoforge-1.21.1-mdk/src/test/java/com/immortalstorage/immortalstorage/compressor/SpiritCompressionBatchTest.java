package com.immortalstorage.immortalstorage.compressor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SpiritCompressionBatchTest {
    @Test void consumesLargestWholeMultipleAcrossAllTwelveSlots() {
        assertEquals(12, SpiritCompressionBatch.multiplier(768, 64, 1, Integer.MAX_VALUE));
        assertEquals(192, SpiritCompressionBatch.multiplier(768, 4, 1, Integer.MAX_VALUE));
        assertEquals(15, SpiritCompressionBatch.multiplier(127, 8, 1, Integer.MAX_VALUE));
    }
    @Test void blockedOutputAndPartialBatchesConsumeNothing() {
        assertEquals(0, SpiritCompressionBatch.multiplier(768, 4, 1, 0));
        assertEquals(0, SpiritCompressionBatch.multiplier(3, 4, 1, 64));
        assertEquals(0, SpiritCompressionBatch.multiplier(768, 1, 8, 7));
    }
    @Test void directStorageUsesLongCapacityAndLocalOutputCannotOverflow() {
        assertEquals(768, SpiritCompressionBatch.multiplier(768, 1, 64, Long.MAX_VALUE));
        assertEquals(1, SpiritCompressionBatch.multiplier(768, 1, 64, 100));
        assertEquals(0, SpiritCompressionBatch.multiplier(768, 1, 64, 63));
        assertEquals(0, SpiritCompressionBatch.multiplier(768, 0, 64, Long.MAX_VALUE));
    }
    @Test void structureExactlyMatchesTheBuildingsFixture() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
            counts.merge(SpiritCompressorStructure.blockId(x, y, z), 1, Integer::sum);
        }
        assertEquals(java.util.Map.of("immortalstorage:world_barrier", 8, "minecraft:chain", 12,
                "minecraft:air", 4, "immortalstorage:world_shard_miner", 1,
                "immortalstorage:treasure_basin", 1, "immortalstorage:miniature_immortal_ruin_block", 1), counts);
        assertEquals("immortalstorage:world_shard_miner", SpiritCompressorStructure.blockId(0, -1, 0));
        assertEquals("immortalstorage:treasure_basin", SpiritCompressorStructure.blockId(0, 1, 0));
    }
}
