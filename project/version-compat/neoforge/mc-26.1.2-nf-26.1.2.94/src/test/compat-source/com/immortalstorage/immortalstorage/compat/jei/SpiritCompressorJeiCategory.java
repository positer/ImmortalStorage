package com.immortalstorage.immortalstorage.compat.jei;

import com.immortalstorage.immortalstorage.compressor.SpiritCompressorRecipes;
import com.immortalstorage.immortalstorage.client.screen.VanillaGuiPainter;
import com.immortalstorage.immortalstorage.item.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Production recipes use the machine's slot geometry; structure has its own category. */
public final class SpiritCompressorJeiCategory implements IRecipeCategory<SpiritCompressorRecipes.Entry> {
    public static final IRecipeType<SpiritCompressorRecipes.Entry> TYPE = IRecipeType.create("immortalstorage", "spirit_compression", SpiritCompressorRecipes.Entry.class);
    public static final IRecipeType<SpiritCompressorRecipes.Entry> DECOMPRESSION_TYPE = IRecipeType.create("immortalstorage", "spirit_decompression", SpiritCompressorRecipes.Entry.class);
    private final boolean reversed;
    private final IDrawable icon;
    public SpiritCompressorJeiCategory(IGuiHelper helper) { this(helper, false); }
    public SpiritCompressorJeiCategory(IGuiHelper helper, boolean reversed) { this.reversed = reversed; icon = helper.createDrawableItemStack(com.immortalstorage.immortalstorage.client.render.SpiritCompressorDisplayItems.controllerStack(reversed)); }
    @Override public IRecipeType<SpiritCompressorRecipes.Entry> getRecipeType() { return reversed ? DECOMPRESSION_TYPE : TYPE; }
    @Override public Component getTitle() { return Component.translatable(reversed ? "container.immortalstorage.spirit_decompressor" : "container.immortalstorage.spirit_compressor"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 64; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, SpiritCompressorRecipes.Entry entry, IFocusGroup focus) {
        for (int i = 0; i < entry.inputs().size(); i++) {
            var input = entry.inputs().get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, reversed ? 26 : 8 + i % 4 * 18,
                    reversed ? 26 : 8 + i / 4 * 18).addIngredients(input.ingredient())
                    .setCustomRenderer(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, new SpiritInputJeiRenderer(input.count()));
        }
        for (int i = 0; i < entry.outputs().size(); i++) builder.addSlot(RecipeIngredientRole.OUTPUT,
                reversed ? 98 + i % 4 * 18 : 134, reversed ? 8 + i / 4 * 18 : 26).addItemStack(entry.outputs().get(i));
    }
    @Override public void draw(SpiritCompressorRecipes.Entry entry, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mx, double my) {
        for (int i = 0; i < 12; i++) VanillaGuiPainter.slot(graphics, (reversed ? 98 : 8) + i % 4 * 18, 8 + i / 4 * 18, true);
        VanillaGuiPainter.slot(graphics, reversed ? 26 : 134, 26, true);
        int x = reversed ? 60 : 94;
        graphics.fill(x, 32, x + 21, 38, 0xFF888888);
        for (int i = 0; i < 9; i++) graphics.fill(x + 21 + i, 27 + i, x + 22 + i, 43 - i, 0xFF888888);
    }
}
