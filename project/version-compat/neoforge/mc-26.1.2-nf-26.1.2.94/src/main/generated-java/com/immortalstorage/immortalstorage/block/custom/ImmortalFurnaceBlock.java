package com.immortalstorage.immortalstorage.block.custom;

import com.immortalstorage.immortalstorage.block.entity.ImmortalFurnaceBlockEntity;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ImmortalFurnaceBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public ImmortalFurnaceBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(HorizontalDirectionalBlock.FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected MapCodec<? extends ImmortalFurnaceBlock> codec() {
        return simpleCodec(ImmortalFurnaceBlock::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new ImmortalFurnaceBlockEntity(p, s);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> t) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof ImmortalFurnaceBlockEntity f && lvl instanceof net.minecraft.server.level.ServerLevel sl) {
                if (!com.immortalstorage.immortalstorage.block.entity.MachineRedstoneControl.allows(f)) return;
                if (f.isAutoConsume() || ImmortalStorageDimensions.isXianqiaoRealm(lvl.dimension())) {
                    f.tryAutoRefuelFromRealmOwner(sl);
                }
                ImmortalFurnaceBlockEntity.serverTick(sl, pos, st, f);
            }
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player p, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ImmortalFurnaceBlockEntity f) {
            if (p.isShiftKeyDown() && p instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                    && com.immortalstorage.immortalstorage.dimension.RealmHelper.isInOwnRealm(serverPlayer)) {
                f.setAutoConsume(!f.isAutoConsume());
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(p, Component.translatable(f.isAutoConsume()
                        ? "message.immortalstorage.immortal_furnace.auto_consume.enabled"
                        : "message.immortalstorage.immortal_furnace.auto_consume.disabled"), true);
                return InteractionResult.CONSUME;
            }
            p.openMenu(f);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockState nextState = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        if (!state.is(nextState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ImmortalFurnaceBlockEntity furnace) {
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    Containers.dropContents(level, pos, furnace);
                    furnace.getRecipesToAwardAndPopExperience(serverLevel, Vec3.atCenterOf(pos));
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
