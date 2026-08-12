package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.StorageCell;
import com.immortalstorage.immortalstorage.api.storage.ExternalResourceStorage;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageApi;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.immortalstorage.core.resource.RevisionedReadCache;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.core.resource.ResourceTransferAction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A slot-local AE2 view over one player's Xianqiao storage.
 *
 * <p>Wrappers deliberately have identity equality. AE2 requires a distinct
 * {@link StorageCell} object for each mounted slot, while the grid service
 * elects only one active wrapper for each owner to prevent double counting.</p>
 */
final class XianqiaoExchangeStorageCell implements StorageCell {
    /**
     * Both supported AE2 lines expose long-valued MEStorage and KeyCounter
     * amounts.  Keep the optimistic default for direct unit construction; the
     * guarded bootstrap replaces it with false only after the runtime probe
     * positively fails.
     */
    private static volatile boolean ae2LongAmountApiSupported = true;
    private static final RevisionStamp OFFLINE_STAMP =
            new RevisionStamp(false, 0, 0L, false, 0L, false, 0L);
    private static final EndpointResolver LIVE_ENDPOINT_RESOLVER = (server, owner) ->
            server == null ? null : PersonalStorageApi.resolveXianqiao(server, owner);

    private final UUID owner;
    private final UUID diskId;
    private final Component description;
    private final EndpointResolver endpointResolver;
    private final RevisionedReadCache<RevisionStamp, StorageSnapshot> snapshotCache =
            new RevisionedReadCache<>();

    private volatile boolean active;
    private MinecraftServer cachedServer;
    private PersonalStorageEndpoint cachedEndpoint;

    XianqiaoExchangeStorageCell(UUID owner, UUID diskId, Component description) {
        this(owner, diskId, description, LIVE_ENDPOINT_RESOLVER);
    }

