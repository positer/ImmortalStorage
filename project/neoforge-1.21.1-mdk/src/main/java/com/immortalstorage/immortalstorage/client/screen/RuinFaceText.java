package com.immortalstorage.immortalstorage.client.screen;

/**
 * Face-button text for the ruin filter panels: UDNEWS first-letter
 * abbreviations (U/D/N/E/W/S) with "A" for any face. No translation needed.
 */
public final class RuinFaceText {
    private RuinFaceText() {
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
