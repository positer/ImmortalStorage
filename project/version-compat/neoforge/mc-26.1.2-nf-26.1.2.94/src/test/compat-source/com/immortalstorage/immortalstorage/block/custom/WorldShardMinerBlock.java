package com.immortalstorage.immortalstorage.block.custom;

import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.WorldShardMinerBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class WorldShardMinerBlock extends BaseEntityBlock {
    public WorldShardMinerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends WorldShardMinerBlock> codec() {
        return simpleCodec(WorldShardMinerBlock::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WorldShardMinerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.WORLD_SHARD_MINER.get(),
                (tickLevel, pos, tickState, miner) -> {
                    if (tickLevel instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        WorldShardMinerBlockEntity.serverTick(serverLevel, pos, tickState, miner);
                    }
                });
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof WorldShardMinerBlockEntity miner) {
            miner.tryClaimOwner(com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.id(player));
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof WorldShardMinerBlockEntity miner) {
            player.openMenu(miner);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockState nextState = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        if (!state.is(nextState.getBlock())
                && level.getBlockEntity(pos) instanceof WorldShardMinerBlockEntity miner) {
            for (int slot = 0; slot < miner.getCacheHandler().getSlots(); slot++) {
                ItemStack stack = miner.getCacheHandler().extractItem(slot, Integer.MAX_VALUE, false);
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
        if (!(level.getBlockEntity(pos) instanceof WorldShardMinerBlockEntity miner)) return 0;
        int filled = 0;
        for (int slot = 0; slot < miner.getCacheHandler().getSlots(); slot++) {
            if (!miner.getCacheHandler().getStackInSlot(slot).isEmpty()) filled++;
        }
        return Math.min(15, (filled * 15 + 26) / 27);
    }
}
