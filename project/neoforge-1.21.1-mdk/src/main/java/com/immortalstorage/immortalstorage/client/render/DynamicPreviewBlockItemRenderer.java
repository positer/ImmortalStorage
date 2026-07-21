package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Viewer-independent animated previews for open-frame ImmortalStorage blocks. */
public final class DynamicPreviewBlockItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final DynamicPreviewBlockItemRenderer INSTANCE = new DynamicPreviewBlockItemRenderer();

    private DynamicPreviewBlockItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        BakedModel base = minecraft.getModelManager().getModel(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath())));
        var baseConsumer = ItemRenderer.getFoilBufferDirect(
                buffers, ItemBlockRenderTypes.getRenderType(stack, true), true, stack.hasFoil());
        minecraft.getItemRenderer().renderModelLists(
                base, stack, packedLight, packedOverlay, poses, baseConsumer);

        float ticks = minecraft.level == null
                ? (System.currentTimeMillis() % 100_000L) / 50.0F
                : minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        if (stack.is(ModBlocks.STABILIZED_MINIATURE_IMMORTAL_RUIN.get().asItem())) {
            poses.pushPose();
            poses.translate(0.5F, 0.5F, 0.72F);
            float radius = 0.24F + Mth.sin(ticks * 0.12F) * 0.025F;
            MiniatureImmortalRuinRenderer.drawDisc(poses, buffers, radius, reversed(stack));
            poses.popPose();
        } else if (stack.is(ModBlocks.XIANQIAO_MANAGER.get().asItem())) {
            FloatingCubeRenderer.render(poses, buffers, ticks,
                    0.5F, 0.34F, 0.035F, 0.075F, 0.72F, 0.43F,
                    0xF7FBFF, 104);
        }
    }

    private static boolean reversed(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        return tag.getBoolean("Reversed");
    }
}
