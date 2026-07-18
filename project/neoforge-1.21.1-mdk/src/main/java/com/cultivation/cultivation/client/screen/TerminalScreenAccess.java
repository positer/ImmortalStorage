package com.cultivation.cultivation.client.screen;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/** Stable client surface consumed by optional recipe-viewer integrations. */
public interface TerminalScreenAccess {
    List<Rect2i> cultivation$getExtraAreas();

    Slot cultivation$getSlotAt(double mouseX, double mouseY);

    Rect2i cultivation$getSlotBounds(Slot slot);

    boolean cultivation$isCraftingVisible();
}
