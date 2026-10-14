package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.compat.patchouli.HandbookPreviewStates;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vazkii.patchouli.client.multiblock.MultiblockPiPRenderState.BlockRenderState;

@Mixin(targets = "vazkii.patchouli.client.book.page.PageMultiblock", remap = false)
public abstract class PatchouliHandbookPreviewMixin {
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockModelResolver;update(Lnet/minecraft/client/renderer/block/BlockModelRenderState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/model/BlockDisplayContext;)V"))
    private void immortalstorage$remember(BlockModelResolver resolver, BlockModelRenderState model,
                                          BlockState state, BlockDisplayContext context, Operation<Void> original) {
        original.call(resolver, model, state, context);
        HandbookPreviewStates.remember(model, state);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lvazkii/patchouli/client/book/page/PageMultiblock;renderMultiblock(Lnet/minecraft/client/gui/GuiGraphicsExtractor;FFIILorg/joml/Matrix4f;Ljava/util/List;)V"))
    private void immortalstorage$centre(GuiGraphicsExtractor gui, float x, float y, int width, int height,
                                         Matrix4f matrix, List<BlockRenderState> blocks, Operation<Void> original) {
        boolean ours = blocks.stream().anyMatch(block -> {
            BlockState state = HandbookPreviewStates.source(block.blockModelRenderState());
            return state != null && (state.is(ModBlocks.MINIATURE_IMMORTAL_RUIN.get())
                    || state.is(ModBlocks.WORLD_SHARD_MINER.get()) || state.is(ModBlocks.TREASURE_BASIN.get()));
        });
        if (ours && !blocks.isEmpty()) {
            int minX = Integer.MAX_VALUE, minY = minX, minZ = minX;
            int maxX = Integer.MIN_VALUE, maxY = maxX, maxZ = maxX;
            for (BlockRenderState block : blocks) {
                var pos = block.pos();
                minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
                minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
                minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
            }
            var start = gui.pose().transformPosition(new org.joml.Vector2f(x, y));
            var end = gui.pose().transformPosition(new org.joml.Vector2f(x + width, y + height));
            int viewportHeight = Math.round(end.y) - Math.round(start.y);
            matrix = com.immortalstorage.immortalstorage.client.render.HandbookPreviewCamera.centre(
                    matrix, minX, minY, minZ, maxX, maxY, maxZ, viewportHeight);
        }
        original.call(gui, x, y, width, height, matrix, blocks);
    }
}
