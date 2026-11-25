package com.immortalstorage.immortalstorage.client.screen;

/** Keep the title and the machine body reachable, including oversized GUI scales. */
public final class ConfigurationWindowBounds {
    private ConfigurationWindowBounds() {}
    public static int x(int value, int viewport, int body) {
        return Math.max(Math.min(0, viewport - body), Math.min(Math.max(0, viewport - body), value));
    }
    public static int y(int value, int viewport, int body) {
        int minimum = Math.max(-12, Math.min(21, viewport - body));
        return Math.max(minimum, Math.min(Math.max(21, viewport - body), value));
    }
}
