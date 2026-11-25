package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.network.chat.Component;

/**
 * Localized face labels for the ruin filter panels; legacy ASCII tokens remain available.
 */
public final class RuinFaceText {
    private RuinFaceText() {
    }

    public static Component label(int ordinal) {
        String name = switch (ordinal) {
            case 0 -> "down"; case 1 -> "up"; case 2 -> "north";
            case 3 -> "south"; case 4 -> "west"; case 5 -> "east"; default -> "any";
        };
        return Component.translatable("container.immortalstorage.configuration.face." + name);
    }

    /** Ordinal is the {@link net.minecraft.core.Direction} index; -1 means any face. */
    public static String abbr(int ordinal) {
        return switch (ordinal) {
            case 0 -> "D";
            case 1 -> "U";
            case 2 -> "N";
            case 3 -> "S";
            case 4 -> "W";
            case 5 -> "E";
            default -> "A";
        };
    }
}
