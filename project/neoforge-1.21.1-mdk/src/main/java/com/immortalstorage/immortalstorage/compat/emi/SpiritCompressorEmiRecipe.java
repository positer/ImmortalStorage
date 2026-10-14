package com.immortalstorage.immortalstorage.compat.emi;

import com.immortalstorage.immortalstorage.compressor.SpiritCompressorRecipes;
import com.immortalstorage.immortalstorage.item.ModItems;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public final class SpiritCompressorEmiRecipe implements EmiRecipe {
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(ResourceLocation.parse("immortalstorage:spirit_compression"), EmiStack.of(ModItems.MINIATURE_IMMORTAL_RUIN.get()));
    public static final EmiRecipeCategory DECOMPRESSION_CATEGORY = new EmiRecipeCategory(ResourceLocation.parse("immortalstorage:spirit_decompression"), EmiStack.of(com.immortalstorage.immortalstorage.client.render.SpiritCompressorDisplayItems.controllerStack(true)));
    private final SpiritCompressorRecipes.Entry entry;
    public SpiritCompressorEmiRecipe(SpiritCompressorRecipes.Entry entry) { this.entry = entry; }
    @Override public EmiRecipeCategory getCategory() { return entry.reversed() ? DECOMPRESSION_CATEGORY : CATEGORY; }
    @Override public ResourceLocation getId() { return entry.id(); }
    @Override public List<EmiIngredient> getInputs() { return entry.inputs().stream().map(input -> EmiIngredient.of(input.ingredient(), input.count())).toList(); }
    @Override public List<EmiStack> getOutputs() { return entry.outputs().stream().map(EmiStack::of).toList(); }
    @Override public int getDisplayWidth() { return 176; }
    @Override public int getDisplayHeight() { return 64; }
    @Override public void addWidgets(WidgetHolder widgets) {
        boolean inverse = entry.reversed();
        widgets.addDrawable(0, 0, 176, 64, (graphics, mx, my, tick) -> {
            for (int i = 0; i < 12; i++) com.immortalstorage.immortalstorage.client.screen.VanillaGuiPainter.slot(graphics,
                    (inverse ? 98 : 8) + i % 4 * 18, 8 + i / 4 * 18, true);
            com.immortalstorage.immortalstorage.client.screen.VanillaGuiPainter.slot(graphics, inverse ? 26 : 134, 26, true);
        });
        for (int i = 0; i < getInputs().size(); i++) widgets.addSlot(getInputs().get(i), inverse ? 25 : 7 + i % 4 * 18, inverse ? 25 : 7 + i / 4 * 18).drawBack(false);
        widgets.addFillingArrow(inverse ? 60 : 94, 26, 20);
        for (int i = 0; i < getOutputs().size(); i++) widgets.addSlot(getOutputs().get(i),
                inverse ? 97 + i % 4 * 18 : 133, inverse ? 7 + i / 4 * 18 : 25).drawBack(false).recipeContext(this);
    }
}
