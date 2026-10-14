package com.immortalstorage.immortalstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;

/** Native block model plus its registered world block-entity renderer, in every item context. */
public final class WorldBlockItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final WorldBlockItemRenderer INSTANCE = new WorldBlockItemRenderer();
    private WorldBlockItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses,
                             MultiBufferSource buffers, int light, int overlay) {
        var visual = WorldBlockItemVisual.resolve(stack);
        if (visual == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (visual.state().getRenderShape() == RenderShape.MODEL) {
            // MODEL states cannot re-enter this item's builtin/entity renderer.
            minecraft.getBlockRenderer().renderSingleBlock(visual.state(), poses, buffers, light, overlay);
        }
        if (visual.entity() == null) return;
        BlockEntityRenderer renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(visual.entity());
        if (renderer != null) {
            float partialTick = minecraft.level == null ? 0F : minecraft.getTimer().getGameTimeDeltaPartialTick(true);
            renderer.render(visual.entity(), partialTick, poses, buffers, light, overlay);
        }
    }
}
