package com.immortalstorage.immortalstorage.block.custom;

import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.YuanLightBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class YuanLightBlock extends BaseEntityBlock {
    public static final EnumProperty<net.minecraft.world.item.DyeColor> COLOR =
            EnumProperty.create("color", net.minecraft.world.item.DyeColor.class);
    public static final BooleanProperty CORE_VISIBLE = BooleanProperty.create("core_visible");
    private static final VoxelShape CORE = box(5, 5, 5, 11, 11, 11);
    private final boolean immortal;

    public YuanLightBlock(boolean immortal) {
        super(Properties.of().mapColor(MapColor.NONE).noOcclusion().noCollission().instabreak()
                .lightLevel(state -> immortal ? 15 : 8));
        this.immortal = immortal;
        registerDefaultState(stateDefinition.any().setValue(COLOR, net.minecraft.world.item.DyeColor.WHITE)
                .setValue(CORE_VISIBLE, true));
    }

    public boolean immortal() { return immortal; }

    @Override protected MapCodec<? extends YuanLightBlock> codec() {
        return simpleCodec(ignored -> new YuanLightBlock(immortal));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(COLOR, CORE_VISIBLE);
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CORE;
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                        Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof DyeItem dye)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && state.getValue(COLOR) != dye.getDyeColor()) {
            level.setBlock(pos, state.setValue(COLOR, dye.getDyeColor()), 3);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                             Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            level.setBlock(pos, state.cycle(CORE_VISIBLE), 3);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new YuanLightBlockEntity(pos, state);
    }
}
