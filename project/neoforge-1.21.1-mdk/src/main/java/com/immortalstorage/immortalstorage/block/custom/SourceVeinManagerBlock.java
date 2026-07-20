package com.immortalstorage.immortalstorage.block.custom;

import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.SourceVeinManagerBlockEntity;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class SourceVeinManagerBlock extends BaseEntityBlock {
    public SourceVeinManagerBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SourceVeinManagerBlock::new);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        return player != null && ImmortalStoragePlayerData.get(player).getStage() >= 6
                && ImmortalStorageDimensions.isPersonalRealmFor(context.getLevel().dimension(), player.getUUID())
                && SourceVeinManagerBlockEntity.canPlaceStackFor(context.getItemInHand(), player.getUUID())
                ? super.getStateForPlacement(context) : null;
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SourceVeinManagerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.SOURCE_VEIN_MANAGER.get(),
                (tickLevel, pos, tickState, manager) -> {
                    if (tickLevel instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        SourceVeinManagerBlockEntity.serverTick(serverLevel, pos, manager);
                    }
                });
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof SourceVeinManagerBlockEntity manager) {
            manager.tryClaimOwner(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(pos) instanceof SourceVeinManagerBlockEntity manager)
                || !manager.tryClaimOwner(serverPlayer)) return InteractionResult.FAIL;
        serverPlayer.openMenu(manager, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof SourceVeinManagerBlockEntity manager)) return drops;
        ItemStack preserved = drops.stream().filter(stack -> stack.getItem() == asItem())
                .findFirst().orElse(ItemStack.EMPTY);
        if (preserved.isEmpty()) {
            drops = new ArrayList<>(drops);
            preserved = new ItemStack(this);
            drops.add(preserved);
        }
        manager.saveToItem(preserved, builder.getLevel().registryAccess());
        return drops;
    }
}
