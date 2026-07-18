package com.cultivation.cultivation.worldshard;

import com.cultivation.cultivation.CultivationMod;
import com.cultivation.cultivation.api.storage.terminal.TerminalEntryKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalItemStorage;
import com.cultivation.cultivation.api.storage.terminal.TerminalStorageAction;
import com.cultivation.cultivation.network.storage.PersonalStorageNetwork;
import com.cultivation.cultivation.player.CultivationPlayerData;
import com.cultivation.cultivation.player.yuan.YuanItemPolicy;
import com.cultivation.cultivation.player.yuan.YuanKind;
import com.cultivation.cultivation.player.yuan.YuanRule;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Strict, dimension-selected output transactions for both shard generators. */
public final class WorldShardOutputRouter {
    private WorldShardOutputRouter() {
    }

    /**
     * Publishes a non-owner-realm generation to the miner's barrel-sized cache.
     * The cache itself plans against a detached snapshot, so either every
     * component-preserving stack appears at once or its live slots stay intact.
     */
    public static RouteResult routeCache(List<ItemStack> outputs, @Nullable WorldShardMinerCache cache) {
        List<ItemStack> entries = normalizedCopies(outputs);
        long offered = total(entries);
        if (offered == 0L) return new RouteResult(0L, 0L, 0L);
        if (cache == null) return new RouteResult(offered, 0L, offered);
        boolean committed;
        try {
            committed = cache.tryInsertAll(entries);
        } catch (RuntimeException failure) {
            CultivationMod.LOG.error("World shard output cache transaction failed", failure);
            committed = false;
        }
        return committed
                ? new RouteResult(offered, offered, 0L)
                : new RouteResult(offered, 0L, offered);
    }

    /**
     * Strict direct-storage transaction for the bound owner's Xianqiao.
     *
     * <p>Identical component identities are aggregated first. Yuan capacity is
     * reserved cumulatively for the complete roll instead of independently per
     * stack, closing the simulate(A), simulate(B), commit(A), reject(B) replay
     * hole. The built-in data backend then commits under one mutation scope. A
     * defensive physical snapshot is restored inside that same scope if an
     * unexpected implementation divergence occurs.</p>
     */
    public static RouteResult routeDirect(
            List<ItemStack> outputs, @Nullable PersonalStorageNetwork.Endpoint endpoint) {
        List<ItemStack> entries = normalizedCopies(outputs);
        long offered = total(entries);
        if (offered == 0L) return new RouteResult(0L, 0L, 0L);
        if (endpoint == null || !endpoint.online() || endpoint.stage() < 6) {
            return new RouteResult(offered, 0L, offered);
        }

        CultivationPlayerData data = endpoint.data();
        TerminalItemStorage storage = endpoint.itemStorage();
        List<BatchEntry> batch = aggregate(entries);
        if (storage == null || !fitsYuanCapacity(data, batch) || !simulatesCompletely(storage, batch)) {
            return new RouteResult(offered, 0L, offered);
        }

        List<ItemStack> before = data.snapshotStorage(true);
        CommitResult commit = data.batchXianqiaoMutations(() -> {
            long accepted = 0L;
            try {
                for (BatchEntry entry : batch) {
                    long inserted = storage.insert(
                            entry.key(), entry.amount(), TerminalStorageAction.EXECUTE);
                    if (inserted != entry.amount()) {
                        data.replaceStorage(true, before);
                        return new CommitResult(false, 0L);
                    }
                    accepted = saturatedAdd(accepted, inserted);
                }
                return new CommitResult(accepted == offered, accepted);
            } catch (RuntimeException failure) {
                data.replaceStorage(true, before);
                CultivationMod.LOG.error("World shard output Xianqiao transaction rolled back", failure);
                return new CommitResult(false, 0L);
            }
        });
        if (!commit.committed() || commit.accepted() != offered) {
            CultivationMod.LOG.error(
                    "World shard output Xianqiao transaction rejected after preflight; no output was published");
            return new RouteResult(offered, 0L, offered);
        }
        return new RouteResult(offered, offered, 0L);
    }

    public static RouteResult reject(List<ItemStack> outputs) {
        long offered = total(normalizedCopies(outputs));
        return new RouteResult(offered, 0L, offered);
    }

    private static boolean simulatesCompletely(TerminalItemStorage storage, List<BatchEntry> batch) {
        try {
            for (BatchEntry entry : batch) {
                long accepted = storage.insert(
                        entry.key(), entry.amount(), TerminalStorageAction.SIMULATE);
                if (accepted != entry.amount()) return false;
            }
            return true;
        } catch (RuntimeException failure) {
            CultivationMod.LOG.error("World shard output Xianqiao preflight failed", failure);
            return false;
        }
    }

    private static boolean fitsYuanCapacity(CultivationPlayerData data, List<BatchEntry> batch) {
        EnumMap<YuanKind, Long> requested = new EnumMap<>(YuanKind.class);
        for (BatchEntry entry : batch) {
            if (entry.yuanKind() == null) continue;
            requested.merge(entry.yuanKind(), entry.amount(), WorldShardOutputRouter::saturatedAdd);
        }
        for (Map.Entry<YuanKind, Long> entry : requested.entrySet()) {
            if (entry.getValue() > remainingYuanCapacity(data, entry.getKey())) return false;
        }
        return true;
    }

    private static long remainingYuanCapacity(CultivationPlayerData data, YuanKind kind) {
        if (kind == YuanKind.IMMORTAL && data.isInfiniteImmortalYuan()) return Long.MAX_VALUE;
        long cap = kind == YuanKind.TRUE
                ? data.getTrueYuanCapLong() : data.getImmortalYuanCapLong();
        if (cap == YuanRule.UNBOUNDED_CAP) return Long.MAX_VALUE;
        if (cap <= 0L) return 0L;
        long stored = kind == YuanKind.TRUE ? data.getTrueYuan() : data.getImmortalYuan();
        return Math.max(0L, cap - Math.min(cap, stored));
    }

    private static List<BatchEntry> aggregate(List<ItemStack> entries) {
        LinkedHashMap<TerminalEntryKey, BatchEntry> grouped = new LinkedHashMap<>();
        for (ItemStack stack : entries) {
            TerminalEntryKey key = TerminalEntryKey.of(stack);
            BatchEntry previous = grouped.get(key);
            long amount = previous == null
                    ? stack.getCount() : saturatedAdd(previous.amount(), stack.getCount());
            grouped.put(key, new BatchEntry(key, amount, YuanItemPolicy.kindOf(stack)));
        }
        return List.copyOf(grouped.values());
    }

    private static List<ItemStack> normalizedCopies(List<ItemStack> outputs) {
        if (outputs == null || outputs.isEmpty()) return List.of();
        List<ItemStack> entries = new ArrayList<>(outputs.size());
        for (ItemStack stack : outputs) {
            if (stack != null && !stack.isEmpty() && stack.getCount() > 0) entries.add(stack.copy());
        }
        return entries;
    }

    private static long total(List<ItemStack> entries) {
        long total = 0L;
        for (ItemStack stack : entries) total = saturatedAdd(total, stack.getCount());
        return total;
    }

    private static long saturatedAdd(long left, long right) {
        if (left <= 0L) return Math.max(0L, right);
        if (right <= 0L) return left;
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private record BatchEntry(TerminalEntryKey key, long amount, @Nullable YuanKind yuanKind) {
    }

    private record CommitResult(boolean committed, long accepted) {
    }

    public record RouteResult(long offered, long accepted, long unaccepted) {
    }
}
