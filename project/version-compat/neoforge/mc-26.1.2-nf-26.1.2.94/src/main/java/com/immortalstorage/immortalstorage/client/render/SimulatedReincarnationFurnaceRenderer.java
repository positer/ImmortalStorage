package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.SimulatedReincarnationFurnaceBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/** Official 26.1 extracted entity-state renderer for the rotating source preview. */
public final class SimulatedReincarnationFurnaceRenderer
        implements BlockEntityRenderer<SimulatedReincarnationFurnaceBlockEntity,
        SimulatedReincarnationFurnaceRenderer.State> {
    private final Map<SimulatedReincarnationFurnaceBlockEntity, CachedEntity> cache = new WeakHashMap<>();

    public SimulatedReincarnationFurnaceRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static final class State extends BlockEntityRenderState {
        private EntityRenderState entityState;
        private float partialTick;
        private long gameTime;
    }

    private record CachedEntity(ItemStack source, LivingEntity entity) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SimulatedReincarnationFurnaceBlockEntity furnace, State state,
                                   float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(furnace, state, breakProgress);
        state.partialTick = partialTick;
        if (furnace.getLevel() == null) {
            state.entityState = null;
            state.gameTime = 0L;
            return;
        }
        state.gameTime = furnace.getLevel().getGameTime();
        ItemStack source = furnace.getItem(SimulatedReincarnationFurnaceBlockEntity.SOURCE_SLOT);
        if (source.isEmpty()) {
            cache.remove(furnace);
            state.entityState = null;
            return;
        }
        CachedEntity cached = cache.get(furnace);
        if (cached == null || !ItemStack.isSameItemSameComponents(cached.source(), source)) {
            LivingEntity entity = furnace.createDisplayEntity(furnace.getLevel());
            if (entity == null) {
                cache.remove(furnace);
                state.entityState = null;
                return;
            }
            cached = new CachedEntity(source.copyWithCount(1), entity);
            cache.put(furnace, cached);
        }
        state.entityState = Minecraft.getInstance().getEntityRenderDispatcher()
                .extractEntity(cached.entity(), partialTick);
    }

    @Override
    public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.entityState == null) return;
        float height = Math.max(0.5F, state.entityState.boundingBoxHeight);
        float scale = Math.min(0.38F, 0.62F / height);
        poses.pushPose();
        poses.translate(0.5D, 0.18D, 0.5D);
        poses.mulPose(Axis.YP.rotationDegrees((state.gameTime + state.partialTick) * 2.2F));
        poses.scale(scale, scale, scale);
        Minecraft.getInstance().getEntityRenderDispatcher().submit(
                state.entityState, camera, 0.0D, 0.0D, 0.0D, poses, collector);
        poses.popPose();
    }
}
