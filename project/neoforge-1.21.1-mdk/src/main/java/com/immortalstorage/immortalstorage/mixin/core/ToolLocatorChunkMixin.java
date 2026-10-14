package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.locator.ToolLocatorManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class ToolLocatorChunkMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void immortalstorage$changed(BlockPos pos, BlockState state, boolean moved,
                                         CallbackInfoReturnable<BlockState> result) {
        var chunk = (LevelChunk)(Object)this;
        if (result.getReturnValue() != null && result.getReturnValue() != state && chunk.getLevel() instanceof ServerLevel level)
            ToolLocatorManager.blockChanged(level,pos,result.getReturnValue(),state);
    }
    @Inject(method = "setBlockEntity", at = @At("TAIL"))
    private void immortalstorage$entity(BlockEntity entity, CallbackInfo info) {
        var chunk = (LevelChunk)(Object)this;
        if (chunk.getLevel() instanceof ServerLevel level) ToolLocatorManager.entityChanged(level,entity.getBlockPos());
    }
    @Inject(method = "removeBlockEntity", at = @At("TAIL"))
    private void immortalstorage$removed(BlockPos pos, CallbackInfo info) {
        var chunk = (LevelChunk)(Object)this;
        if (chunk.getLevel() instanceof ServerLevel level) ToolLocatorManager.entityChanged(level,pos);
    }
}
