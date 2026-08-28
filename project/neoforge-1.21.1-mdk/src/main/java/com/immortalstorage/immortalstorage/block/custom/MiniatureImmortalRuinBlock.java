package com.immortalstorage.immortalstorage.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Unbreakable, non-colliding field anchor; behavior is driven by its block entity in later stages. */
public final class MiniatureImmortalRuinBlock extends BaseEntityBlock {
    public static final MapCodec<MiniatureImmortalRuinBlock> CODEC = simpleCodec(MiniatureImmortalRuinBlock::new);
    public static final BooleanProperty REVERSED = BooleanProperty.create("reversed");

    public MiniatureImmortalRuinBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(REVERSED, false));
    }

    @Override protected MapCodec<? extends MiniatureImmortalRuinBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem)
                || com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem.getMode(stack)
                != com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem.MODE_WRENCH) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity ruin) {
            if (player.isShiftKeyDown()) {
                ruin.unlinkForBreak();
                level.levelEvent(2001, pos, Block.getId(state));
                level.removeBlock(pos, false);
                Block.popResource(level, pos, new ItemStack(
                        com.immortalstorage.immortalstorage.item.ModItems.MINIATURE_IMMORTAL_RUIN.get()));
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
            } else {
                com.immortalstorage.immortalstorage.item.custom.RuinLinkingService.interact(serverPlayer, stack, ruin);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity ruin) {
            if (player.isShiftKeyDown()) level.setBlock(pos, state.cycle(REVERSED), 3);
            else serverPlayer.openMenu(ruin, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(REVERSED);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity(pos, state);
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,
                com.immortalstorage.immortalstorage.block.entity.ModBlockEntities.MINIATURE_IMMORTAL_RUIN.get(),
                (tickLevel, pos, tickState, ruin) -> { if (com.immortalstorage.immortalstorage.block.entity.MachineRedstoneControl.allows(ruin)) ruin.serverTick(); });
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState nextState, boolean movedByPiston) {
        if (!state.is(nextState.getBlock())
                && level.getBlockEntity(pos) instanceof com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity ruin) {
            ruin.unlinkForBreak();
        }
        super.onRemove(state, level, pos, nextState, movedByPiston);
    }
}
