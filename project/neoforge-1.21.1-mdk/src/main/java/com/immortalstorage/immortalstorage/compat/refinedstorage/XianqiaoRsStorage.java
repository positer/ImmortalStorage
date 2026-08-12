package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.immortalstorage.api.storage.PersonalStorageApi;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import com.immortalstorage.immortalstorage.api.storage.ExternalResourceStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeAwareChild;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageType;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.immortalstorage.core.resource.ResourceTransferAction;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.core.resource.RevisionedReadCache;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * RS 2.0.9 long-valued live view over one bound Xianqiao.
 *
 * <p>The concrete RS item/fluid resource keys are isolated here because RS
 * marks them internal even though its stable {@code Storage} API requires a
 * caller to interpret {@code ResourceKey}. This adapter is therefore pinned
 * to the exact 2.0.9 compatibility lane.</p>
 */
final class XianqiaoRsStorage implements SerializableStorage, CompositeAwareChild {
    private final UUID owner;
    private final UUID diskId;
    private final EndpointResolver endpointResolver;
    private final RevisionedReadCache<RevisionStamp, ReadSnapshot> readCache =
            new RevisionedReadCache<>();
    private volatile boolean networkPrimary = true;
    private volatile ParentComposite directParent;

    XianqiaoRsStorage(UUID owner, UUID diskId) {
        this(owner, diskId, (server, requestedOwner) ->
                server == null ? null : PersonalStorageApi.resolveXianqiao(server, requestedOwner));
    }

