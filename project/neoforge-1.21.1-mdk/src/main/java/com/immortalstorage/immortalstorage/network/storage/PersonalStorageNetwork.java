package com.immortalstorage.immortalstorage.network.storage;

import com.immortalstorage.immortalstorage.api.storage.ExternalResourceStorage;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.core.resource.ResourceChannelEntry;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.core.resource.ResourceTransferAction;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Owner-scoped storage endpoint registry for Xianqiao/Kongqiao automation.
 *
 * The backing data still lives on ImmortalStoragePlayerData; this class provides a
 * single registration/lookup surface so future AE2/RS/fluid-network bridges do
 * not each rediscover owner data differently.
 */
public final class PersonalStorageNetwork {
    public static @Nullable Endpoint resolve(MinecraftServer server, @Nullable UUID owner) {
        return resolve(server, owner, null);
    }

    public static @Nullable Endpoint resolve(MinecraftServer server, @Nullable UUID owner, @Nullable Runnable onChanged) {
        if (server == null || owner == null) return null;
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        // Player attachments are the persistent source of truth. Creating an
        // unrelated transient UUID entry while the owner is offline would fork
        // storage and resource state, so offline endpoints are deliberately absent.
        if (player == null) return null;
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        HolderLookup.Provider registryAccess = player.registryAccess();
        if (data.getStage() < 1) return null;
        BooleanSupplier accessAllowed = () -> {
            ServerPlayer currentPlayer = server.getPlayerList().getPlayer(owner);
            return currentPlayer != null
                    && ImmortalStoragePlayerData.get(currentPlayer) == data
                    && data.getStage() >= 1;
        };
        return new Endpoint(owner, data, registryAccess, onChanged, true, accessAllowed, false, server);
    }

    /**
     * Additive fluid-capable lookup for owner-scoped managers and integrations.
     * The legacy {@link #resolve} contract intentionally remains item-only.
     */
    public static @Nullable Endpoint resolveWithFluids(
            MinecraftServer server, @Nullable UUID owner, @Nullable Runnable onChanged) {
        return resolveWithFluids(server, owner, onChanged, () -> true);
    }

