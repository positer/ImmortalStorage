package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.custom.MiniatureImmortalRuinBlock;
import com.immortalstorage.immortalstorage.client.render.MiniatureImmortalRuinRenderer;
import com.immortalstorage.immortalstorage.compat.mc2612.LegacyBlockEntityRenderer;
import com.immortalstorage.immortalstorage.compat.patchouli.HandbookPreviewStates;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Patchouli 26 previews only baked models; the ruin's world core is block-entity geometry. */
@Mixin(targets = "vazkii.patchouli.client.multiblock.MultiblockPiPRenderer", remap = false)
public abstract class PatchouliHandbookCoreMixin {
    @WrapOperation(method = "renderToTexture(Lvazkii/patchouli/client/multiblock/MultiblockPiPRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockModelRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void immortalstorage$core(BlockModelRenderState model, PoseStack poses, SubmitNodeCollector collector,
                                      int light, int overlay, int outline, Operation<Void> original) {
        original.call(model, poses, collector, light, overlay, outline);
        var state = HandbookPreviewStates.source(model);
        if (state == null || !state.is(ModBlocks.MINIATURE_IMMORTAL_RUIN.get())) return;
        var mc = Minecraft.getInstance();
        float time = mc.level == null ? 0F : mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        poses.pushPose();
        // Patchouli has already translated to pos.getCenter(); its baked quads
        // still occupy [0,1]. Keep the world's +0.5 core translation as well.
        LegacyBlockEntityRenderer.submitLegacyGeometry(poses, collector,
                buffers -> MiniatureImmortalRuinRenderer.drawAnimated(poses, buffers, time,
                        state.getValue(MiniatureImmortalRuinBlock.REVERSED)));
        poses.popPose();
    }
}