    XianqiaoExchangeStorageCell(
            UUID owner,
            UUID diskId,
            Component description,
            EndpointResolver endpointResolver) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.diskId = Objects.requireNonNull(diskId, "diskId");
        this.description = Objects.requireNonNull(description, "description").copy();
        this.endpointResolver = Objects.requireNonNull(endpointResolver, "endpointResolver");
    }

    UUID owner() {
        return owner;
    }

    UUID diskId() {
        return diskId;
    }

    boolean setActive(boolean active) {
        if (this.active == active) return false;
        this.active = active;
        invalidateSnapshotCache();
        return true;
    }

    void invalidateSnapshotCache() {
        clearSnapshotCache();
    }

    RevisionStamp revisionStamp() {
        // The grid service calls this once per 20-tick reconciliation. Force
        // a fresh owner lookup here so logout/login, attachment replacement,
        // and the stage-7 fluid boundary replace the cached endpoint. Hot ME
        // insert/extract calls can then reuse its guarded handlers.
        EndpointAccess access = resolveAccess(true);
        return access == null ? OFFLINE_STAMP : access.stamp();
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        if (!active || what == null) return false;
        return snapshot().amounts().containsKey(what);
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(what, amount, mode, source);
        if (!active || amount == 0L) return 0L;

        EndpointAccess access = resolveAccess(false);
        if (access == null) return 0L;
        TerminalStorageAction action = terminalAction(mode);
        long inserted;
        if (what instanceof AEItemKey itemKey) {
            inserted = access.items().insert(TerminalEntryKey.of(itemKey.toStack()), amount, action);
        } else if (what instanceof AEFluidKey fluidKey && access.fluids() != null) {
            inserted = access.fluids().insert(
                    TerminalFluidKey.of(fluidKey.toStack(1)), amount, action);
        } else if (access.externalResources() != null) {
            ResourceChannelKey key = Ae2ExternalKeyBridges.toResourceKey(what);
            inserted = key == null ? 0L : access.externalResources().insert(
                    key, amount, resourceAction(mode));
        } else {
            return 0L;
        }

        long bounded = boundTransfer(inserted, amount);
        if (!mode.isSimulate() && bounded > 0L) invalidateSnapshotCache();
        return bounded;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(what, amount, mode, source);
        if (!active || amount == 0L) return 0L;

        EndpointAccess access = resolveAccess(false);
        if (access == null) return 0L;
        TerminalStorageAction action = terminalAction(mode);
        long extracted;
        if (what instanceof AEItemKey itemKey) {
            extracted = access.items().extract(TerminalEntryKey.of(itemKey.toStack()), amount, action);
        } else if (what instanceof AEFluidKey fluidKey && access.fluids() != null) {
            extracted = access.fluids().extract(
                    TerminalFluidKey.of(fluidKey.toStack(1)), amount, action);
        } else if (access.externalResources() != null) {
            ResourceChannelKey key = Ae2ExternalKeyBridges.toResourceKey(what);
            extracted = key == null ? 0L : access.externalResources().extract(
                    key, amount, resourceAction(mode));
        } else {
            return 0L;
        }

        long bounded = boundTransfer(extracted, amount);
        if (!mode.isSimulate() && bounded > 0L) invalidateSnapshotCache();
        return bounded;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        Objects.requireNonNull(out, "out");
        if (!active) return;
        snapshot().amounts().forEach((key, amount) -> addSaturated(out, key, amount));
    }

    @Override
    public Component getDescription() {
        return description.copy();
    }

    @Override
    public CellState getStatus() {
        if (!active) return CellState.FULL;
        StorageSnapshot snapshot = snapshot();
        if (!snapshot.stamp().online()) return CellState.EMPTY;
        return snapshot.amounts().isEmpty() ? CellState.EMPTY : CellState.NOT_EMPTY;
    }

    @Override
    public double getIdleDrain() {
        return 0.0D;
    }

    @Override
    public boolean canFitInsideCell() {
        return false;
    }

    @Override
    public void persist() {
        // The owner-scoped backend persists independently of the link item.
    }

    private StorageSnapshot snapshot() {
        EndpointAccess access = resolveReadAccess();
        RevisionStamp stamp = access == null ? OFFLINE_STAMP : access.stamp();
        return snapshotCache.get(stamp, () -> buildSnapshot(access, stamp));
    }

    private StorageSnapshot buildSnapshot(
            @Nullable EndpointAccess access, RevisionStamp stamp) {
        if (access == null) {
            return new StorageSnapshot(stamp, Map.of());
        }

        Map<AEKey, Long> amounts = new HashMap<>();
        for (StorageItemSummary entry : access.items().snapshot()) {
            AEItemKey key = AEItemKey.of(entry.prototype());
            if (key != null && entry.amount() > 0L) {
                amounts.merge(key, entry.amount(), XianqiaoExchangeStorageCell::saturatedSum);
            }
        }
        if (access.fluids() != null) {
            access.fluids().snapshot().forEach((fluid, amount) -> {
                if (fluid == null || amount == null || amount <= 0L) return;
                AEFluidKey key = AEFluidKey.of(fluid.prototype());
                if (key != null) {
                    amounts.merge(key, amount, XianqiaoExchangeStorageCell::saturatedSum);
                }
            });
        }
        if (access.externalResources() != null) {
            access.externalResources().snapshot().forEach(entry -> {
                AEKey key = Ae2ExternalKeyBridges.toAeKey(entry.key());
                if (key != null && entry.amount() > 0L) {
                    amounts.merge(key, entry.amount(), XianqiaoExchangeStorageCell::saturatedSum);
                }
            });
        }
        return new StorageSnapshot(stamp, Collections.unmodifiableMap(amounts));
    }

    private @Nullable EndpointAccess resolveReadAccess() {
        EndpointAccess access = resolveAccess(false);
        // A cached endpoint is guarded by the current owner/data identity. If
        // that guard closes (logout or attachment replacement), refresh once
        // so the next long read can observe the replacement immediately.
        return access != null ? access : resolveAccess(true);
    }

    private @Nullable EndpointAccess resolveAccess(boolean forceRefresh) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        PersonalStorageEndpoint endpoint;
        synchronized (this) {
            if (forceRefresh || cachedServer != server || cachedEndpoint == null) {
                cachedServer = server;
                cachedEndpoint = endpointResolver.resolve(server, owner);
            }
            endpoint = cachedEndpoint;
        }
        if (endpoint == null || !endpoint.online() || !owner.equals(endpoint.owner())) return null;
        TerminalItemStorage items = endpoint.itemStorage();
        if (items == null) return null;
        TerminalFluidStorage fluids = endpoint.fluidStorage();
        ExternalResourceStorage externalResources = endpoint.externalResourceStorage();
        RevisionStamp stamp = new RevisionStamp(
                true,
                endpoint.stage(),
                items.revision(),
                fluids != null,
                fluids == null ? 0L : fluids.revision(),
                externalResources != null,
                externalResources == null ? 0L : externalResources.revision());
        return new EndpointAccess(items, fluids, externalResources, stamp);
    }

    private synchronized void clearSnapshotCache() {
        snapshotCache.invalidate();
    }

    private static TerminalStorageAction terminalAction(Actionable mode) {
        return mode.isSimulate() ? TerminalStorageAction.SIMULATE : TerminalStorageAction.EXECUTE;
    }

    private static ResourceTransferAction resourceAction(Actionable mode) {
        return mode.isSimulate() ? ResourceTransferAction.SIMULATE : ResourceTransferAction.EXECUTE;
    }

    private static long boundTransfer(long transferred, long requested) {
        if (transferred <= 0L) return 0L;
        return Math.min(transferred, requested);
    }

    private static long saturatedSum(long left, long right) {
        if (left < 0L || right < 0L || left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    static void addSaturated(KeyCounter out, AEKey key, long amount) {
        if (amount <= 0L) return;
        // Long.MAX_VALUE is ImmortalStorage's non-consuming/infinite sentinel.
        // AE2's supported MEStorage/KeyCounter contract is long-valued, so do
        // not silently shrink it.  The int ceiling is retained only for a
        // target whose runtime probe has positively shown that it cannot carry
        // long amounts.
        long advertised = ae2LongAmountApiSupported || amount != Long.MAX_VALUE
                ? amount : Integer.MAX_VALUE;
        long current = out.get(key);
        if (current < 0L || current > Long.MAX_VALUE - advertised) {
            out.set(key, Long.MAX_VALUE);
        } else {
            out.add(key, advertised);
        }
    }

    static void setLongAmountApiSupported(boolean supported) {
        ae2LongAmountApiSupported = supported;
    }

    static boolean longAmountApiSupported() {
        return ae2LongAmountApiSupported;
    }

    record RevisionStamp(
            boolean online,
            int stage,
            long itemRevision,
            boolean fluidsAvailable,
            long fluidRevision,
            boolean externalResourcesAvailable,
            long externalResourceRevision) {}

    private record EndpointAccess(
            TerminalItemStorage items,
            @Nullable TerminalFluidStorage fluids,
            @Nullable ExternalResourceStorage externalResources,
            RevisionStamp stamp) {}

    private record StorageSnapshot(RevisionStamp stamp, Map<AEKey, Long> amounts) {}

    @FunctionalInterface
    interface EndpointResolver {
        @Nullable PersonalStorageEndpoint resolve(@Nullable MinecraftServer server, UUID owner);
    }
}
