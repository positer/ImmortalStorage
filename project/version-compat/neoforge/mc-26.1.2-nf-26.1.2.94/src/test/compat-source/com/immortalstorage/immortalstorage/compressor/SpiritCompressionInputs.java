package com.immortalstorage.immortalstorage.compressor;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;

/** Integral material allocation; overlapping ingredients never spend the same item twice. */
public final class SpiritCompressionInputs {
    public record Requirement(long count, IntPredicate acceptsSlot) {
        public Requirement { if (count < 1 || count > 768) throw new IllegalArgumentException("ingredient count"); }
    }
    public record Plan(long multiplier, int[] consumed) {}
    private SpiritCompressionInputs() {}

    public static Plan maximum(int[] available, List<Requirement> requirements) {
        if (requirements.isEmpty() || requirements.size() > 12) return null;
        long upper = Long.MAX_VALUE;
        for (Requirement requirement : requirements) {
            long matching = 0;
            for (int slot = 0; slot < available.length; slot++) {
                if (available[slot] < 0) throw new IllegalArgumentException("negative stock");
                if (requirement.acceptsSlot.test(slot)) matching += available[slot];
            }
            upper = Math.min(upper, matching / requirement.count);
        }
        long low = 0, high = upper;
        while (low < high) {
            long mid = low + (high - low + 1) / 2;
            if (allocate(available, requirements, mid) != null) low = mid; else high = mid - 1;
        }
        return low == 0 ? null : new Plan(low, allocate(available, requirements, low));
    }

    private static int[] allocate(int[] stock, List<Requirement> requirements, long multiplier) {
        int slots = stock.length, sink = slots + requirements.size() + 1, size = sink + 1;
        long[][] residual = new long[size][size];
        long total = 0;
        for (int s = 0; s < slots; s++) residual[0][s + 1] = stock[s];
        for (int r = 0; r < requirements.size(); r++) {
            Requirement requirement = requirements.get(r);
            long demand = Math.multiplyExact(requirement.count, multiplier);
            total = Math.addExact(total, demand);
            int node = slots + r + 1;
            residual[node][sink] = demand;
            for (int s = 0; s < slots; s++) if (requirement.acceptsSlot.test(s)) residual[s + 1][node] = stock[s];
        }
        long flow = 0;
        int[] parent = new int[size], queue = new int[size];
        while (flow < total) {
            Arrays.fill(parent, -1); parent[0] = 0;
            int head = 0, tail = 0; queue[tail++] = 0;
            while (head < tail && parent[sink] == -1) {
                int from = queue[head++];
                for (int to = 1; to < size; to++) if (parent[to] == -1 && residual[from][to] > 0) {
                    parent[to] = from; queue[tail++] = to;
                }
            }
            if (parent[sink] == -1) return null;
            long amount = total - flow;
            for (int to = sink; to != 0; to = parent[to]) amount = Math.min(amount, residual[parent[to]][to]);
            for (int to = sink; to != 0; to = parent[to]) {
                residual[parent[to]][to] -= amount; residual[to][parent[to]] += amount;
            }
            flow += amount;
        }
        int[] consumed = new int[slots];
        for (int s = 0; s < slots; s++) consumed[s] = Math.toIntExact(stock[s] - residual[0][s + 1]);
        return consumed;
    }
}
