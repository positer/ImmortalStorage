package com.immortalstorage.immortalstorage.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

/** Preserve JEI's real item renderer and tooltip, with an exact long input-count corner label. */
final class SpiritInputJeiRenderer implements IIngredientRenderer<ItemStack> {
    private final long count;
    SpiritInputJeiRenderer(long count) { this.count = count; }
    private IIngredientRenderer<ItemStack> vanilla() { return ImmortalStorageJeiPlugin.itemRenderer(); }
    @Override public void render(GuiGraphicsExtractor graphics, ItemStack stack) {
        vanilla().render(graphics, stack.copyWithCount(1));
        if (count > 1) {
            var font = vanilla().getFontRenderer(Minecraft.getInstance(), stack);
            String label = Long.toString(count);
            graphics.text(font, label, 17 - font.width(label), 9, 0xFFFFFFFF, true);
        }
    }
    @Override public List<Component> getTooltip(ItemStack stack, TooltipFlag flag) { return vanilla().getTooltip(stack, flag); }
    @Override public void getTooltip(ITooltipBuilder builder, ItemStack stack, TooltipFlag flag) { vanilla().getTooltip(builder, stack, flag); }
}
