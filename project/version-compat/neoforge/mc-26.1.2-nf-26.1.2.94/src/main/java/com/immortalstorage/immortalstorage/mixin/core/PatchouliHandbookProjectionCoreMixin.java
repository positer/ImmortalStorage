package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.custom.MiniatureImmortalRuinBlock;
import com.immortalstorage.immortalstorage.client.render.MiniatureImmortalRuinRenderer;
import com.immortalstorage.immortalstorage.compat.mc2612.LegacyBlockEntityRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vazkii.patchouli.client.multiblock.GhostBlockGeometry;

/** Supply the same animated core with the book ghost's alpha when projecting a missing controller. */
@Mixin(targets = "vazkii.patchouli.client.handler.MultiblockVisualizationHandler", remap = false)
public abstract class PatchouliHandbookProjectionCoreMixin {
    @WrapOperation(method = "renderMultiblock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeStorage;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V"))
    private void immortalstorage$core(SubmitNodeStorage collector, PoseStack poses, RenderType layer,
                                      SubmitNodeCollector.CustomGeometryRenderer geometry, Operation<Void> original) {
        if (!(geometry instanceof GhostBlockGeometry ghost) || !ghost.state().is(ModBlocks.MINIATURE_IMMORTAL_RUIN.get())) {
            original.call(collector, poses, layer, geometry); return;
        }
        var mc = Minecraft.getInstance();
        float time = mc.level == null ? 0F : mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        poses.pushPose();
        float offset = (1F - ghost.scale()) / 2F;
        poses.translate(ghost.position().getX() + offset, ghost.position().getY() + offset, ghost.position().getZ() + offset);
        poses.scale(ghost.scale(), ghost.scale(), ghost.scale());
        LegacyBlockEntityRenderer.submitLegacyGeometry(poses, collector,
                buffers -> MiniatureImmortalRuinRenderer.drawProjection(poses, buffers, time,
                        ghost.state().getValue(MiniatureImmortalRuinBlock.REVERSED), ghost.alpha()));
        poses.popPose();
    }
}
