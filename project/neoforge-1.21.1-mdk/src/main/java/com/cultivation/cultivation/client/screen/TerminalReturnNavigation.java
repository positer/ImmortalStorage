package com.cultivation.cultivation.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.jetbrains.annotations.Nullable;

/** One-shot client navigation target used when the global storage key replaces an open screen. */
public final class TerminalReturnNavigation {
    private static final long MAX_WAIT_MS = 5_000L;
    private static Screen pending;
    private static long armedAt;

    public static void arm(@Nullable Screen screen) {
        // Server-backed container menus are closed when the terminal opens and cannot be revived safely.
        pending = screen instanceof AbstractContainerScreen<?> ? null : screen;
        armedAt = System.currentTimeMillis();
    }

    @Nullable
    static Screen take() {
        Screen result = System.currentTimeMillis() - armedAt <= MAX_WAIT_MS ? pending : null;
        pending = null;
        armedAt = 0L;
        return result;
    }

    private TerminalReturnNavigation() {}
}
