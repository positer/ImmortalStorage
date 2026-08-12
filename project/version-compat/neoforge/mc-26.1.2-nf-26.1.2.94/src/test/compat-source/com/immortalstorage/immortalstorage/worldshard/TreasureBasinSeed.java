package com.immortalstorage.immortalstorage.worldshard;

import com.immortalstorage.core.worldshard.StableSeed64;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Stable seed derivation for retryable treasure-basin cycles. */
public final class TreasureBasinSeed {
    private static final Identifier CATALOG_SALT =
            Identifier.fromNamespaceAndPath("immortalstorage", "treasure_basin_catalog");

    private TreasureBasinSeed() {
    }

    public static long selectionTicket(UUID basinId, Identifier dimension,
                                       BlockPos position, long cycle) {
        return derive(basinId, dimension, position, cycle, 0L, CATALOG_SALT);
    }

    public static long derive(UUID basinId, Identifier dimension, BlockPos position,
                              long cycle, long sourceSeed, Identifier lootTable) {
        return StableSeed64.derive(basinId, dimension.toString(), position.asLong(),
                cycle, sourceSeed, lootTable.toString());
    }
}
