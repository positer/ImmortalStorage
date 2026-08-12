package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class YuanLightBlockEntity extends com.immortalstorage.immortalstorage.compat.mc2612.CompatBlockEntity {
    public YuanLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.YUAN_LIGHT.get(), pos, state);
    }
}
