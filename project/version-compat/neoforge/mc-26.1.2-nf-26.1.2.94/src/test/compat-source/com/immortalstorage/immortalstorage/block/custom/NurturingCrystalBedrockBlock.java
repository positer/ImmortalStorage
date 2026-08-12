package com.immortalstorage.immortalstorage.block.custom;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Budding block implemented from the vanilla growth rules without copying another mod. */
public final class NurturingCrystalBedrockBlock extends Block {
    public static final MapCodec<NurturingCrystalBedrockBlock> CODEC = simpleCodec(NurturingCrystalBedrockBlock::new);

    public NurturingCrystalBedrockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends NurturingCrystalBedrockBlock> codec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) return;
        Direction direction = Direction.getRandom(random);
        BlockPos targetPos = pos.relative(direction);
        BlockState target = level.getBlockState(targetPos);
        BlockState next = null;
        if (target.isAir() || target.is(net.minecraft.world.level.block.Blocks.WATER)
                && target.getFluidState().getAmount() == 8) {
            next = ModBlocks.SMALL_NURTURING_CRYSTAL_BUD.get().defaultBlockState();
        } else if (target.is(ModBlocks.SMALL_NURTURING_CRYSTAL_BUD.get())
                && target.getValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING) == direction) {
            next = ModBlocks.MEDIUM_NURTURING_CRYSTAL_BUD.get().defaultBlockState();
        } else if (target.is(ModBlocks.MEDIUM_NURTURING_CRYSTAL_BUD.get())
                && target.getValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING) == direction) {
            next = ModBlocks.LARGE_NURTURING_CRYSTAL_BUD.get().defaultBlockState();
        } else if (target.is(ModBlocks.LARGE_NURTURING_CRYSTAL_BUD.get())
                && target.getValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING) == direction) {
            next = ModBlocks.NURTURING_CRYSTAL_CLUSTER.get().defaultBlockState();
        }
        if (next == null) return;
        next = next.setValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING, direction)
                .setValue(net.minecraft.world.level.block.AmethystClusterBlock.WATERLOGGED,
                        target.getFluidState().getType() == net.minecraft.world.level.material.Fluids.WATER);
        level.setBlockAndUpdate(targetPos, next);
    }
}
