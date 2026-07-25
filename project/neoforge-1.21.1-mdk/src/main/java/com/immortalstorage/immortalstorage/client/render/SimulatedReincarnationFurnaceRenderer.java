package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.SimulatedReincarnationFurnaceBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/** Rotating source preview is driven solely by source-slot contents. */
public final class SimulatedReincarnationFurnaceRenderer
        implements BlockEntityRenderer<SimulatedReincarnationFurnaceBlockEntity> {
    private final Map<SimulatedReincarnationFurnaceBlockEntity, CachedEntity> cache = new WeakHashMap<>();

    public SimulatedReincarnationFurnaceRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(SimulatedReincarnationFurnaceBlockEntity furnace, float partialTick,
                                 PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        if (furnace.getLevel() == null) return;
        ItemStack source = furnace.getItem(SimulatedReincarnationFurnaceBlockEntity.SOURCE_SLOT);
        if (source.isEmpty()) { cache.remove(furnace); return; }
        CachedEntity cached = cache.get(furnace);
        if (cached == null || !ItemStack.isSameItemSameComponents(cached.source(), source)) {
            LivingEntity entity = furnace.createDisplayEntity(furnace.getLevel());
            if (entity == null) { cache.remove(furnace); return; }
            cached = new CachedEntity(source.copyWithCount(1), entity);
            cache.put(furnace, cached);
        }
        LivingEntity entity = cached.entity();
        float scale = Math.min(0.38F, 0.62F / Math.max(0.5F, entity.getBbHeight()));
        float time = furnace.getLevel().getGameTime() + partialTick;
        poses.pushPose();
        poses.translate(0.5D, 0.18D, 0.5D);
        poses.mulPose(Axis.YP.rotationDegrees(time * 2.2F));
        poses.scale(scale, scale, scale);
        Minecraft.getInstance().getEntityRenderDispatcher().render(entity, 0.0D, 0.0D, 0.0D,
                0.0F, partialTick, poses, buffers, 0x00F000F0);
        poses.popPose();
    }

    private record CachedEntity(ItemStack source, LivingEntity entity) {}
}
