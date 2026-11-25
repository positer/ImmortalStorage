package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.fml.ModList;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

/** Optional sorting UI stays outside our title without changing its filter or persistence behavior. */
final class ConfigurationExternalWidgets {
    private static final String FILTER = "com.nemonotfound.nemos.inventory.sorting.gui.components.FilterBox";
    private static final String PIN = "com.nemonotfound.nemos.inventory.sorting.gui.components.buttons.ToggleFilterPersistenceButton";
    private ConfigurationExternalWidgets() {}

    static void place(List<? extends GuiEventListener> children, int left, int titleY, int width) {
        if (!ModList.get().isLoaded("nemos_inventory_sorting")) return;
        AbstractWidget filter = null, pin = null;
        for (var child : children) if (child instanceof AbstractWidget widget) {
            if (widget.getClass().getName().equals(FILTER)
                    || widget instanceof EditBox && widget.getMessage().getContents() instanceof TranslatableContents text
                    && text.getKey().equals("itemGroup.search")) filter = widget;
            if (widget.getClass().getName().equals(PIN)) pin = widget;
        }
        if (filter == null || !filter.visible) return;
        int pinWidth = pin != null && pin.visible ? pin.getWidth() + 3 : 0;
        int total = filter.getWidth() + pinWidth;
        int height = Math.max(filter.getHeight(), pin != null && pin.visible ? pin.getHeight() : 0);
        int x = left + 2, y = titleY - height - 2;
        int limit = left + width - 2;
        // Share the existing toolbar when possible. Crowded multi-tab pages use a smaller field before adding another row.
        boolean retry;
        do {
            retry = false;
            for (var child : children) if (child instanceof AbstractWidget widget && widget.visible
                    && widget != filter && widget != pin && widget.getY() < titleY
                    && widget.getY() + widget.getHeight() > y
                    && widget.getX() < x + total && widget.getX() + widget.getWidth() > x) {
                x = widget.getX() + widget.getWidth() + 2;
                retry = true;
            }
        } while (retry && x + total <= limit);
        if (x + total > limit) {
            var controls = new ArrayList<AbstractWidget>();
            for (var child : children) if (child instanceof AbstractWidget widget && widget.visible
                    && widget != filter && widget != pin && widget.getY() < titleY
                    && widget.getY() + widget.getHeight() > y) controls.add(widget);
            controls.sort(Comparator.comparingInt(AbstractWidget::getX));
            int cursor = left + 2, bestX = cursor, bestWidth = 0;
            for (var widget : controls) {
                int gap = Math.min(limit, widget.getX() - 2) - cursor;
                if (gap > bestWidth) { bestWidth = gap; bestX = cursor; }
                cursor = Math.max(cursor, widget.getX() + widget.getWidth() + 2);
            }
            if (limit - cursor > bestWidth) { bestWidth = limit - cursor; bestX = cursor; }
            if (bestWidth - pinWidth >= 40) {
                x = bestX; filter.setWidth(Math.min(filter.getWidth(), bestWidth - pinWidth));
            } else {
                x = left + 2;
                for (var widget : controls) y = Math.min(y, widget.getY() - height - 2);
            }
        }
        filter.setX(x); filter.setY(y + height - filter.getHeight());
        if (pin != null && pin.visible) {
            pin.setX(x + filter.getWidth() + 3); pin.setY(y + height - pin.getHeight());
        }
    }
}
