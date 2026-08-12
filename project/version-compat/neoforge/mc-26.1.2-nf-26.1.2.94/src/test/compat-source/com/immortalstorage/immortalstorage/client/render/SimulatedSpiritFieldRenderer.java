package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.SimulatedSpiritFieldBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Official 26.1 model-state renderer for the simulated field substrate/crop. */
public final class SimulatedSpiritFieldRenderer
        implements BlockEntityRenderer<SimulatedSpiritFieldBlockEntity, SimulatedSpiritFieldRenderer.State> {
    private final BlockModelResolver blockModelResolver;

    public SimulatedSpiritFieldRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    public static final class State extends BlockEntityRenderState {
        private BlockState substrate;
        private BlockState crop;
        private boolean growingChorus;
        private int progress;
        private float partialTick;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SimulatedSpiritFieldBlockEntity field, State state, float partialTick,
                                   Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(field, state, breakProgress);
        state.substrate = field.substrate();
        state.crop = field.displayCrop().orElse(null);
        state.growingChorus = field.displaysGrowingChorusFlower();
        state.progress = field.progress();
        state.partialTick = partialTick;
    }

    @Override
    public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        BlockModelRenderState substrate = new BlockModelRenderState();
        blockModelResolver.update(substrate, state.substrate, BlockDisplayContext.create());
        poses.pushPose();
        poses.translate(0.125D, 0.06D, 0.125D);
        poses.scale(0.75F, 0.12F, 0.75F);
        substrate.submit(poses, collector, state.lightCoords, 0, 0);
        poses.popPose();

        if (state.crop == null || state.crop.getRenderShape() == RenderShape.INVISIBLE) return;
        float cropScale = state.growingChorus
                ? 0.15F + 0.55F * Math.min(1.0F, (state.progress + state.partialTick) / 50.0F)
                : 0.50F;
        BlockModelRenderState crop = new BlockModelRenderState();
        blockModelResolver.update(crop, state.crop, BlockDisplayContext.create());
        poses.pushPose();
        poses.translate(0.5D - cropScale / 2.0D, 0.12D, 0.5D - cropScale / 2.0D);
        poses.scale(cropScale, cropScale, cropScale);
        crop.submit(poses, collector, state.lightCoords, 0, 0);
        poses.popPose();
    }
}
