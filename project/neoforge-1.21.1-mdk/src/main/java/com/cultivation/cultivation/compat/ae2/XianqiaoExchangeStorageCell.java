package com.cultivation.cultivation.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.StorageCell;
import com.cultivation.cultivation.api.storage.ExternalResourceStorage;
import com.cultivation.cultivation.api.storage.PersonalStorageApi;
import com.cultivation.cultivation.api.storage.PersonalStorageEndpoint;
import com.cultivation.cultivation.api.storage.terminal.StorageItemSummary;
import com.cultivation.cultivation.api.storage.terminal.TerminalEntryKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalFluidKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalFluidStorage;
import com.cultivation.cultivation.api.storage.terminal.TerminalItemStorage;
import com.cultivation.cultivation.api.storage.terminal.TerminalStorageAction;
import com.cultivation.core.resource.ResourceChannelKey;
import com.cultivation.core.resource.ResourceTransferAction;
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
    private static final long AE2_INFINITE_DISPLAY_AMOUNT = Integer.MAX_VALUE;
    private static final RevisionStamp OFFLINE_STAMP =
            new RevisionStamp(false, 0, 0L, false, 0L, false, 0L);
    private static final EndpointResolver LIVE_ENDPOINT_RESOLVER = (server, owner) ->
            server == null ? null : PersonalStorageApi.resolveXianqiao(server, owner);

    private final UUID owner;
    private final UUID diskId;
    private final Component description;
    private final EndpointResolver endpointResolver;

    private volatile boolean active;
    private MinecraftServer cachedServer;
    private PersonalStorageEndpoint cachedEndpoint;
    private RevisionStamp cachedStamp;
    private Map<AEKey, Long> cachedAmounts = Map.of();

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
        synchronized (this) {
            if (cachedStamp != null) return new StorageSnapshot(cachedStamp, cachedAmounts);
        }

        EndpointAccess access = resolveAccess(false);
        RevisionStamp stamp = access == null ? OFFLINE_STAMP : access.stamp();
        synchronized (this) {
            // All normal calls run on the server thread, but retain the first
            // completed snapshot if an integration invokes this concurrently.
            if (cachedStamp != null) return new StorageSnapshot(cachedStamp, cachedAmounts);
            if (access == null) {
                cachedStamp = stamp;
                cachedAmounts = Map.of();
                return new StorageSnapshot(stamp, cachedAmounts);
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
            cachedStamp = stamp;
            cachedAmounts = Collections.unmodifiableMap(amounts);
            return new StorageSnapshot(stamp, cachedAmounts);
        }
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
        cachedStamp = null;
        cachedAmounts = Map.of();
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
        // Long.MAX_VALUE is Cultivation's non-consuming/infinite sentinel. Do
        // not add that sentinel directly to AE2's shared KeyCounter: another
        // mounted storage could then overflow the signed long. Advertising the
        // broad int-compatible ceiling keeps legacy consumers safe; insert and
        // extract still forward their original long request to the backend.
        long advertised = amount == Long.MAX_VALUE ? AE2_INFINITE_DISPLAY_AMOUNT : amount;
        long current = out.get(key);
        if (current < 0L || current > Long.MAX_VALUE - advertised) {
            out.set(key, Long.MAX_VALUE);
        } else {
            out.add(key, advertised);
        }
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
