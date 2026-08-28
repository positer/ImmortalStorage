package com.immortalstorage.immortalstorage.block.custom;

import com.immortalstorage.immortalstorage.block.entity.EnergyCrystalBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.CrystalKind;
import com.immortalstorage.immortalstorage.compat.CrystalResourceCompatHooks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The placed Energy Crystal is a small machine block; its crystal geometry is
 * deliberately kept in the model so the block entity only owns machine state.
 */
public final class EnergyCrystalBlock extends BaseEntityBlock {
    public static final MapCodec<EnergyCrystalBlock> CODEC = simpleCodec(EnergyCrystalBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Exact axis-aligned bounds of the base and crystal model elements (5..11, 0..12). */
    private static final VoxelShape MODEL_BOUNDS = box(5, 0, 5, 11, 12, 11);
    private final CrystalKind kind;

    public EnergyCrystalBlock(Properties properties) {
        this(properties, CrystalKind.ELECTRIC);
    }

    public EnergyCrystalBlock(Properties properties, CrystalKind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    public CrystalKind kind() {
        return kind;
    }

    @Override protected MapCodec<? extends EnergyCrystalBlock> codec() {
        return CODEC;
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                             CollisionContext context) {
        return MODEL_BOUNDS;
    }

    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                                      CollisionContext context) {
        return MODEL_BOUNDS;
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCrystalBlockEntity(pos, state, kind);
    }

    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,
                ModBlockEntities.typeFor(kind).get(),
                (tickLevel, pos, tickState, crystal) -> {
                    if (tickLevel instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        if (com.immortalstorage.immortalstorage.block.entity.MachineRedstoneControl.allows(crystal)) EnergyCrystalBlockEntity.serverTick(serverLevel, pos, tickState, crystal);
                    }
                });
    }

    @Override
    protected ItemInteractionResult useItemOn(
            net.minecraft.world.item.ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EnergyCrystalBlockEntity crystal)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        InteractionResult result = CrystalResourceCompatHooks.useItemOn(
                crystal, player, stack, hand, hit);
        return result == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level,
                                                           BlockPos pos, Player player,
                                                           BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EnergyCrystalBlockEntity crystal) {
            player.openMenu(crystal, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos,
                                      BlockState nextState, boolean movedByPiston) {
        if (!state.is(nextState.getBlock())
                && level.getBlockEntity(pos) instanceof EnergyCrystalBlockEntity crystal) {
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                crystal.onRemovedFromWorld(serverLevel);
            }
            crystal.flushBoundCache();
            Containers.dropContents(level, pos, crystal);
        }
        super.onRemove(state, level, pos, nextState, movedByPiston);
    }
}
