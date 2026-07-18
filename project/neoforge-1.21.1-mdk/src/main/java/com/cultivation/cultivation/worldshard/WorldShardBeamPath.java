package com.cultivation.cultivation.worldshard;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.IntPredicate;

/** Generic world-geometry clipping for the miner's visual white beam. */
public final class WorldShardBeamPath {
    public static final int OPEN_SKY_RENDER_HEIGHT = 1024;

    private WorldShardBeamPath() {
    }

    /**
     * The pyramid alone controls machine activation. This method only computes
     * how far the visual beam can be drawn before ordinary opaque geometry.
     */
    public static int renderHeight(Level level, BlockPos origin) {
        return renderHeightFromBlockingY(origin.getY(), firstBlockingY(level, origin));
    }

    static int renderHeightFromBlockingY(int originY, int blockingY) {
        return blockingY < 0
                ? OPEN_SKY_RENDER_HEIGHT
                : Math.max(1, blockingY - originY);
    }

    private static int firstBlockingY(Level level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                origin.getX(), origin.getY(), origin.getZ());
        return firstBlockingY(origin.getY(), level.getMaxBuildHeight(), y -> {
            cursor.set(origin.getX(), y, origin.getZ());
            BlockState state = level.getBlockState(cursor);
            return blocksBeaconBeam(state.getLightBlock(level, cursor), state.is(Blocks.BEDROCK));
        });
    }

    static int firstBlockingY(int originY, int maxBuildHeight, IntPredicate blocksBeamAtY) {
        for (int y = originY + 1; y < maxBuildHeight; y++) {
            if (blocksBeamAtY.test(y)) return y;
        }
        return -1;
    }

    static boolean blocksBeaconBeam(int lightBlock, boolean bedrock) {
        return lightBlock >= 15 && !bedrock;
    }
}
