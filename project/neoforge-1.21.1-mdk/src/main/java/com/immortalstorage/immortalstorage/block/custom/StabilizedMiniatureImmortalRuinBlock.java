package com.immortalstorage.immortalstorage.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Persistent inventory machine; its block entity owns all mutable settings. */
public final class StabilizedMiniatureImmortalRuinBlock extends BaseEntityBlock {
    public static final MapCodec<StabilizedMiniatureImmortalRuinBlock> CODEC = simpleCodec(StabilizedMiniatureImmortalRuinBlock::new);

    public StabilizedMiniatureImmortalRuinBlock(Properties properties) { super(properties); }

    @Override
    protected MapCodec<? extends StabilizedMiniatureImmortalRuinBlock> codec() { return CODEC; }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem)
                || com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem.getMode(stack)
                != com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem.MODE_WRENCH) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof com.immortalstorage.immortalstorage.block.entity.StabilizedMiniatureImmortalRuinBlockEntity ruin) {
            if (player.isShiftKeyDown()) {
                ruin.preparePortableRemoval();
                ItemStack dropped = new ItemStack(com.immortalstorage.immortalstorage.block.ModBlocks.STABILIZED_MINIATURE_IMMORTAL_RUIN.get());
                CompoundTag blockData = ruin.saveWithFullMetadata(player.registryAccess());
                dropped.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockData));
                level.levelEvent(2001, pos, Block.getId(state));
                level.removeBlock(pos, false);
                Block.popResource(level, pos, dropped);
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
            } else {
                com.immortalstorage.immortalstorage.item.custom.RuinLinkingService.interact(serverPlayer, stack, ruin);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.immortalstorage.immortalstorage.block.entity.StabilizedMiniatureImmortalRuinBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,
                com.immortalstorage.immortalstorage.block.entity.ModBlockEntities.STABILIZED_MINIATURE_IMMORTAL_RUIN.get(),
                (tickLevel, pos, tickState, ruin) -> ruin.serverTick());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState nextState, boolean movedByPiston) {
        if (!state.is(nextState.getBlock())
                && level.getBlockEntity(pos) instanceof com.immortalstorage.immortalstorage.block.entity.StabilizedMiniatureImmortalRuinBlockEntity ruin) {
            ruin.handleBlockRemoval();
        }
        super.onRemove(state, level, pos, nextState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof com.immortalstorage.immortalstorage.block.entity.StabilizedMiniatureImmortalRuinBlockEntity ruin) {
            if (player.isShiftKeyDown()) ruin.toggleReversed();
            else serverPlayer.openMenu(ruin, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
