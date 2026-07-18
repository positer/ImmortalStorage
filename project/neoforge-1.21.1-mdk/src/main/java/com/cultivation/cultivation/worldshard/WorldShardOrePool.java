package com.cultivation.cultivation.worldshard;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorldShardOrePool {
    private final List<Entry> entries;
    private final long totalWeight;

    private WorldShardOrePool(List<Entry> entries, long totalWeight) {
        this.entries = entries;
        this.totalWeight = totalWeight;
    }

    public static WorldShardOrePool of(Map<Item, Long> weights) {
        List<Map.Entry<Item, Long>> sorted = weights.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0L)
                .sorted(Comparator.comparing(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey()).toString()))
                .toList();
        List<Entry> entries = new ArrayList<>(sorted.size());
        long cumulative = 0L;
        for (Map.Entry<Item, Long> entry : sorted) {
            try {
                cumulative = Math.addExact(cumulative, entry.getValue());
            } catch (ArithmeticException overflow) {
                throw new IllegalArgumentException("ore weight total exceeds long range", overflow);
            }
            entries.add(new Entry(entry.getKey(), cumulative));
        }
        return new WorldShardOrePool(List.copyOf(entries), cumulative);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public Map<Item, Integer> sampleBatch(RandomSource random, int samples) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        if (samples <= 0 || totalWeight <= 0L) return counts;
        for (int sample = 0; sample < samples; sample++) {
            long target = nextLong(random, totalWeight) + 1L;
            int low = 0;
            int high = entries.size() - 1;
            while (low < high) {
                int mid = (low + high) >>> 1;
                if (target <= entries.get(mid).cumulativeWeight()) high = mid;
                else low = mid + 1;
            }
            counts.merge(entries.get(low).item(), 1, Integer::sum);
        }
        return counts;
    }

    private static long nextLong(RandomSource random, long bound) {
        long bits;
        long value;
        do {
            bits = random.nextLong() >>> 1;
            value = bits % bound;
        } while (bits - value + (bound - 1L) < 0L);
        return value;
    }

    private record Entry(Item item, long cumulativeWeight) {
    }
}
