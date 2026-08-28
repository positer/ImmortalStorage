package com.immortalstorage.immortalstorage.block.custom;

import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.SimulatedReincarnationFurnaceBlockEntity;
import com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

public final class SimulatedReincarnationFurnaceBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SimulatedReincarnationFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, LIT);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override protected MapCodec<? extends SimulatedReincarnationFurnaceBlock> codec() {
        return simpleCodec(SimulatedReincarnationFurnaceBlock::new);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos,
                                      RandomSource random) {
        if (!state.getValue(LIT)) return;
        for (int i = 0; i < 2; i++) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.32D + random.nextDouble() * 0.36D,
                    pos.getY() + 0.18D + random.nextDouble() * 0.30D,
                    pos.getZ() + 0.32D + random.nextDouble() * 0.36D,
                    0.0D, 0.015D, 0.0D);
        }
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimulatedReincarnationFurnaceBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,
                ModBlockEntities.SIMULATED_REINCARNATION_FURNACE.get(),
                (tickLevel, pos, tickState, furnace) -> {
                    if (tickLevel instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        if (com.immortalstorage.immortalstorage.block.entity.MachineRedstoneControl.allows(furnace)) SimulatedReincarnationFurnaceBlockEntity.serverTick(
                                serverLevel, pos, tickState, furnace);
                    }
                });
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                                         BlockPos pos, Player player,
                                                         net.minecraft.world.InteractionHand hand,
                                                         BlockHitResult hit) {
        if (player.isShiftKeyDown() && stack.getItem() instanceof SpiritStaffItem
                && level.getBlockEntity(pos) instanceof SimulatedReincarnationFurnaceBlockEntity furnace) {
            if (!level.isClientSide) furnace.dropAsItem((ServerPlayer) player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                       Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof SimulatedReincarnationFurnaceBlockEntity furnace) {
            serverPlayer.openMenu(furnace, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
    @Override protected java.util.List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (tool == null || tool.isEmpty()) return java.util.List.of();
        var silk = builder.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
        return tool.getEnchantmentLevel(silk) > 0
                ? java.util.List.of(preservedDrop(builder)) : java.util.List.of();
    }
    private ItemStack preservedDrop(LootParams.Builder builder) {
        ItemStack drop = new ItemStack(this);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof SimulatedReincarnationFurnaceBlockEntity furnace) {
            furnace.saveToItem(drop, builder.getLevel().registryAccess());
        }
        return drop;
    }
}
