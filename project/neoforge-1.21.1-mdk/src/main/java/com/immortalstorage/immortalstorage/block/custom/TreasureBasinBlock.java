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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class TreasureBasinBlock extends BaseEntityBlock {
    private static final VoxelShape MODEL_BOUNDS = box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D);

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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return MODEL_BOUNDS;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return MODEL_BOUNDS;
    }

    /** Opaque basin geometry naturally clips any beacon-style beam passing through it. */
    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 15;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TreasureBasinBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.TREASURE_BASIN.get(),
                (tickLevel, pos, tickState, basin) -> {
                    if (tickLevel instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        if (com.immortalstorage.immortalstorage.block.entity.MachineRedstoneControl.allows(basin)) TreasureBasinBlockEntity.serverTick(serverLevel, pos, basin);
                    }
                });
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TreasureBasinBlockEntity basin) {
            serverPlayer.openMenu(basin, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState nextState, boolean movedByPiston) {
        if (!state.is(nextState.getBlock())
                && level.getBlockEntity(pos) instanceof TreasureBasinBlockEntity basin) {
            for (int slot = 0; slot < basin.getCacheHandler().getSlots(); slot++) {
                ItemStack stack = basin.getCacheHandler().extractItem(slot, Integer.MAX_VALUE, false);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
            for (ItemStack pending : basin.drainPendingOutputForRemoval()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), pending);
            }
            ItemStack plugin = basin.reinforcementPlugin();
            if (!plugin.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), plugin.copy());
                basin.setReinforcementPlugin(ItemStack.EMPTY);
            }
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, nextState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TreasureBasinBlockEntity basin)) return 0;
        int filled = 0;
        for (int slot = 0; slot < basin.getCacheHandler().getSlots(); slot++) {
            if (!basin.getCacheHandler().getStackInSlot(slot).isEmpty()) filled++;
        }
        return Math.min(15, (filled * 15 + 26) / 27);
    }
}
