package com.cultivation.cultivation.client.render;

import com.cultivation.cultivation.block.custom.YuanLightBlock;
import com.cultivation.cultivation.block.entity.YuanLightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class YuanLightRenderer implements BlockEntityRenderer<YuanLightBlockEntity> {
    public YuanLightRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(YuanLightBlockEntity entity, float partialTick, PoseStack poses,
                                 MultiBufferSource buffers, int light, int overlay) {
        if (!entity.getBlockState().getValue(YuanLightBlock.CORE_VISIBLE)) return;
        int rgb = entity.getBlockState().getValue(YuanLightBlock.COLOR).getTextureDiffuseColor();
        float time = (entity.getLevel() == null ? 0 : entity.getLevel().getGameTime()) + partialTick;
        FloatingCubeRenderer.render(poses, buffers, time, 0.5F, 0.34F,
                0.035F, 0.085F, 0.82F, 0.51F, rgb, 112);
    }
}
