package com.immortalstorage.immortalstorage.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Client mirror of the server-authoritative time-shift record, one immutable
 * snapshot per dimension.
 *
 * <p>The client network thread writes while the render thread reads, so the map
 * is concurrent and every published value is an immutable list that is never
 * mutated in place.  The cache never drives a gameplay decision; the renderer is
 * its only reader.</p>
 */
public final class TimeShiftClientCache {
    /** One shifted block: its packed-pos form is decoded by the network layer. */
    public record Entry(BlockPos pos, int tier, boolean boosted, long boostEndEpochMillis) {}

    private static final Map<ResourceKey<Level>, List<Entry>> RECORDS = new ConcurrentHashMap<>();

    private TimeShiftClientCache() {}

    /**
     * Replaces the whole record of {@code dimension}.  An empty list drops the
     * dimension instead of storing an empty snapshot, so a cleared record can
     * never grow back into a stale one.
     */
    public static void replace(ResourceKey<Level> dimension, List<Entry> entries) {
        if (dimension == null) return;
        if (entries == null || entries.isEmpty()) {
            RECORDS.remove(dimension);
        } else {
            RECORDS.put(dimension, List.copyOf(entries));
        }
    }

    /** Drops every dimension; intended for a disconnect or a level reload. */
    public static void clearAll() {
        RECORDS.clear();
    }

    /** The current snapshot of {@code dimension}; empty when nothing is recorded. */
    public static List<Entry> entries(ResourceKey<Level> dimension) {
        if (dimension == null) return List.of();
        List<Entry> snapshot = RECORDS.get(dimension);
        return snapshot != null ? snapshot : List.of();
    }
}
