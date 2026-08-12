package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.custom.SourceVeinBlock;
import com.immortalstorage.immortalstorage.item.ModDataComponents;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinition;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinitions;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Slot-local source identity rendered after the base item and vanilla decorations. */
public final class SourceVeinOutputDecorator implements IItemDecorator {
    public static final SourceVeinOutputDecorator INSTANCE = new SourceVeinOutputDecorator();

    @Override
    public boolean render(GuiGraphicsExtractor graphics, Font font, ItemStack source, int x, int y) {
        ItemStack output = outputStack(definition(source));
        if (output.isEmpty()) return false;
        var pose = graphics.pose();
        pose.pushMatrix();
        // The caller owns clipping. Scissor coordinates are screen-space and
        // do not follow the slot's current PoseStack translation.
        pose.translate(x + 10.0F, y + 10.0F);
        pose.scale(0.5F, 0.5F);
        graphics.item(output, 0, 0);
        pose.popMatrix();
        return false;
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

    private static SourceDefinition definition(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof SourceVeinBlock block)) return null;
        Identifier id = block.isGenericDefinitionCarrier()
                ? stack.get(ModDataComponents.SOURCE_DEFINITION_ID.get())
                : SourceDefinitions.legacyId(block.getKind());
        return SourceDefinitions.find(id).orElse(null);
    }

    private SourceVeinOutputDecorator() {}
}
