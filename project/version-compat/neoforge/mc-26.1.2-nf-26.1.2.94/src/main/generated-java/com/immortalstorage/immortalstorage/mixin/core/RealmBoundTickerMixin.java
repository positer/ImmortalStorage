package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.dimension.RealmSafeBlockTicker;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Covers custom ticker implementations without entering the factory guard twice. */
@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class RealmBoundTickerMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
    private void immortalstorage$safeTick(BlockEntityTicker<?> ticker, Level level, BlockPos pos, BlockState state, BlockEntity entity, Operation<Void> original) {
        if (ticker instanceof RealmSafeBlockTicker<?>) original.call(ticker, level, pos, state, entity);
        else RealmSafeBlockTicker.execute(level, entity, () -> original.call(ticker, level, pos, state, entity));
    }
}
