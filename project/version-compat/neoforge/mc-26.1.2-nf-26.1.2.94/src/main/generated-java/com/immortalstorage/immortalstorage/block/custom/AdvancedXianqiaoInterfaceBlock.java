package com.immortalstorage.immortalstorage.block.custom;

import com.immortalstorage.immortalstorage.block.entity.AdvancedXianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.compat.XianqiaoInterfaceCompatHooks;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Owner-bound advanced Xianqiao import/export interface. Unlike the plain
 * interface it rejects its own per-face active pull/push scheduler; instead it
 * schedules every container inside a configurable bounding box exactly like the
 * advanced stabilized miniature immortal ruin series, while retaining the nine
 * mixed item/fluid/power/chemical cache slots.
 */
public final class AdvancedXianqiaoInterfaceBlock extends Block implements EntityBlock {
    public static final MapCodec<AdvancedXianqiaoInterfaceBlock> CODEC =
            simpleCodec(AdvancedXianqiaoInterfaceBlock::new);

    public AdvancedXianqiaoInterfaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends AdvancedXianqiaoInterfaceBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || ImmortalStoragePlayerData.get(player).getStage() < 6) return null;
        if (!XianqiaoInterfaceBlockEntity.canPlaceStackFor(
                context.getItemInHand(), com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.id(player))) return null;
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof AdvancedXianqiaoInterfaceBlockEntity interfaceEntity) {
            interfaceEntity.tryBindOwner(player);
        }
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AdvancedXianqiaoInterfaceBlockEntity interfaceEntity)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        InteractionResult result = XianqiaoInterfaceCompatHooks.useItemOn(
                interfaceEntity, player, stack, hand, hit);
        return result == InteractionResult.PASS
                ? InteractionResult.TRY_WITH_EMPTY_HAND
                : (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
    }

    @Override
    public InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof AdvancedXianqiaoInterfaceBlockEntity interfaceEntity)) {
            return InteractionResult.PASS;
        }
        if (!interfaceEntity.canUse(player)) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, 
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
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockState nextState = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        if (state.getBlock() != nextState.getBlock() && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof AdvancedXianqiaoInterfaceBlockEntity interfaceEntity) {
            interfaceEntity.releaseBuffersForRemoval(serverLevel);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof AdvancedXianqiaoInterfaceBlockEntity interfaceEntity) {
            interfaceEntity.releaseBuffersForRemoval(builder.getLevel());
        }
        List<ItemStack> drops = super.getDrops(state, builder);
        if (!(blockEntity instanceof AdvancedXianqiaoInterfaceBlockEntity interfaceEntity)) return drops;

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
        return new AdvancedXianqiaoInterfaceBlockEntity(pos, state);
    }

    @Override
    public @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof MenuProvider provider ? provider : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.ADVANCED_XIANQIAO_INTERFACE.get()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof AdvancedXianqiaoInterfaceBlockEntity interfaceEntity) {
                if (com.immortalstorage.immortalstorage.block.entity.MachineRedstoneControl.allows(interfaceEntity)) interfaceEntity.serverTick();
            }
        };
    }
}
