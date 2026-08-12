package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.custom.SourceVeinBlock;
import com.immortalstorage.immortalstorage.item.ModDataComponents;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinition;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinitions;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Adds slot-local dynamic previews at the common GuiGraphicsExtractor item-render boundary. */
public final class GuiItemPreviewOverlay {
    private static boolean renderingOverlay;

    public static void render(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (renderingOverlay || stack.isEmpty()) return;
        renderingOverlay = true;
        try {
            
            
            graphics.enableScissor(x, y, x + 16, y + 16);
            if (stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof SourceVeinBlock source) {
                renderSource(graphics, stack, source, x, y);
            } else if (stack.is(ModBlocks.STABILIZED_MINIATURE_IMMORTAL_RUIN.get().asItem())) {
                renderRuin(graphics, stack, x, y);
            } else if (stack.is(ModBlocks.XIANQIAO_MANAGER.get().asItem())) {
                renderManager(graphics, x, y);
            }
            
            graphics.disableScissor();
        } finally {
            
            renderingOverlay = false;
        }
    }

    private static void renderSource(GuiGraphicsExtractor graphics, ItemStack stack, SourceVeinBlock block, int x, int y) {
        Identifier definitionId = block.isGenericDefinitionCarrier()
                ? stack.get(ModDataComponents.SOURCE_DEFINITION_ID.get())
                : SourceDefinitions.legacyId(block.getKind());
        SourceDefinition definition = SourceDefinitions.find(definitionId).orElse(null);
        ItemStack output = outputStack(definition);
        if (output.isEmpty()) return;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 9.0F, y + 9.0F);
        graphics.pose().scale(0.48F, 0.48F);
        graphics.item(output, 0, 0);
        graphics.pose().popMatrix();
    }

    private static void renderRuin(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        float ticks = ticks();
        int radius = Mth.sin(ticks * 0.12F) > 0.0F ? 4 : 3;
        boolean reversed = reversed(stack);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, 0.0F);
        drawDisc(graphics, x + 8, y + 8, radius, reversed ? 0x96000000 : 0x96FFFFFF);
        drawDisc(graphics, x + 8, y + 8, Math.max(2, radius - 1), reversed ? 0xFFFFFFFF : 0xFF000000);
        graphics.pose().popMatrix();
    }

    private static void renderManager(GuiGraphicsExtractor graphics, int x, int y) {
        float ticks = ticks();
        int centerX = x + 8 + (Mth.sin(ticks * 0.04F) > 0.45F ? 1 : Mth.sin(ticks * 0.04F) < -0.45F ? -1 : 0);
        int centerY = y + 8 + (Mth.sin(ticks * 0.075F) > 0.35F ? -1 : Mth.sin(ticks * 0.075F) < -0.35F ? 1 : 0);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, 0.0F);
        graphics.fill(centerX - 3, centerY - 2, centerX + 3, centerY + 2, 0x68F7FBFF);
        graphics.fill(centerX - 2, centerY - 3, centerX + 2, centerY + 3, 0x68F7FBFF);
        graphics.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, 0xFFF7FBFF);
        graphics.pose().popMatrix();
    }

    private static ItemStack outputStack(SourceDefinition definition) {
        if (definition == null) return ItemStack.EMPTY;
        if (definition.fluid()) {
            var fluid = BuiltInRegistries.FLUID.get(definition.outputId()).map(net.minecraft.core.Holder.Reference::value).orElse(null);
            return fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY
                    ? ItemStack.EMPTY : FluidUtil.getFilledBucket(new FluidStack(fluid, 1_000));
        }
        var item = BuiltInRegistries.ITEM.get(definition.outputId()).map(net.minecraft.core.Holder.Reference::value).orElse(null);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static float ticks() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? (System.currentTimeMillis() % 100_000L) / 50.0F
                : minecraft.level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
    }

    private static boolean reversed(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        return tag.getBooleanOr("Reversed", false);
    }

    private static void drawDisc(GuiGraphicsExtractor graphics, int centerX, int centerY, int radius, int color) {
        int radiusSquared = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            int width = Mth.floor(Math.sqrt(Math.max(0, radiusSquared - dy * dy)));
            graphics.fill(centerX - width, centerY + dy, centerX + width + 1, centerY + dy + 1, color);
        }
    }

    private GuiItemPreviewOverlay() {}
}
