package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.AdvancedXianqiaoInterfaceBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/**
 * Advanced Xianqiao Interface: optional selection box + per-face highlights.
 * Interaction faces keep the white translucent quad; PULL faces get an extra
 * green translucent quad and PUSH faces an extra red translucent quad.
 */
public final class AdvancedXianqiaoInterfaceRenderer
        implements BlockEntityRenderer<AdvancedXianqiaoInterfaceBlockEntity> {
    public AdvancedXianqiaoInterfaceRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(AdvancedXianqiaoInterfaceBlockEntity entity, float partialTick,
                       PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        if (!entity.previewEnabled()) return;
        AABB area = new AABB(entity.offsetX(), entity.offsetY(), entity.offsetZ(),
                entity.offsetX() + entity.sizeX(), entity.offsetY() + entity.sizeY(),
                entity.offsetZ() + entity.sizeZ()).inflate(0.002D);
        LevelRenderer.renderLineBox(poses, buffers.getBuffer(RenderType.lines()), area,
                1.0F, 1.0F, 1.0F, 1.0F);
        int[] faceModes = new int[Direction.values().length];
        for (Direction face : Direction.values()) {
            faceModes[face.ordinal()] = switch (entity.getSideMode(face)) {
                case PULL -> 1;
                case PUSH -> 2;
                case DISABLED -> 0;
            };
        }
        RuinFaceHighlightRenderer.render(poses, buffers, area, faceModes);
    }

    @Override public boolean shouldRenderOffScreen(AdvancedXianqiaoInterfaceBlockEntity entity) { return true; }
    @Override public int getViewDistance() { return 96; }
}
