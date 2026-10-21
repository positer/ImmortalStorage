package com.immortalstorage.immortalstorage.timeshift;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ticker lookup bridge.
 *
 * <p>1.21.1 asks the block through {@code EntityBlock#getTicker(Level, BlockState, BlockEntityType)};
 * 26.1 moved the same lookup onto the state as {@code BlockStateBase#getTicker(Level, BlockEntityType)}.
 * The rest of the mod only calls this class, so the generation-specific call lives in one place.</p>
 */
public final class TimeShiftTickers {
    @SuppressWarnings("unchecked")
    public static @Nullable BlockEntityTicker<BlockEntity> tickerFor(Level level, BlockState state, BlockEntity blockEntity) {
        try {
            if (!(state.getBlock() instanceof EntityBlock entityBlock)) return null;
            BlockEntityType<BlockEntity> type = (BlockEntityType<BlockEntity>) blockEntity.getType();
            return entityBlock.getTicker(level, state, type);
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private TimeShiftTickers() {}
}
