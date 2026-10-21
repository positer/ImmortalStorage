package com.immortalstorage.immortalstorage.timeshift;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 26.1.2 ticker lookup: the lookup moved from {@code Block#getTicker(Level, BlockState, type)}
 * to {@code BlockStateBase#getTicker(Level, type)}. 1.21.1 keeps its own copy in the canonical tree.
 */
public final class TimeShiftTickers {
    @SuppressWarnings("unchecked")
    public static @Nullable BlockEntityTicker<BlockEntity> tickerFor(Level level, BlockState state, BlockEntity blockEntity) {
        try {
            BlockEntityType<BlockEntity> type = (BlockEntityType<BlockEntity>) blockEntity.getType();
            return state.getTicker(level, type);
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private TimeShiftTickers() {}
}
