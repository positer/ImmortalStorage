package com.immortalstorage.immortalstorage.network.storage;

import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.SourceVeinManagerBlockEntity;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-local index of loaded, owner-bound source veins that are visible from
 * the owner's Xianqiao storage. The index stores weak block-entity handles and
 * one-count prototypes only; the persistent amount remains in each source's
 * real cache.
 */
public final class SourceVeinStorageIndex {
    private static final Map<MinecraftServer, Map<UUID, OwnerDirectory>> SERVERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static void register(SourceVeinBlockEntity source) {
        SourceHandle handle = liveHandle(source);
        MinecraftServer server = server(source);
        if (handle == null || server == null || handle.owner() == null) return;
        OwnerDirectory directory = directory(server, handle.owner(), true);
        if (directory.register(handle)) notifyOwner(server, handle.owner(), handle.fluid());
    }

    public static void register(SourceVeinManagerBlockEntity manager) {
        MinecraftServer server = manager == null ? null : manager.server();
        UUID owner = manager == null ? null : manager.getOwner();
        if (server == null || owner == null) return;
        OwnerDirectory directory = directory(server, owner, true);
        boolean changed = directory.removePrefix(manager.storageIndexPrefix());
        for (int slot = 0; slot < manager.memberSlots(); slot++) {
            if (!manager.hasMember(slot)) continue;
            changed |= directory.register(new ManagerMemberHandle(manager, slot));
        }
        if (changed) {
            notifyOwner(server, owner, false);
            notifyOwner(server, owner, true);
        }
    }

    public static void unregister(SourceVeinManagerBlockEntity manager) {
        MinecraftServer server = manager == null ? null : manager.server();
        UUID owner = manager == null ? null : manager.getOwner();
        if (server == null || owner == null) return;
        OwnerDirectory directory = directory(server, owner, false);
        if (directory != null && directory.removePrefix(manager.storageIndexPrefix())) {
            notifyOwner(server, owner, false);
            notifyOwner(server, owner, true);
        }
    }

    /** Publishes live manager-cache changes without rebuilding its 72-slot membership index. */
    public static void changed(SourceVeinManagerBlockEntity manager, boolean itemChanged, boolean fluidChanged) {
        MinecraftServer server = manager == null ? null : manager.server();
        UUID owner = manager == null ? null : manager.getOwner();
        if (server == null || owner == null || !itemChanged && !fluidChanged) return;
        OwnerDirectory directory = directory(server, owner, false);
        if (directory == null) return;
        boolean publishItem = itemChanged && directory.touchPrefix(manager.storageIndexPrefix(), false);
        boolean publishFluid = fluidChanged && directory.touchPrefix(manager.storageIndexPrefix(), true);
        if (publishItem) notifyOwner(server, owner, false);
        if (publishFluid) notifyOwner(server, owner, true);
    }

    public static void unregister(SourceVeinBlockEntity source) {
        MinecraftServer server = server(source);
        UUID owner = source == null ? null : source.getOwner();
        if (server == null || owner == null) return;
        OwnerDirectory directory = directory(server, owner, false);
        if (directory == null) return;
        SourceHandle removed = directory.remove(source.storageIndexId());
        if (removed != null) notifyOwner(server, owner, removed.fluid());
    }

    /** Publishes a real paid-cache mutation to every storage view. */
    public static void changed(SourceVeinBlockEntity source) {
        MinecraftServer server = server(source);
        UUID owner = source == null ? null : source.getOwner();
        if (server == null || owner == null) return;
        OwnerDirectory directory = directory(server, owner, false);
        if (directory == null || !directory.touch(source.storageIndexId())) return;
        notifyOwner(server, owner, source.fluidSource());
    }

    /** Rebinds one loaded source after a data-definition reload and invalidates both catalogs. */
    public static void rebindDefinition(SourceVeinBlockEntity source) {
        MinecraftServer server = server(source);
        UUID owner = source == null ? null : source.getOwner();
        if (server == null || owner == null) return;
        OwnerDirectory directory = directory(server, owner, true);
        directory.remove(source.storageIndexId());
        directory.register(new BlockEntityHandle(source));
        notifyOwner(server, owner, false);
        notifyOwner(server, owner, true);
    }

    public static List<StorageItemSummary> itemSnapshot(MinecraftServer server, UUID owner) {
        OwnerDirectory directory = directory(server, owner, false);
        return directory == null ? List.of() : directory.itemSnapshot();
    }

