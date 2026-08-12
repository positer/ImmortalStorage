package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity;
import com.immortalstorage.immortalstorage.compat.mc2612.LegacyBlockEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/** Target renderer using the official 26.1 block/item submission pipeline. */
public final class SourceVeinRenderer extends LegacyBlockEntityRenderer<SourceVeinBlockEntity> {
    public SourceVeinRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected boolean submitOfficial(SourceVeinBlockEntity blockEntity, float partialTick,
                                     PoseStack poseStack, SubmitNodeCollector collector,
                                     CameraRenderState camera) {
        float worldTime = blockEntity.getLevel() == null
                ? partialTick
                : blockEntity.getLevel().getGameTime() + partialTick;
        float positionPhase = (blockEntity.getBlockPos().asLong() & 0xFFL) * 0.125F;
        SourceVeinDisplayRenderer.submit(blockEntity.definition(), worldTime + positionPhase,
                poseStack, collector, 0x00F000F0, 0);
        return true;
    }

    @Override
    protected void legacyRender(SourceVeinBlockEntity blockEntity, float partialTick,
                                PoseStack poseStack,
                                net.minecraft.client.renderer.MultiBufferSource buffers,
                                int packedLight, int packedOverlay,
                                net.minecraft.world.phys.Vec3 cameraPosition) {
        // The official hook above handles all target submissions. This body is
        // retained only for the adapter contract and is never reached.
    }
}
