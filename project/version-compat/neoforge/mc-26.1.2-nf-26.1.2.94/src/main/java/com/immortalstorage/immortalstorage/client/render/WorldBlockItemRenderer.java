package com.immortalstorage.immortalstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import java.util.function.Consumer;

/** Official model-state/BER extraction and submission shared with the placed block. */
public final class WorldBlockItemRenderer implements SpecialModelRenderer<ItemStack> {
    public static final WorldBlockItemRenderer INSTANCE = new WorldBlockItemRenderer();
    private WorldBlockItemRenderer() {}

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void submit(ItemStack stack, PoseStack poses, SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int outlineColor) {
        var visual = WorldBlockItemVisual.resolve(stack);
        if (visual == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (visual.state().getRenderShape() == RenderShape.MODEL) {
            var model = minecraft.getModelManager().getBlockModelSet().get(visual.state());
            BlockModelRenderState base = new BlockModelRenderState();
            model.update(base, visual.state(), BlockDisplayContext.create(), 42L);
            base.submit(poses, collector, light, overlay, outlineColor);
        }
        if (visual.entity() == null) return;
        BlockEntityRenderer renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(visual.entity());
        if (renderer == null) return;
        BlockEntityRenderState state = renderer.createRenderState();
        float partialTick = minecraft.level == null ? 0F
                : minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        renderer.extractRenderState(visual.entity(), state, partialTick, Vec3.ZERO, null);
        state.lightCoords = light;
        CameraRenderState camera = new CameraRenderState();
        camera.initialized = true;
        renderer.submit(state, poses, collector, camera);
    }

    @Override public ItemStack extractArgument(ItemStack stack) { return stack; }
    @Override public void getExtents(Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(-0.5F, -0.5F, -0.5F));
        consumer.accept(new Vector3f(0.5F, 0.5F, 0.5F));
    }
    public static final class Unbaked implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());
        @Override public SpecialModelRenderer<ItemStack> bake(SpecialModelRenderer.BakingContext context) { return INSTANCE; }
        @Override public MapCodec<Unbaked> type() { return MAP_CODEC; }
    }
}
