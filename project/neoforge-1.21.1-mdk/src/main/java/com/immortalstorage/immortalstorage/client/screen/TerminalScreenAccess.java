package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/** Stable client surface consumed by optional recipe-viewer integrations. */
public interface TerminalScreenAccess {
    List<Rect2i> immortalstorage$getExtraAreas();

    Slot immortalstorage$getSlotAt(double mouseX, double mouseY);

    Rect2i immortalstorage$getSlotBounds(Slot slot);

    boolean immortalstorage$isCraftingVisible();
}
