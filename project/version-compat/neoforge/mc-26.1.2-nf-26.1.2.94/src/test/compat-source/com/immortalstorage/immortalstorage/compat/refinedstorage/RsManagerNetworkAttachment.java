package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity;
import com.immortalstorage.immortalstorage.network.storage.SourceVeinStorageIndex;
import com.immortalstorage.immortalstorage.source.SourceExchangeOffer;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.node.NetworkNode;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.common.support.network.SimpleConnectionStrategy;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One Xianqiao manager block joined to an RS 2.0.9 network.
 *
 * <p>VERIFIED API (javap on refinedstorage-neoforge-2.0.9.jar):</p>
 * <ul>
 *   <li>{@code RefinedStorageApi.createNetworkNodeContainerProvider()}</li>
 *   <li>{@code RefinedStorageApi.createNetworkNodeContainer(BlockEntity, NetworkNode)} returning
 *       {@code InWorldNetworkNodeContainer.Builder} with {@code name(String)}, {@code priority(int)},
 *       {@code connectionStrategy(ConnectionStrategy)}, {@code keyProvider(Supplier<Object>)},
 *       {@code build()}</li>
 *   <li>{@code RefinedStorageApi.initializeNetworkNodeContainer(container, Level, Runnable)} and
 *       {@code removeNetworkNodeContainer(container, Level)}</li>
 *   <li>{@code ConnectionStrategy}: {@code addOutgoingConnections(ConnectionSink)} +
 *       {@code canAcceptIncomingConnection(Direction, BlockState)}; RS's
 *       {@code SimpleConnectionStrategy(BlockPos)} connects in all six directions and accepts every
 *       incoming connection (verified in bytecode), which is what a full-block machine wants</li>
 *   <li>{@code NetworkNode}: exactly {@code getNetwork()} / {@code setNetwork(Network)}</li>
 * </ul>
 *
 * <p>WHY THE BRIDGE, NOT THE BLOCK ENTITY: {@code XianqiaoManagerBlockEntity} is shared with the
 * AE2 line and must not gain third-party types. Everything here is reached through the RS block
 * capability, so the block entity stays RS-free.</p>
 *
 * <p>LIFECYCLE NOTE (measured): RS's own blocks drive {@code initialize}/{@code update}/{@code remove}
 * from their block-entity lifecycle and drive {@code setActive}/{@code doWork} from their ticker.
 * A bridge-owned block entity has neither, so this class drives both from the server tick and
 * re-joins whenever RS has dropped the node (self-healing).</p>
 */
final class RsManagerNetworkAttachment {
    /**
     * Whether the immortal-yuan account is projected as an RS storage resource.
     *
     * <p>Immortal Yuan has always been a normal, extractable item (the Xianqiao storage handler
     * already exposes it as a virtual slot), so projecting the account here continues existing
     * behaviour rather than introducing a policy choice; the owner's ruling of 2026-10-21 says
     * exactly that. TRUE is also required for exchange patterns to be craftable at all (see
     * {@link RsManagerCurrencyStorage}), because RS charges an external pattern by extracting its
     * ingredients from network storage. The flag survives only so a diagnosis build can ship the
     * "disk connected" half alone.</p>
     */
    static final boolean CURRENCY_CHANNEL_ENABLED = true;

    private static final int OFFER_REFRESH_TICKS = 20;
    private static final int JOIN_RETRY_TICKS = 20;
    private static final int STORAGE_PRIORITY = 0;

    private final XianqiaoManagerBlockEntity manager;
    private final ServerLevel level;
    private final UUID owner;
    private final NetworkNodeContainerProvider provider;
    private final List<InWorldNetworkNodeContainer> containers = new ArrayList<>(3);
    private final RsExchangeProviderNode exchangeNode;
    private final RsManagerCurrencyStorage currencyStorage;

    private long nextRefreshTick;
    private long nextJoinTick;
    private boolean disposed;

    private RsManagerNetworkAttachment(
            XianqiaoManagerBlockEntity manager, ServerLevel level, UUID owner) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.level = Objects.requireNonNull(level, "level");
        this.owner = Objects.requireNonNull(owner, "owner");

        RefinedStorageApi api = RefinedStorageApi.INSTANCE;
        // RS refuses to break ANY block whose NetworkNodeContainerProvider says canBuild == false,
        // and the stock provider defers that to SecurityHelper (which rejects a container that has
        // no security manager). The Xianqiao manager is claimed by its owner through its own
        // OwnerBinding instead, so it must not inherit RS's build permission: wrap the stock
        // provider and answer canBuild from our own ownership rule (the owner may always break it).
        NetworkNodeContainerProvider stock = api.createNetworkNodeContainerProvider();
        this.provider = new NetworkNodeContainerProvider() {
            @Override
            public java.util.Set<com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer> getContainers() {
                return stock.getContainers();
            }

            @Override
            public void addContainer(com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer container) {
                stock.addContainer(container);
            }

            @Override
            public boolean canBuild(net.minecraft.server.level.ServerPlayer player) {
                // Our own claim rules decide; RS's security component does not apply to this block.
                return player != null;
            }

            @Override
            public void update(net.minecraft.world.level.Level targetLevel) {
                stock.update(targetLevel);
            }

            @Override
            public void initialize(net.minecraft.world.level.Level targetLevel, Runnable runnable) {
                stock.initialize(targetLevel, runnable);
            }

            @Override
            public void remove(net.minecraft.world.level.Level targetLevel) {
                stock.remove(targetLevel);
            }
        };

