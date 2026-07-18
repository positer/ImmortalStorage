package com.cultivation.cultivation.block.entity;

import com.cultivation.cultivation.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** One loaded-chunk lookup for Immortal Yuan light hostile-spawn suppression. */
public final class YuanLightIndex {
    public static boolean suppresses(ServerLevel level, BlockPos spawnPos) {
        ChunkPos center = new ChunkPos(spawnPos);
        var chunk = level.getChunkSource().getChunkNow(center.x, center.z);
        if (chunk == null) return false;
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            if (chunk.getBlockState(pos).is(ModBlocks.IMMORTAL_YUAN_LIGHT.get())) return true;
        }
        return false;
    }

    private YuanLightIndex() {}
}
