package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Pure visual-state mapping shared by the world-shard miner renderer and tests. */
public final class WorldShardMinerAppearance {
    public static final int INACTIVE_CORE_COLOR = 0xF7FBFF;
    public static final int BEAM_COLOR = 0xFFFFFFFF;

    private WorldShardMinerAppearance() {
    }

    /** The enclosure is always the runtime vanilla clear-glass block. */
    public static BlockState glassCover() {
        return Blocks.GLASS.defaultBlockState();
    }

    /** Active modes tint only the synchronized inner core; inactive is white. */
    public static int coreColor(boolean active, int modeArgb) {
        return active ? modeArgb & 0xFFFFFF : INACTIVE_CORE_COLOR;
    }

    /** The beacon beam intentionally stays vanilla white in every mode. */
    public static int beamColor() {
        return BEAM_COLOR;
    }
}