    XianqiaoRsStorage(UUID owner, UUID diskId, EndpointResolver endpointResolver) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.diskId = Objects.requireNonNull(diskId, "diskId");
        this.endpointResolver = Objects.requireNonNull(endpointResolver, "endpointResolver");
    }

    UUID owner() {
        return owner;
    }

    UUID diskId() {
        return diskId;
    }

    @Override
    public Collection<ResourceAmount> getAll() {
        if (!networkPrimary) return List.of();
        return readSnapshot().entries();
    }

    private ReadSnapshot readSnapshot() {
        EndpointAccess access = resolveAccess();
        RevisionStamp stamp = access == null ? OFFLINE_STAMP : access.stamp();
        return readCache.get(stamp, () -> buildReadSnapshot(access, stamp));
    }

    private ReadSnapshot buildReadSnapshot(
            @Nullable EndpointAccess access, RevisionStamp stamp) {
        if (access == null) return new ReadSnapshot(stamp, List.of(), 0L);

        List<ResourceAmount> result = new ArrayList<>();
        for (StorageItemSummary entry : access.items().snapshot()) {
            long amount = RsAmountPolicy.advertised(entry.amount());
            if (amount > 0L) {
                result.add(new ResourceAmount(ItemResource.ofItemStack(entry.prototype()), amount));
            }
        }
        if (access.fluids() != null) {
            access.fluids().snapshot().forEach((key, stored) -> {
                long amount = RsAmountPolicy.advertised(stored == null ? 0L : stored);
                if (key == null || amount <= 0L) return;
                FluidStack stack = key.prototype();
                result.add(new ResourceAmount(new FluidResource(
                        stack.getFluid(), stack.getComponentsPatch()), amount));
            });
        }
        if (access.externalResources() != null) {
            access.externalResources().snapshot().forEach(entry -> {
                long amount = RsAmountPolicy.advertised(entry.amount());
                if (entry.key() != null && amount > 0L) {
                    ResourceKey resource = RsExternalResourceKeyBridges.toRsKey(entry.key());
                    if (resource != null) result.add(new ResourceAmount(resource, amount));
                }
            });
        }
        // ResourceAmount is immutable, but the fluid and external callbacks
        // above keep their local amount. Recompute the total from the final
        // immutable list so getAll() and getStored() share one exact view.
        long total = 0L;
        for (ResourceAmount entry : result) {
            total = RsAmountPolicy.saturatedSum(total, entry.amount());
        }
        return new ReadSnapshot(stamp, List.copyOf(result), total);
    }

    @Override
    public long getStored() {
        if (!networkPrimary) return 0L;
        return readSnapshot().total();
    }

    @Override
    public long insert(ResourceKey resource, long amount, Action action, Actor actor) {
        if (!networkPrimary || resource == null || amount <= 0L || action == null) return 0L;
        EndpointAccess access = resolveAccess();
        if (access == null) return 0L;

        long inserted;
        TerminalStorageAction terminalAction = terminalAction(action);
        if (resource instanceof ItemResource item) {
            inserted = access.items().insert(
                    TerminalEntryKey.of(item.toItemStack()), amount, terminalAction);
        } else if (resource instanceof FluidResource fluid && access.fluids() != null) {
            FluidStack stack = new FluidStack(
                    BuiltInRegistries.FLUID.wrapAsHolder(fluid.fluid()),
                    1,
                    fluid.components());
            inserted = access.fluids().insert(
                    TerminalFluidKey.of(stack), amount, terminalAction);
        } else if (access.externalResources() != null) {
            ResourceChannelKey external = RsExternalResourceKeyBridges.toResourceKey(resource);
            if (external == null) return 0L;
            inserted = access.externalResources().insert(
                    external, amount, resourceAction(action));
        } else {
            return 0L;
        }
        long bounded = RsAmountPolicy.boundedTransfer(inserted, amount);
        if (bounded > 0L && action == Action.EXECUTE) readCache.invalidate();
        return bounded;
    }

    @Override
    public long extract(ResourceKey resource, long amount, Action action, Actor actor) {
        if (!networkPrimary || resource == null || amount <= 0L || action == null) return 0L;
        EndpointAccess access = resolveAccess();
        if (access == null) return 0L;

        long extracted;
        TerminalStorageAction terminalAction = terminalAction(action);
        if (resource instanceof ItemResource item) {
            extracted = access.items().extract(
                    TerminalEntryKey.of(item.toItemStack()), amount, terminalAction);
        } else if (resource instanceof FluidResource fluid && access.fluids() != null) {
            FluidStack stack = new FluidStack(
                    BuiltInRegistries.FLUID.wrapAsHolder(fluid.fluid()),
                    1,
                    fluid.components());
            extracted = access.fluids().extract(
                    TerminalFluidKey.of(stack), amount, terminalAction);
        } else if (access.externalResources() != null) {
            ResourceChannelKey external = RsExternalResourceKeyBridges.toResourceKey(resource);
            if (external == null) return 0L;
            extracted = access.externalResources().extract(
                    external, amount, resourceAction(action));
        } else {
            return 0L;
        }
        long bounded = RsAmountPolicy.boundedTransfer(extracted, amount);
        if (bounded > 0L && action == Action.EXECUTE) readCache.invalidate();
        return bounded;
    }

    @Override
    public StorageType getType() {
        return RsXianqiaoStorageType.INSTANCE;
    }

    @Override
    public void onAddedIntoComposite(ParentComposite parentComposite) {
        directParent = Objects.requireNonNull(parentComposite, "parentComposite");
        RsNetworkDeduplicator.rebalanceFrom(parentComposite);
    }

    @Override
    public void onRemovedFromComposite(ParentComposite parentComposite) {
        RsNetworkDeduplicator.rebalanceFrom(parentComposite);
        directParent = null;
        setNetworkPrimary(true);
    }

    @Override
    public Amount compositeInsert(
            ResourceKey resource, long amount, Action action, Actor actor) {
        long inserted = insert(resource, amount, action, actor);
        return inserted <= 0L ? Amount.ZERO : new Amount(inserted, inserted);
    }

    @Override
    public Amount compositeExtract(
            ResourceKey resource, long amount, Action action, Actor actor) {
        long extracted = extract(resource, amount, action, actor);
        return extracted <= 0L ? Amount.ZERO : new Amount(extracted, extracted);
    }

    @Override
    public boolean contains(com.refinedmods.refinedstorage.api.storage.Storage storage) {
        return RsNetworkDeduplicator.containsOwner(storage, owner);
    }

    boolean isNetworkPrimary() {
        return networkPrimary;
    }

    void setNetworkPrimary(boolean networkPrimary) {
        if (this.networkPrimary == networkPrimary) return;
        this.networkPrimary = networkPrimary;
        readCache.invalidate();
    }

    ParentComposite directParent() {
        return directParent;
    }

    private @Nullable EndpointAccess resolveAccess() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        PersonalStorageEndpoint endpoint = endpointResolver.resolve(server, owner);
        if (endpoint == null || !endpoint.online() || !owner.equals(endpoint.owner())) return null;
        TerminalItemStorage items = endpoint.itemStorage();
        if (items == null) return null;
        TerminalFluidStorage fluids = endpoint.fluidStorage();
        ExternalResourceStorage externalResources = endpoint.externalResourceStorage();
        return new EndpointAccess(items, fluids, externalResources, new RevisionStamp(
                true,
                endpoint.stage(),
                items.revision(),
                fluids != null,
                fluids == null ? 0L : fluids.revision(),
                externalResources != null,
                externalResources == null ? 0L : externalResources.revision()));
    }

    private static TerminalStorageAction terminalAction(Action action) {
        return action == Action.SIMULATE
                ? TerminalStorageAction.SIMULATE
                : TerminalStorageAction.EXECUTE;
    }

    private static ResourceTransferAction resourceAction(Action action) {
        return action == Action.SIMULATE
                ? ResourceTransferAction.SIMULATE
                : ResourceTransferAction.EXECUTE;
    }

    private record EndpointAccess(
            TerminalItemStorage items,
            @Nullable TerminalFluidStorage fluids,
            @Nullable ExternalResourceStorage externalResources,
            RevisionStamp stamp) {
    }

    private static final RevisionStamp OFFLINE_STAMP =
            new RevisionStamp(false, 0, 0L, false, 0L, false, 0L);

    private record RevisionStamp(
            boolean online,
            int stage,
            long itemRevision,
            boolean fluidsAvailable,
            long fluidRevision,
            boolean externalResourcesAvailable,
            long externalResourceRevision) {
    }

    private record ReadSnapshot(
            RevisionStamp stamp,
            List<ResourceAmount> entries,
            long total) {
    }

    @FunctionalInterface
    interface EndpointResolver {
        @Nullable PersonalStorageEndpoint resolve(@Nullable MinecraftServer server, UUID owner);
    }
}
