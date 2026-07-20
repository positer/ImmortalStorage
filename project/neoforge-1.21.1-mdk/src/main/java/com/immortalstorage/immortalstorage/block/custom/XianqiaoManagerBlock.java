package com.immortalstorage.immortalstorage.block.custom;

import com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class XianqiaoManagerBlock extends BaseEntityBlock {
    public XianqiaoManagerBlock(Properties props) { super(props); }

    @Override
    protected MapCodec<? extends XianqiaoManagerBlock> codec() {
        return simpleCodec(XianqiaoManagerBlock::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null
                || ImmortalStoragePlayerData.get(player).getStage() < 6
                || !ImmortalStorageDimensions.isPersonalRealmFor(context.getLevel().dimension(), player.getUUID())) {
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new XianqiaoManagerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.XIANQIAO_MANAGER.get(),
                (tickLevel, pos, tickState, manager) -> {
                    if (tickLevel instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        XianqiaoManagerBlockEntity.serverTick(serverLevel, pos, manager);
                    }
                });
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof XianqiaoManagerBlockEntity manager) {
            manager.tryClaimOwner(player);
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player p, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof XianqiaoManagerBlockEntity x) {
            if (!x.tryClaimOwner(p)) return InteractionResult.FAIL;
            p.openMenu(x);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
