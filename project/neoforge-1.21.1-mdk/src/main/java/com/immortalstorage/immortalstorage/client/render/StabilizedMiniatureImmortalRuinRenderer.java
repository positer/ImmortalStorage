package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.StabilizedMiniatureImmortalRuinBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Internal ruin sphere plus optional white selection outline. */
public final class StabilizedMiniatureImmortalRuinRenderer implements BlockEntityRenderer<StabilizedMiniatureImmortalRuinBlockEntity> {
    public StabilizedMiniatureImmortalRuinRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(StabilizedMiniatureImmortalRuinBlockEntity entity, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        float time = (entity.getLevel() == null ? 0 : entity.getLevel().getGameTime()) + partialTick;
        float scale = 0.27F + Mth.sin(time * 0.12F) * 0.03F;
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        poses.pushPose();
        poses.translate(0.5F, 0.5F, 0.5F);
        poses.mulPose(camera.rotation());
        MiniatureImmortalRuinRenderer.drawDisc(poses, buffers, scale, entity.reversed());
        poses.popPose();

        if (!entity.previewEnabled()) return;
        AABB area = new AABB(entity.offsetX(), entity.offsetY(), entity.offsetZ(),
                entity.offsetX() + entity.sizeX(), entity.offsetY() + entity.sizeY(), entity.offsetZ() + entity.sizeZ())
                .inflate(0.002D);
        LevelRenderer.renderLineBox(poses, buffers.getBuffer(RenderType.lines()), area,
                1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override public boolean shouldRenderOffScreen(StabilizedMiniatureImmortalRuinBlockEntity entity) { return true; }
    @Override public int getViewDistance() { return 96; }
}
