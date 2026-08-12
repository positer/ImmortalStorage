package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Immutable mode/owner snapshot inherited from the active miner directly below. */
public record TreasureBasinActivation(@Nullable Identifier mode,
                                      @Nullable UUID owner) {
    public TreasureBasinActivation {
        if (mode == null) owner = null;
    }

    public static TreasureBasinActivation resolve(
            boolean directlyBelow,
            boolean minerActive,
            @Nullable Identifier mode,
            @Nullable UUID owner) {
        if (!directlyBelow || !minerActive || mode == null) return inactive();
        return new TreasureBasinActivation(mode, owner);
    }

    public static TreasureBasinActivation inactive() {
        return new TreasureBasinActivation(null, null);
    }

    public boolean active() {
        return mode != null;
    }
}
