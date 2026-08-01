package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.SimulatedSpiritFieldBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public final class SimulatedSpiritFieldRenderer implements BlockEntityRenderer<SimulatedSpiritFieldBlockEntity> {
    public SimulatedSpiritFieldRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(SimulatedSpiritFieldBlockEntity field, float partialTick, PoseStack poses,
                                 MultiBufferSource buffers, int light, int overlay) {
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        poses.pushPose();
        poses.translate(0.125D, 0.06D, 0.125D); poses.scale(0.75F, 0.12F, 0.75F);
        dispatcher.renderSingleBlock(field.substrate(), poses, buffers, light, overlay);
        poses.popPose();
        BlockState crop = field.displayCrop().orElse(null);
        if (crop == null || crop.getRenderShape() == RenderShape.INVISIBLE) return;
        float cropScale = field.displaysGrowingChorusFlower()
                ? 0.15F + 0.55F * Math.min(1.0F, (field.progress() + partialTick) / 50.0F)
                : 0.50F;
        poses.pushPose();
        poses.translate(0.5D - cropScale / 2.0D, 0.12D, 0.5D - cropScale / 2.0D);
        poses.scale(cropScale, cropScale, cropScale);
        dispatcher.renderSingleBlock(crop, poses, buffers, light, overlay);
        poses.popPose();
    }
}
