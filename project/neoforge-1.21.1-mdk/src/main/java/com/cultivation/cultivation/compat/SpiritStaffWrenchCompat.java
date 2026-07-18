package com.cultivation.cultivation.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Loader-neutral dispatch for optional-mod wrench APIs. */
public final class SpiritStaffWrenchCompat {
    private static final List<Handler> HANDLERS = new CopyOnWriteArrayList<>();

    public static void register(Handler handler) {
        if (handler != null) HANDLERS.add(handler);
    }

    public static InteractionResult interact(UseOnContext context, ServerPlayer player) {
        for (Handler handler : HANDLERS) {
            InteractionResult result = handler.interact(context, player);
            if (result != null && result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    }

    @FunctionalInterface
    public interface Handler {
        InteractionResult interact(UseOnContext context, ServerPlayer player);
    }

    private SpiritStaffWrenchCompat() {}
}
