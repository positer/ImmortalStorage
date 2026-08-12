package com.immortalstorage.immortalstorage.compat.mc2612;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Bridges the maintained 1.21.1 immediate renderer body to the official 26.1
 * extraction/submission contract through custom-geometry submission nodes.
 */
public abstract class LegacyBlockEntityRenderer<T extends BlockEntity>
        implements BlockEntityRenderer<T, LegacyBlockEntityRenderer.State<T>> {
    /** Submits canonical immediate-mode geometry through the official 26.1 node collector. */
    public static void submitLegacyGeometry(PoseStack poseStack, SubmitNodeCollector collector,
                                            Consumer<MultiBufferSource> renderer) {
        RecordingMultiBufferSource buffers = new RecordingMultiBufferSource(collector, poseStack);
        renderer.accept(buffers);
        buffers.submit();
    }

    public static final class State<T extends BlockEntity> extends BlockEntityRenderState {
        private T entity;
        private float partialTick;

        private void capture(T entity, float partialTick) {
            this.entity = entity;
            this.partialTick = partialTick;
        }
    }

    @Override
    public State<T> createRenderState() {
        return new State<>();
    }

    @Override
    public void extractRenderState(T entity, State<T> state, float partialTick,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        state.capture(entity, partialTick);
    }

    @Override
    public void submit(State<T> state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (submitOfficial(state.entity, state.partialTick, poseStack, collector, camera)) {
            return;
        }
        RecordingMultiBufferSource buffers = new RecordingMultiBufferSource(collector, poseStack);
        legacyRender(state.entity, state.partialTick, poseStack, buffers,
                state.lightCoords, 0, camera.pos);
        buffers.submit();
    }

    /**
     * Target-only hook for renderers whose output is a real 26.1 block/item
     * model and therefore must be submitted through the official collector.
     * Existing migrated renderers continue through the recording bridge.
     */
    protected boolean submitOfficial(T entity, float partialTick, PoseStack poseStack,
                                     SubmitNodeCollector collector, CameraRenderState camera) {
        return false;
    }

    protected abstract void legacyRender(T entity, float partialTick, PoseStack poseStack,
                                         MultiBufferSource buffers, int light, int overlay,
                                         Vec3 cameraPosition);

    private static final class RecordingMultiBufferSource implements MultiBufferSource {
        private final SubmitNodeCollector collector;
        private final PoseStack poseStack;
        private final Map<RenderType, RecordingVertexConsumer> buffers = new LinkedHashMap<>();

        private RecordingMultiBufferSource(SubmitNodeCollector collector, PoseStack poseStack) {
            this.collector = collector;
            this.poseStack = poseStack;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return buffers.computeIfAbsent(renderType, ignored -> new RecordingVertexConsumer());
        }

        private void submit() {
            for (Map.Entry<RenderType, RecordingVertexConsumer> entry : buffers.entrySet()) {
                if (entry.getValue().commands.isEmpty()) continue;
                collector.submitCustomGeometry(poseStack, entry.getKey(),
                        (ignoredPose, vertices) -> entry.getValue().replay(vertices));
            }
        }
    }

    private static final class RecordingVertexConsumer implements VertexConsumer {
        private final List<Consumer<VertexConsumer>> commands = new ArrayList<>();

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            commands.add(vertices -> vertices.addVertex(x, y, z));
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            commands.add(vertices -> vertices.setColor(red, green, blue, alpha));
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            commands.add(vertices -> vertices.setColor(color));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            commands.add(vertices -> vertices.setUv(u, v));
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            commands.add(vertices -> vertices.setUv1(u, v));
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            commands.add(vertices -> vertices.setUv2(u, v));
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            commands.add(vertices -> vertices.setNormal(x, y, z));
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            commands.add(vertices -> vertices.setLineWidth(width));
            return this;
        }

        private void replay(VertexConsumer vertices) {
            for (Consumer<VertexConsumer> command : commands) command.accept(vertices);
        }
    }
}
