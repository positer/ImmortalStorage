package com.cultivation.cultivation.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class YuanLightBlockEntity extends BlockEntity {
    public YuanLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.YUAN_LIGHT.get(), pos, state);
    }
}
