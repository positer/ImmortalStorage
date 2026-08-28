package com.immortalstorage.immortalstorage.block.custom;

import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.compat.XianqiaoInterfaceCompatHooks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Owner-bound Xianqiao import/export interface with a front-facing model. */
public final class XianqiaoInterfaceBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<XianqiaoInterfaceBlock> CODEC = simpleCodec(XianqiaoInterfaceBlock::new);

    public XianqiaoInterfaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || ImmortalStoragePlayerData.get(player).getStage() < 6) return null;
        if (!XianqiaoInterfaceBlockEntity.canPlaceStackFor(
                context.getItemInHand(), com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.id(player))) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof XianqiaoInterfaceBlockEntity interfaceEntity) {
            interfaceEntity.tryBindOwner(player);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof XianqiaoInterfaceBlockEntity interfaceEntity)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        InteractionResult result = XianqiaoInterfaceCompatHooks.useItemOn(
                interfaceEntity, player, stack, hand, hit);
        return result == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof XianqiaoInterfaceBlockEntity interfaceEntity)) {
            return InteractionResult.PASS;
        }
        if (!interfaceEntity.canUse(player)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.immortalstorage.xianqiao_interface.not_owner"), true);
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(interfaceEntity, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState nextState, boolean movedByPiston) {
        if (state.getBlock() != nextState.getBlock() && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof XianqiaoInterfaceBlockEntity interfaceEntity) {
            interfaceEntity.releaseBuffersForRemoval(serverLevel);
        }
        super.onRemove(state, level, pos, nextState, movedByPiston);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        // Settle and clear real buffers before any loot function can inspect
        // block-entity data. The BE guard makes the later onRemove path a no-op.
        if (blockEntity instanceof XianqiaoInterfaceBlockEntity interfaceEntity) {
            interfaceEntity.releaseBuffersForRemoval(builder.getLevel());
        }
        List<ItemStack> drops = super.getDrops(state, builder);
        if (!(blockEntity instanceof XianqiaoInterfaceBlockEntity interfaceEntity)) return drops;

        ItemStack interfaceDrop = ItemStack.EMPTY;
        for (ItemStack drop : drops) {
            if (drop.getItem() == asItem()) {
                interfaceDrop = drop;
                break;
            }
        }
        if (interfaceDrop.isEmpty()) {
            drops = new ArrayList<>(drops);
            interfaceDrop = new ItemStack(this);
            drops.add(interfaceDrop);
        }
        interfaceEntity.saveToItem(interfaceDrop, builder.getLevel().registryAccess());
        return drops;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new XianqiaoInterfaceBlockEntity(pos, state);
    }

    @Override
    public @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof MenuProvider provider ? provider : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.XIANQIAO_INTERFACE.get()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof XianqiaoInterfaceBlockEntity interfaceEntity) {
                if (com.immortalstorage.immortalstorage.block.entity.MachineRedstoneControl.allows(interfaceEntity)) interfaceEntity.serverTick();
            }
        };
    }
}
