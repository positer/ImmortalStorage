package com.immortalstorage.immortalstorage.network.storage;

import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.server.MinecraftServer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Live, owner-guarded long item view over stage-six-or-higher Xianqiao storage. */
public final class PersonalStorageLongItemStorage implements TerminalItemStorage {
    private final ImmortalStoragePlayerData data;
    private final Runnable onChanged;
    private final BooleanSupplier accessAllowed;
    private final MinecraftServer server;
    private final UUID owner;
    private long observedPhysicalGeneration = Long.MIN_VALUE;
    private long observedSourceGeneration = Long.MIN_VALUE;
    private long viewRevision;

    public PersonalStorageLongItemStorage(
            ImmortalStoragePlayerData data, Runnable onChanged, BooleanSupplier accessAllowed) {
        this(data, onChanged, accessAllowed, null, null);
    }

    public PersonalStorageLongItemStorage(
            ImmortalStoragePlayerData data, Runnable onChanged, BooleanSupplier accessAllowed,
            MinecraftServer server, UUID owner) {
        if (data == null) throw new IllegalArgumentException("player data is required");
        this.data = data;
        this.onChanged = onChanged == null ? () -> {} : onChanged;
        this.accessAllowed = accessAllowed == null ? () -> false : accessAllowed;
        this.server = server;
        this.owner = owner;
    }

    private boolean isAvailable() {
        return data.getStage() >= 6 && accessAllowed.getAsBoolean();
    }

    @Override
    public long revision() {
        if (!isAvailable()) return 0L;
        if (!hasSourceDirectory()) return data.getXianqiaoStorageRevision();
        long physical = data.getXianqiaoStorageGeneration();
        long source = data.getXianqiaoSourceItemGeneration();
        if (observedPhysicalGeneration == Long.MIN_VALUE) {
            viewRevision = Math.max(0L, physical);
        } else if (physical != observedPhysicalGeneration || source != observedSourceGeneration) {
            viewRevision = nextRevision(viewRevision);
        }
        observedPhysicalGeneration = physical;
        observedSourceGeneration = source;
        return viewRevision;
    }

    @Override
    public List<StorageItemSummary> snapshot() {
        if (!isAvailable()) return List.of();
        LinkedHashMap<TerminalEntryKey, StorageItemSummary> grouped = new LinkedHashMap<>();
        for (StorageItemSummary summary : data.getXianqiaoItemSummary()) {
            if (data.isInfiniteImmortalYuan()
                    && data.isVirtualInfiniteImmortalYuanStack(summary.prototype())) continue;
            grouped.put(TerminalEntryKey.of(summary.prototype()), summary);
        }
        if (hasSourceDirectory()) {
            for (StorageItemSummary source : SourceVeinStorageIndex.itemSnapshot(server, owner)) {
                TerminalEntryKey key = TerminalEntryKey.of(source.prototype());
                StorageItemSummary previous = grouped.get(key);
                long amount = previous == null ? source.amount()
                        : saturatingAdd(previous.amount(), source.amount());
                grouped.put(key, new StorageItemSummary(source.prototype(), amount));
            }
        }
        if (data.isInfiniteImmortalYuan()) {
            data.getVirtualTerminalEntries().forEach(entry -> grouped.put(
                    TerminalEntryKey.of(entry.prototype()),
                    new StorageItemSummary(entry.prototype(), entry.amount())));
        }
        return List.copyOf(grouped.values());
    }

    @Override
    public long insert(TerminalEntryKey key, long amount, TerminalStorageAction action) {
        if (!isAvailable()) return 0L;
        long committed = data.insertXianqiaoItem(key, amount, action);
        if (committed > 0L && action != null && action.executes()) onChanged.run();
        return committed;
    }

    @Override
    public long extract(TerminalEntryKey key, long amount, TerminalStorageAction action) {
        if (!isAvailable()) return 0L;
        long physical = data.extractXianqiaoItem(key, amount, action);
        long remaining = Math.max(0L, amount - physical);
        long source = hasSourceDirectory() && remaining > 0L
                ? SourceVeinStorageIndex.extractItem(server, owner, key, remaining, action) : 0L;
        long committed = saturatingAdd(physical, source);
        if (committed > 0L && action != null && action.executes()) onChanged.run();
        return committed;
    }

    private boolean hasSourceDirectory() {
        return server != null && owner != null;
    }

    private static long saturatingAdd(long left, long right) {
        if (left <= 0L) return Math.max(0L, right);
        if (right <= 0L) return left;
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static long nextRevision(long revision) {
        return revision == Long.MAX_VALUE ? 0L : revision + 1L;
    }
}
