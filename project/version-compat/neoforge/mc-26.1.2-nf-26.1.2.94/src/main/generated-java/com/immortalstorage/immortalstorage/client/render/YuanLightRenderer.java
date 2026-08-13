package com.immortalstorage.immortalstorage.client.render;
import com.immortalstorage.immortalstorage.compat.mc2612.LegacyBlockEntityRenderer;

import com.immortalstorage.immortalstorage.block.custom.YuanLightBlock;
import com.immortalstorage.immortalstorage.block.entity.YuanLightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class YuanLightRenderer extends LegacyBlockEntityRenderer<YuanLightBlockEntity> {
    public YuanLightRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void legacyRender(YuanLightBlockEntity entity, float partialTick, PoseStack poses,
                                 MultiBufferSource buffers, int light, int overlay, net.minecraft.world.phys.Vec3 cameraPosition) {
        if (!entity.getBlockState().getValue(YuanLightBlock.CORE_VISIBLE)) return;
        int rgb = entity.getBlockState().getValue(YuanLightBlock.COLOR).getTextureDiffuseColor();
        float time = (entity.getLevel() == null ? 0 : entity.getLevel().getGameTime()) + partialTick;
        FloatingCubeRenderer.render(poses, buffers, time, 0.5F, 0.34F,
                0.035F, 0.085F, 0.82F, 0.51F, rgb, 112);
    }
}
