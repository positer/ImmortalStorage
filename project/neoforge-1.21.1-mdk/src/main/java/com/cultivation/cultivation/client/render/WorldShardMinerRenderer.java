package com.cultivation.cultivation.client.render;

import com.cultivation.cultivation.block.entity.WorldShardMinerBlockEntity;
import com.cultivation.cultivation.worldshard.WorldShardBeamPath;
import com.cultivation.cultivation.worldshard.WorldShardMinerAppearance;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WorldShardMinerRenderer implements BlockEntityRenderer<WorldShardMinerBlockEntity> {
    private static final float CORE_CENTER_Y = 0.53F;
    private static final float CORE_SIZE = 0.58F;
    private static final int CORE_ALPHA = 184;

    private final BlockRenderDispatcher blockRenderer;

    public WorldShardMinerRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(WorldShardMinerBlockEntity miner, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        // Use Minecraft's live glass block model and texture. The enclosure is
        // intentionally never tinted; only the full-bright core changes color.
        poseStack.pushPose();
        this.blockRenderer.renderSingleBlock(WorldShardMinerAppearance.glassCover(),
                poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        FloatingCubeRenderer.render(
                poseStack, buffers, 0.0F,
                CORE_CENTER_Y, CORE_SIZE,
                0.0F, 0.0F,
                0.0F, 0.0F,
                WorldShardMinerAppearance.coreColor(miner.hasActiveBeam(), miner.getBeamColor()),
                CORE_ALPHA);

        if (!miner.hasActiveBeam() || miner.getLevel() == null) return;
        BeaconRenderer.renderBeaconBeam(poseStack, buffers, BeaconRenderer.BEAM_LOCATION,
                partialTick, 1.0F, miner.getLevel().getGameTime(), 0,
                WorldShardBeamPath.renderHeight(miner.getLevel(), miner.getBlockPos()),
                WorldShardMinerAppearance.beamColor(), 0.2F, 0.25F);
    }

    @Override
    public boolean shouldRenderOffScreen(WorldShardMinerBlockEntity miner) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(WorldShardMinerBlockEntity miner, Vec3 cameraPosition) {
        return Vec3.atCenterOf(miner.getBlockPos()).multiply(1.0D, 0.0D, 1.0D)
                .closerThan(cameraPosition.multiply(1.0D, 0.0D, 1.0D), getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(WorldShardMinerBlockEntity miner) {
        var pos = miner.getBlockPos();
        int height = miner.getLevel() == null
                ? WorldShardBeamPath.OPEN_SKY_RENDER_HEIGHT
                : WorldShardBeamPath.renderHeight(miner.getLevel(), pos);
        return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + height, pos.getZ() + 1.0D);
    }
}
