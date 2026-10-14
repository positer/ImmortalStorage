package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.custom.SourceVeinBlock;
import com.immortalstorage.immortalstorage.item.ModDataComponents;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinition;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinitions;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Adds slot-local dynamic previews at the common GuiGraphics item-render boundary. */
public final class GuiItemPreviewOverlay {
    private static boolean renderingOverlay;

    public static void render(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (renderingOverlay || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof BlockItem item && item.getBlock() instanceof SourceVeinBlock)) return;
        renderingOverlay = true;
        try {
            graphics.flush();
            RenderSystem.disableDepthTest();
            graphics.enableScissor(x, y, x + 16, y + 16);
            if (stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof SourceVeinBlock source) {
                renderSource(graphics, stack, source, x, y);
            }
            graphics.flush();
            graphics.disableScissor();
        } finally {
            RenderSystem.enableDepthTest();
            renderingOverlay = false;
        }
    }

    private static void renderSource(GuiGraphics graphics, ItemStack stack, SourceVeinBlock block, int x, int y) {
        ResourceLocation definitionId = block.isGenericDefinitionCarrier()
                ? stack.get(ModDataComponents.SOURCE_DEFINITION_ID.get())
                : SourceDefinitions.legacyId(block.getKind());
        SourceDefinition definition = SourceDefinitions.find(definitionId).orElse(null);
        ItemStack output = outputStack(definition);
        if (output.isEmpty()) return;
        graphics.pose().pushPose();
        graphics.pose().translate(x + 9.0F, y + 9.0F, 180.0F);
        graphics.pose().scale(0.48F, 0.48F, 0.48F);
        graphics.renderItem(output, 0, 0);
        graphics.pose().popPose();
    }

    private static ItemStack outputStack(SourceDefinition definition) {
        if (definition == null) return ItemStack.EMPTY;
        if (definition.fluid()) {
            var fluid = BuiltInRegistries.FLUID.get(definition.outputId());
            return fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY
                    ? ItemStack.EMPTY : FluidUtil.getFilledBucket(new FluidStack(fluid, 1_000));
        }
        var item = BuiltInRegistries.ITEM.get(definition.outputId());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private GuiItemPreviewOverlay() {}
}