    public static Map<TerminalFluidKey, Long> fluidSnapshot(MinecraftServer server, UUID owner) {
        OwnerDirectory directory = directory(server, owner, false);
        return directory == null ? Map.of() : directory.fluidSnapshot();
    }

    public static long extractItem(MinecraftServer server, UUID owner, TerminalEntryKey key,
                                   long amount, TerminalStorageAction action) {
        OwnerDirectory directory = directory(server, owner, false);
        return directory == null ? 0L : directory.extractItem(key, amount, action);
    }

    public static long extractFluid(MinecraftServer server, UUID owner, TerminalFluidKey key,
                                    long amount, TerminalStorageAction action) {
        OwnerDirectory directory = directory(server, owner, false);
        return directory == null ? 0L : directory.extractFluid(key, amount, action);
    }

    public static long itemRevision(MinecraftServer server, UUID owner) {
        OwnerDirectory directory = directory(server, owner, false);
        return directory == null ? 0L : directory.itemRevision();
    }

    public static long fluidRevision(MinecraftServer server, UUID owner) {
        OwnerDirectory directory = directory(server, owner, false);
        return directory == null ? 0L : directory.fluidRevision();
    }

    private static SourceHandle liveHandle(SourceVeinBlockEntity source) {
        if (source == null || source.getOwner() == null
                || !(source.getLevel() instanceof ServerLevel level)
                || !ImmortalStorageDimensions.isPersonalRealmFor(level.dimension(), source.getOwner())) return null;
        return new BlockEntityHandle(source);
    }

    private static MinecraftServer server(SourceVeinBlockEntity source) {
        return source != null && source.getLevel() instanceof ServerLevel level ? level.getServer() : null;
    }

    private static OwnerDirectory directory(MinecraftServer server, UUID owner, boolean create) {
        if (server == null || owner == null) return null;
        synchronized (SERVERS) {
            Map<UUID, OwnerDirectory> owners = SERVERS.get(server);
            if (owners == null && create) {
                owners = new LinkedHashMap<>();
                SERVERS.put(server, owners);
            }
            if (owners == null) return null;
            return create ? owners.computeIfAbsent(owner, OwnerDirectory::new) : owners.get(owner);
        }
    }

