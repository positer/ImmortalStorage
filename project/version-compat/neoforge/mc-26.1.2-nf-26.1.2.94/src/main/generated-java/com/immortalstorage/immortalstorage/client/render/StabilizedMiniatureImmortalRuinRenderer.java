package com.immortalstorage.immortalstorage.client.render;
import com.immortalstorage.immortalstorage.compat.mc2612.LegacyBlockEntityRenderer;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatRenderTypes;

import com.immortalstorage.immortalstorage.block.entity.StabilizedMiniatureImmortalRuinBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Internal ruin sphere plus optional white selection outline. */
public final class StabilizedMiniatureImmortalRuinRenderer extends LegacyBlockEntityRenderer<StabilizedMiniatureImmortalRuinBlockEntity> {
    public StabilizedMiniatureImmortalRuinRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void legacyRender(StabilizedMiniatureImmortalRuinBlockEntity entity, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay, net.minecraft.world.phys.Vec3 cameraPosition) {
        float time = (entity.getLevel() == null ? 0 : entity.getLevel().getGameTime()) + partialTick;
        float scale = 0.27F + Mth.sin(time * 0.12F) * 0.03F;
        poses.pushPose();
        poses.translate(0.5F, 0.5F, 0.5F);
        poses.mulPose(com.mojang.math.Axis.YP.rotation(time * 0.018F));
        poses.mulPose(com.mojang.math.Axis.XP.rotation(time * 0.011F));
        MiniatureImmortalRuinRenderer.drawDisc(poses, buffers, scale, entity.reversed());
        poses.popPose();
        RuinLinkRenderer.render(poses, buffers, entity.getBlockPos(), entity.linkedPos());

        if (!entity.previewEnabled()) return;
        AABB area = new AABB(entity.offsetX(), entity.offsetY(), entity.offsetZ(),
                entity.offsetX() + entity.sizeX(), entity.offsetY() + entity.sizeY(), entity.offsetZ() + entity.sizeZ())
                .inflate(0.002D);
        com.immortalstorage.immortalstorage.compat.mc2612.CompatRender.renderLineBox(poses, buffers.getBuffer(CompatRenderTypes.lines()), area,
                1.0F, 1.0F, 1.0F, 1.0F);
        RuinFaceHighlightRenderer.render(poses, buffers, area, entity.faceMask());
    }

    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 96; }
}
