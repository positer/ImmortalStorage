package com.immortalstorage.immortalstorage.worldshard;

import com.immortalstorage.core.worldshard.StableSeed64;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Stable seed derivation for retryable treasure-basin cycles. */
public final class TreasureBasinSeed {
    private static final ResourceLocation CATALOG_SALT =
            ResourceLocation.fromNamespaceAndPath("immortalstorage", "treasure_basin_catalog");

    private TreasureBasinSeed() {
    }

    public static long selectionTicket(UUID basinId, ResourceLocation dimension,
                                       BlockPos position, long cycle) {
        return derive(basinId, dimension, position, cycle, 0L, CATALOG_SALT);
    }

    public static long derive(UUID basinId, ResourceLocation dimension, BlockPos position,
                              long cycle, long sourceSeed, ResourceLocation lootTable) {
        return StableSeed64.derive(basinId, dimension.toString(), position.asLong(),
                cycle, sourceSeed, lootTable.toString());
    }
}
