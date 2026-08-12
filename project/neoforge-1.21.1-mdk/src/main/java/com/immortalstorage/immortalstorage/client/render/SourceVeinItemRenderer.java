package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.custom.SourceVeinBlock;
import com.immortalstorage.immortalstorage.item.ModDataComponents;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinition;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinitions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the source base model and output identity outside slot-based GUI viewers. */
public final class SourceVeinItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final SourceVeinItemRenderer INSTANCE = new SourceVeinItemRenderer();
    private final SourceVeinAnimation.Clock animationClock = new SourceVeinAnimation.Clock();

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

        SourceDefinition definition = definition(source);
        if (definition == null) return;
        double logicalTime = minecraft.level == null
                ? SourceVeinAnimation.realTime()
                : SourceVeinAnimation.continuousTime(
                        minecraft.level.getGameTime(), minecraft.getTimer().getGameTimeDeltaPartialTick(true));
        double animation = animationClock.sample(logicalTime);
        SourceVeinDisplayRenderer.renderForItem(definition, animation,
                poseStack, buffers, packedLight, packedOverlay,
                orientationSeed(source, definition));
    }

    private static ResourceLocation baseModel(ItemStack source) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(source.getItem());
        return ResourceLocation.fromNamespaceAndPath(
                itemId.getNamespace(), "item/" + itemId.getPath() + "_base");
    }

    private static SourceDefinition definition(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof SourceVeinBlock block)) return null;
        ResourceLocation id = block.isGenericDefinitionCarrier()
                ? stack.get(ModDataComponents.SOURCE_DEFINITION_ID.get())
                : SourceDefinitions.legacyId(block.getKind());
        return SourceDefinitions.find(id).orElse(null);
    }

    private static long orientationSeed(ItemStack source, SourceDefinition definition) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(source.getItem());
        return 31L * itemId.hashCode() + definition.outputId().hashCode();
    }

}
