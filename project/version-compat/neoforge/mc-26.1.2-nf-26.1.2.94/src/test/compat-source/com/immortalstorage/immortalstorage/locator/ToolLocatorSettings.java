package com.immortalstorage.immortalstorage.locator;

import com.google.gson.JsonObject;
import com.immortalstorage.immortalstorage.config.ImmortalStorageConfig;

public final class ToolLocatorSettings {
    public static final int DEFAULT_RADIUS = 32, DEFAULT_MINING_RADIUS = 16, MAX_RADIUS = 48, CACHE_CHUNKS = 3;
    private static volatile int dataRadius, dataMiningRadius;
    private static volatile long generation;
    private ToolLocatorSettings() {}
    public static int parseRadius(JsonObject json) {
        return parseRadius(json, "radius");
    }
    public static int parseMiningRadius(JsonObject json) { return parseRadius(json, "mining_radius"); }
    private static int parseRadius(JsonObject json, String key) {
        if (!json.has(key)) return 0;
        if (!json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isNumber())
            throw new IllegalArgumentException("radius must be an integer");
        int value = json.getAsJsonPrimitive(key).getAsBigDecimal().intValueExact();
        if (value < 1 || value > MAX_RADIUS) throw new IllegalArgumentException("radius must be 1..48");
        return value;
    }
    public static void install(int override, int miningOverride) {
        if (override < 0 || override > MAX_RADIUS || miningOverride < 0 || miningOverride > MAX_RADIUS) throw new IllegalArgumentException("radius");
        dataRadius = override; dataMiningRadius = miningOverride; generation++;
    }
    public static void tagsChanged() { generation++; }
    public static long generation() { return generation; }
    public static int radius() { return dataRadius > 0 ? dataRadius : ImmortalStorageConfig.TOOL_LOCATOR_RADIUS.get(); }
    public static int miningRadius() { return dataMiningRadius > 0 ? dataMiningRadius : ImmortalStorageConfig.TOOL_LOCATOR_MINING_RADIUS.get(); }
}