    /**
     * Owner-bound Xianqiao-only lookup for interfaces and optional storage
     * networks. The live predicate is retained by every returned handler, so
     * logout, attachment replacement or a debug downgrade fails closed.
     */
    public static @Nullable Endpoint resolveXianqiao(
            MinecraftServer server, @Nullable UUID owner, @Nullable Runnable onChanged) {
        if (server == null || owner == null) return null;
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player == null) return null;
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (data.getStage() < 6) return null;
        BooleanSupplier accessAllowed = () -> {
            ServerPlayer currentPlayer = server.getPlayerList().getPlayer(owner);
            return currentPlayer != null
                    && ImmortalStoragePlayerData.get(currentPlayer) == data
                    && data.getStage() >= 6;
        };
        boolean includeFluids = data.getStage() >= ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE;
        return new Endpoint(owner, data, player.registryAccess(), onChanged,
                true, accessAllowed, includeFluids, server);
    }

    /**
     * Resolves an endpoint only from the exact UUID-bound personal realm.
     * Stage-six owners retain item access; the optional fluid bridge is added
     * only after the stage-seven fluid unlock.
     */
    public static @Nullable Endpoint resolveInOwnerRealm(
            @Nullable ServerLevel realm, @Nullable UUID owner, @Nullable Runnable onChanged) {
        if (realm == null || realm.isClientSide || owner == null || realm.getServer() == null) return null;
        if (!ImmortalStorageDimensions.isPersonalRealmFor(realm.dimension(), owner)) return null;
        MinecraftServer boundServer = realm.getServer();
        ServerPlayer player = boundServer.getPlayerList().getPlayer(owner);
        if (player == null) return null;
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (data.getStage() < 6) return null;
        BooleanSupplier accessAllowed = () -> {
            ServerPlayer currentPlayer = boundServer.getPlayerList().getPlayer(owner);
            return currentPlayer != null
                    && ImmortalStoragePlayerData.get(currentPlayer) == data
                    && data.getStage() >= 6
                    && realm.getServer() == boundServer
                    && ImmortalStorageDimensions.isPersonalRealmFor(realm.dimension(), owner);
        };
        boolean includeFluids = data.getStage() >= ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE;
        return new Endpoint(owner, data, player.registryAccess(), onChanged,
                true, accessAllowed, includeFluids, boundServer);
    }

    private static @Nullable Endpoint resolveWithFluids(
            MinecraftServer server, @Nullable UUID owner, @Nullable Runnable onChanged,
            BooleanSupplier additionalAccessCheck) {
        if (server == null || owner == null) return null;
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player == null) return null;
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (data.getStage() < ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE) return null;
        BooleanSupplier accessAllowed = () -> {
            ServerPlayer currentPlayer = server.getPlayerList().getPlayer(owner);
            return currentPlayer != null
                    && ImmortalStoragePlayerData.get(currentPlayer) == data
                    && data.getStage() >= ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE
                    && additionalAccessCheck.getAsBoolean();
        };
        return new Endpoint(owner, data, player.registryAccess(), onChanged,
                true, accessAllowed, true, server);
    }

    public static final class Endpoint implements PersonalStorageEndpoint {
        private final UUID owner;
        private final ImmortalStoragePlayerData data;
        private final HolderLookup.Provider registryAccess;
        private final Runnable onChanged;
        private final IItemHandler itemHandler;
        private final TerminalItemStorage itemStorage;
        private final @Nullable PersonalStorageFluidHandler fluidHandler;
        private final ExternalResourceStorage externalResourceStorage;
        private final BooleanSupplier accessAllowed;

        private final boolean online;

        public Endpoint(UUID owner, ImmortalStoragePlayerData data, HolderLookup.Provider registryAccess, @Nullable Runnable onChanged) {
            this(owner, data, registryAccess, onChanged, true);
        }

        public Endpoint(UUID owner, ImmortalStoragePlayerData data, HolderLookup.Provider registryAccess, @Nullable Runnable onChanged, boolean online) {
            this(owner, data, registryAccess, onChanged, online,
                    () -> data != null && data.getStage() >= 1, false, null);
        }

        private Endpoint(UUID owner, ImmortalStoragePlayerData data, HolderLookup.Provider registryAccess,
                         @Nullable Runnable onChanged, boolean online,
                         BooleanSupplier accessAllowed, boolean includeFluids,
                         @Nullable MinecraftServer server) {
            this.owner = owner;
            this.data = data;
            this.registryAccess = registryAccess;
            this.onChanged = onChanged == null ? () -> {} : onChanged;
            this.accessAllowed = accessAllowed == null ? () -> false : accessAllowed;
            PersonalStorageLongItemStorage combinedItems = new PersonalStorageLongItemStorage(
                    data, this.onChanged, this.accessAllowed, server, owner);
            this.itemStorage = combinedItems;
            this.itemHandler = new PersonalStorageItemHandler(
                    data, registryAccess, this.onChanged, this.accessAllowed, combinedItems);
            this.fluidHandler = includeFluids
                    ? new PersonalStorageFluidHandler(data, this.onChanged, this.accessAllowed, server, owner)
                    : null;
            this.externalResourceStorage = new ExternalResourceStorage() {
                @Override
                public long revision() {
                    return data.getExternalResourceRevision();
                }

                @Override
                public List<ResourceChannelEntry> snapshot() {
                    return accessAllowed.getAsBoolean() && data.getStage() >= 8
                            ? data.getExternalResourceEntries() : List.of();
                }

                @Override
                public long insert(ResourceChannelKey key, long amount, ResourceTransferAction action) {
                    if (!accessAllowed.getAsBoolean()) return 0L;
                    long inserted = data.insertExternalResource(key, amount, action);
                    if (inserted > 0L && action.executes()) Endpoint.this.onChanged.run();
                    return inserted;
                }

                @Override
                public long extract(ResourceChannelKey key, long amount, ResourceTransferAction action) {
                    if (!accessAllowed.getAsBoolean()) return 0L;
                    long extracted = data.extractExternalResource(key, amount, action);
                    if (extracted > 0L && action.executes()) Endpoint.this.onChanged.run();
                    return extracted;
                }
            };
            this.online = online;
        }

        @Override
        public UUID owner() {
            return owner;
        }

        public ImmortalStoragePlayerData data() {
            return data;
        }

        @Override
        public int stage() {
            return data.getStage();
        }

        @Override
        public boolean online() {
            return online;
        }

        @Override
        public IItemHandler itemHandler() {
            return itemHandler;
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            return itemHandler.insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extract(ItemStack template, int amount, boolean simulate) {
            if (!accessAllowed.getAsBoolean() || template.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            int request = Math.min(amount, Math.max(1, template.getMaxStackSize()));
            long extracted = itemStorage.extract(
                    com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey.of(template), request,
                    simulate
                            ? com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction.SIMULATE
                            : com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction.EXECUTE);
            return extracted <= 0L ? ItemStack.EMPTY
                    : template.copyWithCount((int) Math.min(request, extracted));
        }

        @Override
        public TerminalItemStorage itemStorage() {
            return itemStorage;
        }

        @Override
        public @Nullable TerminalFluidStorage fluidStorage() {
            return fluidHandler;
        }

        @Override
        public @Nullable IFluidHandler fluidHandler() {
            return fluidHandler;
        }

        @Override
        public ExternalResourceStorage externalResourceStorage() {
            return externalResourceStorage;
        }

    }

    private PersonalStorageNetwork() {}
}
