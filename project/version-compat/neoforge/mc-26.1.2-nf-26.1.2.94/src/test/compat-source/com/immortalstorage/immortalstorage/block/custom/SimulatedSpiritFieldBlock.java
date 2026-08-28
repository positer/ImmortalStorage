package com.immortalstorage.immortalstorage.block.custom;

import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.SimulatedSpiritFieldBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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

public final class SimulatedSpiritFieldBlock extends BaseEntityBlock {
    public static final MapCodec<SimulatedSpiritFieldBlock> CODEC = simpleCodec(SimulatedSpiritFieldBlock::new);

    public SimulatedSpiritFieldBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends SimulatedSpiritFieldBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimulatedSpiritFieldBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.SIMULATED_SPIRIT_FIELD.get(),
                (tickLevel, pos, tickState, field) -> {
                    if (tickLevel instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        if (com.immortalstorage.immortalstorage.block.entity.MachineRedstoneControl.allows(field)) SimulatedSpiritFieldBlockEntity.serverTick(serverLevel, pos, tickState, field);
                    }
                });
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                                         BlockPos pos, Player player,
                                                         net.minecraft.world.InteractionHand hand,
                                                         BlockHitResult hit) {
        if (!(stack.getItem() instanceof BlockItem blockItem)
                || !(level.getBlockEntity(pos) instanceof SimulatedSpiritFieldBlockEntity field)
                || !field.isValidSubstrate(blockItem.getBlock().defaultBlockState())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            field.replaceSubstrate(serverPlayer, blockItem.getBlock().defaultBlockState(), stack);
        }
        return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                          Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SimulatedSpiritFieldBlockEntity field) {
            player.openMenu(field, pos);
        }
        return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
    }

    @Override protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockState nextState = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        if (!state.is(nextState.getBlock())
                && level.getBlockEntity(pos) instanceof SimulatedSpiritFieldBlockEntity field) {
            Containers.dropContents(level, pos, field);
            for (ItemStack pending : field.drainPendingOutputForRemoval()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), pending);
            }
            field.dropStoredSubstrate();
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
