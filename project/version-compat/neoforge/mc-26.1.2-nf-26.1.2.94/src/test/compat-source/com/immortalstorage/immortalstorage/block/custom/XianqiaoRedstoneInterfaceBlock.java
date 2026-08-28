package com.immortalstorage.immortalstorage.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoRedstoneInterfaceBlockEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Redstone endpoint for Xianqiao cache thresholds. Configuration is persisted by the
 * future block entity; the block-level contract (15 strength/light while active) is stable. */
public final class XianqiaoRedstoneInterfaceBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");
    public static final MapCodec<XianqiaoRedstoneInterfaceBlock> CODEC = simpleCodec(XianqiaoRedstoneInterfaceBlock::new);
    public XianqiaoRedstoneInterfaceBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(ACTIVATED, false)); }
    @Override protected MapCodec<? extends XianqiaoRedstoneInterfaceBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(ACTIVATED); }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new XianqiaoRedstoneInterfaceBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player && level.getBlockEntity(pos) instanceof XianqiaoRedstoneInterfaceBlockEntity be) be.tryBindOwner(player);
    }
    @Override protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) { return state.getValue(ACTIVATED) ? 15 : 0; }
    @Override protected boolean isSignalSource(BlockState state) { return true; }
    @Override public int getLightEmission(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) { return state.getValue(ACTIVATED) ? 15 : 0; }
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof XianqiaoRedstoneInterfaceBlockEntity be) serverPlayer.openMenu(be, pos);
        return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
    }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != com.immortalstorage.immortalstorage.block.entity.ModBlockEntities.XIANQIAO_REDSTONE_INTERFACE.get()) return null;
        return (l,p,s,be) -> { if (be instanceof XianqiaoRedstoneInterfaceBlockEntity redstone) redstone.serverTick(); };
    }
}
