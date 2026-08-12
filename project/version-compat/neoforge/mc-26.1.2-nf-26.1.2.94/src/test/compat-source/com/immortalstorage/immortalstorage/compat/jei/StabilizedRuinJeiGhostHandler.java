package com.immortalstorage.immortalstorage.compat.jei;

import com.immortalstorage.immortalstorage.client.screen.StabilizedMiniatureImmortalRuinScreen;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

final class StabilizedRuinJeiGhostHandler implements IGhostIngredientHandler<StabilizedMiniatureImmortalRuinScreen> {
    @Override public <I> List<Target<I>> getTargetsTyped(StabilizedMiniatureImmortalRuinScreen screen,
                                                         ITypedIngredient<I> ingredient, boolean doStart) {
        ItemStack stack = ingredient.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
        if (stack.isEmpty() || !screen.filtersOpen()) return List.of();
        List<Target<I>> targets = new ArrayList<>();
        for (int slot = 0; slot < 20; slot++) {
            int index = slot;
            targets.add(new Target<>() {
                @Override public net.minecraft.client.renderer.Rect2i getArea() { return screen.filterSlotBounds(index); }
                @Override public void accept(I ignored) { screen.setGhostFilter(index, stack); }
            });
        }
        return targets;
    }
    @Override public void onComplete() {}
}
