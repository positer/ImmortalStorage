package com.immortalstorage.immortalstorage.compat.emi;

import com.immortalstorage.immortalstorage.client.screen.StabilizedMiniatureImmortalRuinScreen;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class StabilizedRuinEmiGhostHandler extends EmiDragDropHandler.BoundsBased<StabilizedMiniatureImmortalRuinScreen> {
    StabilizedRuinEmiGhostHandler() { super(StabilizedRuinEmiGhostHandler::collect); }
    private static void collect(StabilizedMiniatureImmortalRuinScreen screen,
                                BiConsumer<Bounds, Consumer<EmiIngredient>> consumer) {
        if (!screen.filtersOpen()) return;
        for (int slot = 0; slot < 20; slot++) {
            int index = slot; var area = screen.filterSlotBounds(slot);
            consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()), ingredient -> {
                if (ingredient == null || ingredient.isEmpty()) return;
                for (EmiStack stack : ingredient.getEmiStacks()) if (!stack.getItemStack().isEmpty()) {
                    screen.setGhostFilter(index, stack.getItemStack()); return;
                }
            });
        }
    }
}
