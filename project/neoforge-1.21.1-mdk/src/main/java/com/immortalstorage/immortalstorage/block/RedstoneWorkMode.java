package com.immortalstorage.immortalstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Three-state redstone gate shared by ImmortalStorage machines. */
public enum RedstoneWorkMode {
    SIGNAL_WORK, NO_SIGNAL_WORK, IGNORE;

    public boolean allows(Level level, BlockPos pos) {
        return allows(level.hasNeighborSignal(pos));
    }
    public boolean allows(boolean signal) { return this == IGNORE || (this == SIGNAL_WORK ? signal : !signal); }
    public RedstoneWorkMode next() { return values()[(ordinal() + 1) % values().length]; }
    public static RedstoneWorkMode byId(int id) {
        return values()[Math.max(0, Math.min(values().length - 1, id))];
    }
}
