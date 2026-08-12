package com.immortalstorage.immortalstorage.block.custom;

import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.TreasureBasinBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class TreasureBasinBlock extends BaseEntityBlock {
    public TreasureBasinBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends TreasureBasinBlock> codec() {
        return simpleCodec(TreasureBasinBlock::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Opaque basin geometry naturally clips any beacon-style beam passing through it. */
    @Override
    protected int getLightDampening(BlockState state) {
        return 15;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TreasureBasinBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.TREASURE_BASIN.get(),
                (tickLevel, pos, tickState, basin) -> {
                    if (tickLevel instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        TreasureBasinBlockEntity.serverTick(serverLevel, pos, basin);
                    }
                });
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TreasureBasinBlockEntity basin) {
            serverPlayer.openMenu(basin, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockState nextState = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        if (!state.is(nextState.getBlock())
                && level.getBlockEntity(pos) instanceof TreasureBasinBlockEntity basin) {
            for (int slot = 0; slot < basin.getCacheHandler().getSlots(); slot++) {
                ItemStack stack = basin.getCacheHandler().extractItem(slot, Integer.MAX_VALUE, false);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof TreasureBasinBlockEntity basin)) return 0;
        int filled = 0;
        for (int slot = 0; slot < basin.getCacheHandler().getSlots(); slot++) {
            if (!basin.getCacheHandler().getStackInSlot(slot).isEmpty()) filled++;
        }
        return Math.min(15, (filled * 15 + 26) / 27);
    }
}
