package com.immortalstorage.immortalstorage.compat.mc2612;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;

/** Official 26.1 submission helpers for item special-model compatibility renderers. */
public final class SpecialModelGeometry {
    @FunctionalInterface
    public interface LegacyGeometry {
        void render(MultiBufferSource buffers);
    }

    /** Submits an immediate-style geometry body through the official node collector. */
    public static void submit(SubmitNodeCollector collector, PoseStack poses, LegacyGeometry geometry) {
        RecordingMultiBufferSource buffers = new RecordingMultiBufferSource(collector, poses);
        geometry.render(buffers);
        buffers.submit();
    }

    /** Submits the block-state model that is the base of a block item. */
    public static void submitBlockBase(ItemStack stack, PoseStack poses,
                                       SubmitNodeCollector collector, int light,
                                       int overlay, int outlineColor) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        BlockState state = blockItem.getBlock().defaultBlockState();
        Minecraft minecraft = Minecraft.getInstance();
        BlockModel model = minecraft.getModelManager().getBlockModelSet().get(state);
        BlockModelRenderState renderState = new BlockModelRenderState();
        model.update(renderState, state, BlockDisplayContext.create(), 42L);
        renderState.submit(poses, collector, light, overlay, outlineColor);
    }

    /** Submits a nested item model using the target ItemModelResolver API. */
    public static void submitNestedItem(ItemStack stack, float x, float y, float z,
                                        float scale, PoseStack parent,
                                        SubmitNodeCollector collector, int light,
                                        int overlay, int outlineColor) {
        if (stack.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        ItemStackRenderState renderState = new ItemStackRenderState();
        minecraft.getItemModelResolver().appendItemLayers(renderState, stack,
                ItemDisplayContext.NONE, minecraft.level, null, 0);
        parent.pushPose();
        parent.translate(x, y, z);
        parent.scale(scale, scale, scale);
        renderState.submit(parent, collector, light, overlay, outlineColor);
        parent.popPose();
    }

    /** Returns the resolved item model bounds in the same context used by submitNestedItem. */
    public static AABB itemModelBounds(ItemStack stack) {
        if (stack.isEmpty()) return new AABB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        Minecraft minecraft = Minecraft.getInstance();
        ItemStackRenderState renderState = new ItemStackRenderState();
        minecraft.getItemModelResolver().appendItemLayers(renderState, stack,
                ItemDisplayContext.NONE, minecraft.level, null, 0);
        return renderState.getModelBoundingBox();
    }

    private static final class RecordingMultiBufferSource implements MultiBufferSource {
        private final SubmitNodeCollector collector;
        private final PoseStack poses;
        private final Map<net.minecraft.client.renderer.rendertype.RenderType, RecordingVertexConsumer> buffers =
                new LinkedHashMap<>();

        private RecordingMultiBufferSource(SubmitNodeCollector collector, PoseStack poses) {
            this.collector = collector;
            this.poses = poses;
        }

        @Override
        public VertexConsumer getBuffer(net.minecraft.client.renderer.rendertype.RenderType renderType) {
            return buffers.computeIfAbsent(renderType, ignored -> new RecordingVertexConsumer());
        }

        private void submit() {
            for (Map.Entry<net.minecraft.client.renderer.rendertype.RenderType, RecordingVertexConsumer> entry
                    : buffers.entrySet()) {
                if (entry.getValue().commands.isEmpty()) continue;
                collector.submitCustomGeometry(poses, entry.getKey(),
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

    private SpecialModelGeometry() {}
}
