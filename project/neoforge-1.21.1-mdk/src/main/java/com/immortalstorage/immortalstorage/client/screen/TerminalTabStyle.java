package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.resources.ResourceLocation;

/**
 * Runtime Minecraft Advancement-tab geometry shared by terminal widgets and
 * layout tests. No vanilla texture is copied into ImmortalStorage resources.
 */
public final class TerminalTabStyle {
    public static final int WIDTH = 32;
    public static final int HEIGHT = 28;
    public static final int PANEL_OVERLAP = 4;

    public enum Side {
        LEFT("left", 10),
        RIGHT("right", 6);

        private final String resourcePart;
        private final int iconInsetX;

        Side(String resourcePart, int iconInsetX) {
            this.resourcePart = resourcePart;
            this.iconInsetX = iconInsetX;
        }

        public int iconInsetX() {
            return this.iconInsetX;
        }
    }

    public enum Segment {
        TOP("top"),
        MIDDLE("middle"),
        BOTTOM("bottom");

        private final String resourcePart;

        Segment(String resourcePart) {
            this.resourcePart = resourcePart;
        }
    }

    private TerminalTabStyle() {}

    public static Segment segment(int index, int count) {
        if (count <= 0 || index < 0 || index >= count) {
            throw new IllegalArgumentException("tab index " + index + " outside count " + count);
        }
        if (index == 0) return Segment.TOP;
        if (index == count - 1) return Segment.BOTTOM;
        return Segment.MIDDLE;
    }

    public static ResourceLocation sprite(Side side, Segment segment, boolean selected) {
        String suffix = selected ? "_selected" : "";
        return ResourceLocation.withDefaultNamespace("advancements/tab_"
                + side.resourcePart + "_" + segment.resourcePart + suffix);
    }
}
