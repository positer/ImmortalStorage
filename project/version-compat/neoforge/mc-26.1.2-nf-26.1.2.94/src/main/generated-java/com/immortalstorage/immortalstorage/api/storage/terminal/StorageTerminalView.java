package com.immortalstorage.immortalstorage.api.storage.terminal;

import java.util.List;

/** Additive public contract shared by terminal screens and optional integrations. */
public interface StorageTerminalView {
    TerminalViewport viewport();
    TerminalQuery query();
    List<TerminalEntry> visibleEntries();
    default int bufferedBaseRow() { return viewport().baseRow(); }
    default int bufferedRowCount() {
        return Math.min(TerminalViewport.MAX_ROWS, viewport().visibleRows() + 1);
    }
    int storageSlotStart();
    int storageSlotCount();
    int craftingSlotStart();
    int craftingResultSlot();
    int playerInventoryStart();
    boolean isCraftingUnlocked();
    boolean isCraftingVisible();
}
