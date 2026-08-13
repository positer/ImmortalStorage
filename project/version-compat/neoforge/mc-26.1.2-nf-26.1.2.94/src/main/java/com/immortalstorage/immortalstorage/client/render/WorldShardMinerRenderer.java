package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.WorldShardMinerBlockEntity;
import com.immortalstorage.immortalstorage.compat.mc2612.LegacyBlockEntityRenderer;
import com.immortalstorage.immortalstorage.worldshard.WorldShardBeamPath;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerAppearance;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Official 26.1 extraction/submission renderer for the miner's glass and beam. */
public final class WorldShardMinerRenderer
        implements BlockEntityRenderer<WorldShardMinerBlockEntity, WorldShardMinerRenderer.State> {
    private static final float CORE_CENTER_Y = 0.53F;
    private static final float CORE_SIZE = 0.58F;
    private static final int CORE_ALPHA = 184;

    private final BlockModelResolver blockModelResolver;

    public WorldShardMinerRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    public static final class State extends BlockEntityRenderState {
        private BlockState glass;
        private boolean active;
        private int coreColor;
        private int beamHeight;
        private long gameTime;
        private float partialTick;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WorldShardMinerBlockEntity miner, State state, float partialTick,
                                   Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(miner, state, breakProgress);
        state.glass = WorldShardMinerAppearance.glassCover();
        state.active = miner.hasActiveBeam();
        state.coreColor = WorldShardMinerAppearance.coreColor(state.active, miner.getBeamColor());
        state.partialTick = partialTick;
        if (miner.getLevel() == null) {
            state.gameTime = 0L;
            state.beamHeight = WorldShardBeamPath.OPEN_SKY_RENDER_HEIGHT;
        } else {
            state.gameTime = miner.getLevel().getGameTime();
            state.beamHeight = WorldShardBeamPath.renderHeight(miner.getLevel(), miner.getBlockPos());
        }
    }

    @Override
    public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        BlockModelRenderState glassModel = new BlockModelRenderState();
        blockModelResolver.update(glassModel, state.glass, BlockDisplayContext.create());
        // The final argument is an outline colour, not a random/model seed.
        // Passing blockPos here tinted clear glass according to the low bits
        // of the position (most visibly green above grass).
        // Glass must retain its translucent model layer. The basic submit path
        // can flatten a block-display model onto the wrong layer in 26.1,
        // causing the red active core behind it to colour the whole cover.
        glassModel.submitMultiLayer(poses, collector, state.lightCoords,
                OverlayTexture.NO_OVERLAY, 0);

        LegacyBlockEntityRenderer.submitLegacyGeometry(poses, collector, buffers ->
                FloatingCubeRenderer.render(poses, buffers, 0.0F, CORE_CENTER_Y, CORE_SIZE,
                        0.0F, 0.0F, 0.0F, 0.0F, state.coreColor, CORE_ALPHA));

        if (state.active) {
            BeaconRenderer.submitBeaconBeam(poses, collector, BeaconRenderer.BEAM_LOCATION,
                    1.0F, (float) Math.floorMod(state.gameTime, 40L) + state.partialTick,
                    0, state.beamHeight,
                    WorldShardMinerAppearance.beamColor(), 0.2F, 0.25F);
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
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
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D,
                pos.getY() + height, pos.getZ() + 1.0D);
    }
}
