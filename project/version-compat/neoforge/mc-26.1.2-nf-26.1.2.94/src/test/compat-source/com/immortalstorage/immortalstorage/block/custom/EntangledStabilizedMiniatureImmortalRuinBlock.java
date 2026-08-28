package com.immortalstorage.immortalstorage.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Merged opposite-state miniature-ruin field carrying the stabilized ruin frame. */
public final class EntangledStabilizedMiniatureImmortalRuinBlock extends BaseEntityBlock {
    public static final MapCodec<EntangledStabilizedMiniatureImmortalRuinBlock> CODEC =
            simpleCodec(EntangledStabilizedMiniatureImmortalRuinBlock::new);

    public EntangledStabilizedMiniatureImmortalRuinBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends EntangledStabilizedMiniatureImmortalRuinBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem)
                || com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem.getMode(stack)
                != com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem.MODE_WRENCH) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()
                && level.getBlockEntity(pos) instanceof com.immortalstorage.immortalstorage.block.entity.EntangledStabilizedMiniatureImmortalRuinBlockEntity ruin) {
            ItemStack dropped = new ItemStack(com.immortalstorage.immortalstorage.block.ModBlocks.ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get());
            CompoundTag blockData = ruin.saveWithFullMetadata(player.registryAccess());
            dropped.set(DataComponents.CUSTOM_DATA, CustomData.of(blockData));
            level.levelEvent(2001, pos, Block.getId(state));
            level.removeBlock(pos, false);
            Block.popResource(level, pos, dropped);
            stack.hurtAndBreak(1, player, com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayer.slotForHand(hand));
        }
        return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.immortalstorage.immortalstorage.block.entity.EntangledStabilizedMiniatureImmortalRuinBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type,
                com.immortalstorage.immortalstorage.block.entity.ModBlockEntities.ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get(),
                (tickLevel, pos, tickState, ruin) -> { if (com.immortalstorage.immortalstorage.block.entity.MachineRedstoneControl.allows(ruin)) ruin.serverTick(); });
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof com.immortalstorage.immortalstorage.block.entity.EntangledStabilizedMiniatureImmortalRuinBlockEntity ruin) {
            serverPlayer.openMenu(ruin, pos);
        }
        return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
    }
}