    private static void notifyOwner(MinecraftServer server, UUID owner, boolean fluid) {
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player == null) return;
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (fluid) data.invalidateXianqiaoSourceFluidDirectory();
        else data.invalidateXianqiaoSourceItemDirectory();
    }

    interface SourceHandle {
        String sourceId();
        UUID owner();
        boolean activeFor(UUID requestedOwner);
        boolean fluid();
        boolean free();
        ItemStack itemPrototype();
        FluidStack fluidPrototype();
        long availableUnits();
        long extract(long requested, boolean simulate);

        /** Production handles publish mutations through {@link #changed}; test fakes do not. */
        default boolean publishesMutation() { return false; }
    }

    static final class OwnerDirectory {
        private final UUID owner;
        private final TreeMap<String, SourceHandle> sources = new TreeMap<>();
        private long itemRevision;
        private long fluidRevision;

        OwnerDirectory(UUID owner) {
            if (owner == null) throw new IllegalArgumentException("owner is required");
            this.owner = owner;
        }

        synchronized boolean register(SourceHandle source) {
            if (source == null || source.sourceId() == null || source.sourceId().isBlank()) return false;
            SourceHandle previous = sources.put(source.sourceId(), source);
            if (previous != null && sameLiveSource(previous, source)) return false;
            bump(source.fluid());
            if (previous != null && previous.fluid() != source.fluid()) bump(previous.fluid());
            return true;
        }

        synchronized boolean unregister(String sourceId) {
            return remove(sourceId) != null;
        }

        synchronized SourceHandle remove(String sourceId) {
            if (sourceId == null) return null;
            SourceHandle removed = sources.remove(sourceId);
            if (removed != null) bump(removed.fluid());
            return removed;
        }

        synchronized boolean removePrefix(String prefix) {
            if (prefix == null || prefix.isBlank()) return false;
            boolean item = false;
            boolean fluid = false;
            var iterator = sources.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (!entry.getKey().startsWith(prefix)) continue;
                if (entry.getValue().fluid()) fluid = true;
                else item = true;
                iterator.remove();
            }
            if (item) bump(false);
            if (fluid) bump(true);
            return item || fluid;
        }

        synchronized boolean touch(String sourceId) {
            SourceHandle source = sources.get(sourceId);
            if (source == null) return false;
            bump(source.fluid());
            return true;
        }

        synchronized boolean touchPrefix(String prefix, boolean fluid) {
            if (prefix == null || prefix.isBlank()) return false;
            boolean found = sources.entrySet().stream().anyMatch(entry ->
                    entry.getKey().startsWith(prefix) && entry.getValue().fluid() == fluid);
            if (found) bump(fluid);
            return found;
        }

        synchronized long itemRevision() { return itemRevision; }
        synchronized long fluidRevision() { return fluidRevision; }

        synchronized List<StorageItemSummary> itemSnapshot() {
            LinkedHashMap<TerminalEntryKey, MutableItem> grouped = new LinkedHashMap<>();
            for (SourceHandle source : sources.values()) {
                if (source.fluid() || !source.activeFor(owner)) continue;
                ItemStack prototype = source.itemPrototype();
                if (prototype == null || prototype.isEmpty()) continue;
                TerminalEntryKey key = TerminalEntryKey.of(prototype);
                MutableItem item = grouped.computeIfAbsent(key,
                        ignored -> new MutableItem(prototype.copyWithCount(1)));
                long amount = Math.max(0L, source.availableUnits());
                if (source.free() || item.amount == Long.MAX_VALUE) {
                    item.amount = Math.max(item.amount, amount);
                } else {
                    item.amount = saturatingAdd(item.amount, amount);
                }
            }
            ArrayList<StorageItemSummary> result = new ArrayList<>(grouped.size());
            grouped.values().forEach(item -> {
                if (item.amount > 0L) result.add(new StorageItemSummary(item.prototype, item.amount));
            });
            return List.copyOf(result);
        }

        synchronized Map<TerminalFluidKey, Long> fluidSnapshot() {
            LinkedHashMap<TerminalFluidKey, Long> result = new LinkedHashMap<>();
            for (SourceHandle source : sources.values()) {
                if (!source.fluid() || !source.activeFor(owner)) continue;
                FluidStack prototype = source.fluidPrototype();
                if (prototype == null || prototype.isEmpty()) continue;
                TerminalFluidKey key = TerminalFluidKey.of(prototype);
                long previous = result.getOrDefault(key, 0L);
                long amount = Math.max(0L, source.availableUnits());
                result.put(key, source.free() || previous == Long.MAX_VALUE
                        ? Math.max(previous, amount) : saturatingAdd(previous, amount));
            }
            result.values().removeIf(amount -> amount == null || amount <= 0L);
            return Map.copyOf(result);
        }

        synchronized long extractItem(TerminalEntryKey key, long amount, TerminalStorageAction action) {
            if (key == null || amount <= 0L || action == null) return 0L;
            SourcePredicate matches = source -> {
                if (source.fluid()) return false;
                ItemStack prototype = source.itemPrototype();
                return prototype != null && !prototype.isEmpty() && key.matches(prototype);
            };
            return extract(Math.min(amount, visibleAmount(matches)), action, matches);
        }

        synchronized long extractFluid(TerminalFluidKey key, long amount, TerminalStorageAction action) {
            if (key == null || amount <= 0L || action == null) return 0L;
            SourcePredicate matches = source -> {
                if (!source.fluid()) return false;
                FluidStack prototype = source.fluidPrototype();
                return prototype != null && !prototype.isEmpty()
                        && key.equals(TerminalFluidKey.of(prototype));
            };
            return extract(Math.min(amount, visibleAmount(matches)), action, matches);
        }

        private long visibleAmount(SourcePredicate matches) {
            long visible = 0L;
            for (SourceHandle source : sources.values()) {
                if (!source.activeFor(owner) || !matches.test(source)) continue;
                long amount = Math.max(0L, source.availableUnits());
                if (source.free()) return amount;
                visible = saturatingAdd(visible, amount);
            }
            return visible;
        }

        private long extract(long amount, TerminalStorageAction action, SourcePredicate matches) {
            long remaining = amount;
            long extracted = 0L;
            for (SourceHandle source : sources.values()) {
                if (remaining <= 0L) break;
                if (!source.activeFor(owner) || !matches.test(source)) continue;
                long visible = Math.max(0L, source.availableUnits());
                long request = Math.min(remaining, visible);
                if (request <= 0L) continue;
                long committed = Math.max(0L, Math.min(request,
                        source.extract(request, !action.executes())));
                extracted = saturatingAdd(extracted, committed);
                remaining -= committed;
                if (action.executes() && committed > 0L && !source.free() && !source.publishesMutation()) {
                    bump(source.fluid());
                }
            }
            return extracted;
        }

        private void bump(boolean fluid) {
            if (fluid) fluidRevision = nextRevision(fluidRevision);
            else itemRevision = nextRevision(itemRevision);
        }

        private static boolean sameLiveSource(SourceHandle left, SourceHandle right) {
            if (left == right) return true;
            if (left instanceof BlockEntityHandle a && right instanceof BlockEntityHandle b) {
                return a.source.get() == b.source.get();
            }
            return false;
        }
    }

    private static final class BlockEntityHandle implements SourceHandle {
        private final WeakReference<SourceVeinBlockEntity> source;
        private final String sourceId;
        private final UUID owner;

        private BlockEntityHandle(SourceVeinBlockEntity source) {
            this.source = new WeakReference<>(source);
            this.sourceId = source.storageIndexId();
            this.owner = source.getOwner();
        }

        @Override public String sourceId() { return sourceId; }
        @Override public UUID owner() { return owner; }
        @Override public boolean activeFor(UUID requestedOwner) {
            SourceVeinBlockEntity live = source.get();
            return live != null && live.isVisibleInXianqiaoStorage(requestedOwner);
        }
        @Override public boolean fluid() {
            SourceVeinBlockEntity live = source.get();
            return live != null && live.fluidSource();
        }
        @Override public boolean free() {
            SourceVeinBlockEntity live = source.get();
            return live != null && live.definition().free();
        }
        @Override public ItemStack itemPrototype() {
            SourceVeinBlockEntity live = source.get();
            return live == null ? ItemStack.EMPTY : live.itemSample(1);
        }
        @Override public FluidStack fluidPrototype() {
            SourceVeinBlockEntity live = source.get();
            return live == null || !live.fluidSource() ? FluidStack.EMPTY : new FluidStack(live.fluid(), 1);
        }
        @Override public long availableUnits() {
            SourceVeinBlockEntity live = source.get();
            return live == null ? 0L : live.storageVisibleUnits();
        }
        @Override public long extract(long requested, boolean simulate) {
            SourceVeinBlockEntity live = source.get();
            return live == null ? 0L : live.extractForXianqiaoStorage(owner, requested, simulate);
        }
        @Override public boolean publishesMutation() { return true; }
    }

    private static final class ManagerMemberHandle implements SourceHandle {
        private final WeakReference<SourceVeinManagerBlockEntity> manager;
        private final int slot;
        private final String sourceId;
        private final UUID owner;

        private ManagerMemberHandle(SourceVeinManagerBlockEntity manager, int slot) {
            this.manager = new WeakReference<>(manager);
            this.slot = slot;
            this.sourceId = manager.storageIndexPrefix() + slot;
            this.owner = manager.getOwner();
        }

        @Override public String sourceId() { return sourceId; }
        @Override public UUID owner() { return owner; }
        @Override public boolean activeFor(UUID requestedOwner) {
            SourceVeinManagerBlockEntity live = manager.get();
            return live != null && live.isMemberVisible(slot, requestedOwner);
        }
        @Override public boolean fluid() {
            SourceVeinManagerBlockEntity live = manager.get();
            return live != null && live.memberIsFluid(slot);
        }
        @Override public boolean free() {
            SourceVeinManagerBlockEntity live = manager.get();
            return live != null && live.memberIsFree(slot);
        }
        @Override public ItemStack itemPrototype() {
            SourceVeinManagerBlockEntity live = manager.get();
            return live == null ? ItemStack.EMPTY : live.memberItemPrototype(slot);
        }
        @Override public FluidStack fluidPrototype() {
            SourceVeinManagerBlockEntity live = manager.get();
            return live == null ? FluidStack.EMPTY : live.memberFluidPrototype(slot);
        }
        @Override public long availableUnits() {
            SourceVeinManagerBlockEntity live = manager.get();
            return live == null ? 0L : live.memberAvailableUnits(slot);
        }
        @Override public long extract(long requested, boolean simulate) {
            SourceVeinManagerBlockEntity live = manager.get();
            return live == null ? 0L : live.extractMember(slot, requested, simulate);
        }
        @Override public boolean publishesMutation() { return true; }
    }

    private static long saturatingAdd(long left, long right) {
        if (left <= 0L) return Math.max(0L, right);
        if (right <= 0L) return left;
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static long nextRevision(long revision) {
        return revision == Long.MAX_VALUE ? Long.MAX_VALUE : revision + 1L;
    }

    @FunctionalInterface
    private interface SourcePredicate {
        boolean test(SourceHandle source);
    }

    private static final class MutableItem {
        private final ItemStack prototype;
        private long amount;

        private MutableItem(ItemStack prototype) {
            this.prototype = prototype;
        }
    }

    private SourceVeinStorageIndex() {}
}