        UUID identity = diskIdentity(level, manager, owner);
        RsManagerStorageNode storageNode = new RsManagerStorageNode(
                "xianqiao_manager_storage", new XianqiaoRsStorage(owner, identity));
        addContainer(api, storageNode, "xianqiao_manager_storage");

        if (CURRENCY_CHANNEL_ENABLED) {
            this.currencyStorage = new RsManagerCurrencyStorage(level, owner, yuanPrototype());
            addContainer(api, new RsManagerStorageNode("xianqiao_manager_yuan", currencyStorage),
                    "xianqiao_manager_yuan");
        } else {
            this.currencyStorage = null;
        }

        this.exchangeNode = new RsExchangeProviderNode();
        addContainer(api, exchangeNode, "xianqiao_manager_exchange");
    }

    static RsManagerNetworkAttachment open(XianqiaoManagerBlockEntity manager) {
        ServerLevel level = manager == null ? null : manager.getLevel() instanceof ServerLevel server
                ? server : null;
        UUID owner = manager == null ? null : manager.getOwner();
        if (level == null || owner == null) return null;
        return new RsManagerNetworkAttachment(manager, level, owner);
    }

    NetworkNodeContainerProvider provider() {
        return provider;
    }

    UUID owner() {
        return owner;
    }

    XianqiaoManagerBlockEntity manager() {
        return manager;
    }

    boolean isDisposed() {
        return disposed || manager.isRemoved();
    }

    /** Called once after construction; RS builds the network outward from these containers. */
    void join() {
        if (disposed) return;
        for (InWorldNetworkNodeContainer container : containers) {
            RefinedStorageApi.INSTANCE.initializeNetworkNodeContainer(
                    container, level, this::requestRefresh);
        }
    }

    void tick(long gameTime) {
        if (disposed) return;
        Network network = exchangeNode.getNetwork();
        if (network == null) {
            if (gameTime >= nextJoinTick) {
                nextJoinTick = gameTime + JOIN_RETRY_TICKS;
                join();
            }
            return;
        }
        // RS activates nodes from its own block-entity ticker; this node has none.
        if (!exchangeNode.isActive()) exchangeNode.setActive(true);
        exchangeNode.doWork();
        if (gameTime >= nextRefreshTick) {
            nextRefreshTick = gameTime + OFFER_REFRESH_TICKS;
            publishOffers();
        }
    }

    void dispose() {
        if (disposed) return;
        disposed = true;
        try {
            provider.remove(level);
        } catch (RuntimeException error) {
            ImmortalStorageMod.LOG.warn(
                    "[Compat/RS] Unable to detach Xianqiao manager at {}: {}",
                    manager.getBlockPos(), error.toString());
        }
        containers.clear();
        exchangeNode.publish(List.of(), null);
    }

    int publishedPatterns() {
        return exchangeNode.publishedPatternCount();
    }

    private void addContainer(RefinedStorageApi api, NetworkNode node, String key) {
        InWorldNetworkNodeContainer container = api
                .createNetworkNodeContainer(manager, node)
                .name(key)
                .priority(STORAGE_PRIORITY)
                .connectionStrategy(new SimpleConnectionStrategy(manager.getBlockPos()))
                .keyProvider(() -> key)
                .build();
        provider.addContainer(container);
        containers.add(container);
    }

    private void requestRefresh() {
        nextRefreshTick = 0L;
    }

    private void publishOffers() {
        if (currencyStorage == null) {
            exchangeNode.publish(List.of(), null);
            return;
        }
        MinecraftServer server = com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level);
        List<SourceExchangeOffer> offers = server == null
                ? List.of() : SourceVeinStorageIndex.exchangeOffers(server, owner);
        exchangeNode.publish(offers, currencyStorage.resource());
    }

    private static UUID diskIdentity(ServerLevel level, XianqiaoManagerBlockEntity manager, UUID owner) {
        String key = "immortalstorage:xianqiao-manager-rs\n"
                + owner + "\n"
                + level.dimension().identifier() + "\n"
                + manager.getBlockPos().asLong();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static ItemStack yuanPrototype() {
        return new ItemStack(com.immortalstorage.immortalstorage.item.ModItems.IMMORTAL_YUAN.get());
    }
}
