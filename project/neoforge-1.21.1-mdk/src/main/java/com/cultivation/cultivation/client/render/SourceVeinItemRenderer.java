package com.cultivation.cultivation.client.render;

import com.cultivation.cultivation.block.custom.SourceVeinBlock;
import com.cultivation.cultivation.item.ModDataComponents;
import com.cultivation.cultivation.source.definition.SourceDefinition;
import com.cultivation.cultivation.source.definition.SourceDefinitions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Draws the 0.0.1 source model and a compact output identity in one item-render call. */
public final class SourceVeinItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final SourceVeinItemRenderer INSTANCE = new SourceVeinItemRenderer();

    private SourceVeinItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(
            ItemStack source, ItemDisplayContext context, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel base = minecraft.getModelManager().getModel(
                ModelResourceLocation.standalone(baseModel(source)));

        // ItemRenderer has already applied the GUI transform and -0.5 centering
        // before entering a BEWLR. Applying either again makes the base disappear.
        var baseConsumer = ItemRenderer.getFoilBufferDirect(
                buffers, ItemBlockRenderTypes.getRenderType(source, true), true, source.hasFoil());
        minecraft.getItemRenderer().renderModelLists(
                base, source, packedLight, packedOverlay, poseStack, baseConsumer);

        ItemStack output = outputStack(definition(source));
        if (output.isEmpty()) return;
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            // BEWLR item space is centered on the slot. Keep the whole badge
            // inside the lower-right quadrant instead of translating past it.
            poseStack.translate(0.27F, 0.27F, 1.25F);
            poseStack.scale(0.34F, 0.34F, 0.34F);
        } else {
            poseStack.translate(0.58F, 0.58F, 0.58F);
            poseStack.scale(0.32F, 0.32F, 0.32F);
        }
        BakedModel outputModel = minecraft.getItemRenderer().getModel(
                output, minecraft.level, null, 0);
        minecraft.getItemRenderer().render(
                output, ItemDisplayContext.NONE, false, poseStack, buffers, packedLight,
                OverlayTexture.NO_OVERLAY, outputModel);
        poseStack.popPose();
    }

    private static ResourceLocation baseModel(ItemStack source) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(source.getItem());
        return ResourceLocation.fromNamespaceAndPath(
                itemId.getNamespace(), "item/" + itemId.getPath() + "_base");
    }

    private static ItemStack outputStack(SourceDefinition definition) {
        if (definition == null) return ItemStack.EMPTY;
        if (definition.fluid()) {
            var fluid = BuiltInRegistries.FLUID.get(definition.outputId());
            if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) {
                return ItemStack.EMPTY;
            }
            return FluidUtil.getFilledBucket(new FluidStack(fluid, 1_000));
        }
        var item = BuiltInRegistries.ITEM.get(definition.outputId());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static SourceDefinition definition(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof SourceVeinBlock block)) return null;
        ResourceLocation id = block.isGenericDefinitionCarrier()
                ? stack.get(ModDataComponents.SOURCE_DEFINITION_ID.get())
                : SourceDefinitions.legacyId(block.getKind());
        return SourceDefinitions.find(id).orElse(null);
    }
}
