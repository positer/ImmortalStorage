package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Renders the luminous white core held by the manager's open frame. */
public final class XianqiaoManagerRenderer implements BlockEntityRenderer<XianqiaoManagerBlockEntity> {
    private static final int CORE_COLOR = 0xF7FBFF;

    public XianqiaoManagerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(XianqiaoManagerBlockEntity blockEntity,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource buffers,
                       int packedLight,
                       int packedOverlay) {
        float animationTime = blockEntity.getLevel() == null
                ? partialTick
                : blockEntity.getLevel().getGameTime() + partialTick;

        FloatingCubeRenderer.render(
                poseStack, buffers, animationTime,
                0.5F, 0.42F,
                0.045F, 0.075F,
                0.72F, 0.43F,
                CORE_COLOR, 104);
    }
}
