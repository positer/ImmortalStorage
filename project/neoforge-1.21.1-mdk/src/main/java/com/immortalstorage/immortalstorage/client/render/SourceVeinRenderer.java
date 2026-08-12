package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import java.util.HashMap;
import java.util.Map;

/** Renders the definition-driven floating output for every source-vein variant. */
public final class SourceVeinRenderer implements BlockEntityRenderer<SourceVeinBlockEntity> {
    private final Map<Long, SourceVeinAnimation.Clock> animationClocks = new HashMap<>();

    public SourceVeinRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SourceVeinBlockEntity blockEntity,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource buffers,
                       int packedLight,
                       int packedOverlay) {
        double logicalTime = blockEntity.getLevel() == null
                ? SourceVeinAnimation.continuousTime(0L, partialTick)
                : SourceVeinAnimation.continuousTime(blockEntity.getLevel().getGameTime(), partialTick);
        double worldTime = clockFor(blockEntity.getBlockPos().asLong()).sample(logicalTime);
        double positionPhase = (blockEntity.getBlockPos().asLong() & 0xFFL) * 0.125D;

        SourceVeinDisplayRenderer.render(blockEntity.definition(), worldTime + positionPhase,
                poseStack, buffers, packedLight, packedOverlay,
                blockEntity.getBlockPos().asLong());
    }

    private SourceVeinAnimation.Clock clockFor(long key) {
        if (animationClocks.size() >= 512 && !animationClocks.containsKey(key)) {
            animationClocks.clear();
        }
        return animationClocks.computeIfAbsent(key, ignored -> new SourceVeinAnimation.Clock());
    }
}
