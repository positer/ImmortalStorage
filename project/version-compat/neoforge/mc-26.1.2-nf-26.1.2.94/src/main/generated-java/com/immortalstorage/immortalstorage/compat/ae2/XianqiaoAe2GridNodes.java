package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.AECapabilities;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.storage.IStorageProvider;
import appeng.api.util.AECableType;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * AE2 grid-node bridge for the Xianqiao Manager.
 *
 * <p><b>Verified against {@code appliedenergistics2-19.2.17.jar} (1.21.1 line) with
 * {@code javap}; every signature used here is quoted in
 * {@code archive/...-ae2-node-bridge/} of the development hub.</b></p>
 *
 * <ul>
 *   <li>{@code IInWorldGridNodeHost { IGridNode getGridNode(Direction); default AECableType
 *       getCableConnectionType(Direction) }} - the default returns {@code AECableType.GLASS}.</li>
 *   <li>{@code GridHelper.createManagedNode(T owner, IGridNodeListener<T> listener)}.</li>
 *   <li>{@code IManagedGridNode}: {@code create(Level, BlockPos)}, {@code destroy()},
 *       {@code loadFromNBT(CompoundTag)}, {@code saveToNBT(CompoundTag)},
 *       {@code setFlags(GridFlags...)}, {@code setExposedOnSides(Set<Direction>)},
 *       {@code setInWorldNode(boolean)}, {@code setVisualRepresentation(ItemLike)},
 *       {@code addService(Class&lt;T extends IGridNodeService&gt;, T)}, {@code getNode()},
 *       {@code getGrid()}.</li>
 *   <li>{@code IGridNodeListener<T>} has one abstract method,
 *       {@code onSaveChanges(T, IGridNode)}; there is <b>no</b> {@code IGridNodeListener.Empty}
 *       in 19.2.17 (the nested types are only {@code State}).</li>
 * </ul>
 *
 * <p><b>Discovery is capability-based, not interface-based.</b> {@code GridHelper.getNodeHost}
 * in 19.2.17 reads
 * {@code Level#getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null)} and never
 * performs an {@code instanceof IInWorldGridNodeHost} check, so a conditional mixin that only
 * makes the block entity <i>implement</i> the interface would never be discovered. The
 * capability registration in {@link #registerCapabilities(RegisterCapabilitiesEvent)} is the
 * functional half of the integration; the mixin supplies the lifecycle only.</p>
 *
 * <p><b>Activation (owner-of-shared-files):</b> {@code Ae2Compat.registerCapabilities} must call
 * {@code XianqiaoAe2GridNodes.registerCapabilities(event)} and
 * {@code META-INF/neoforge.mods.toml} must declare the {@code immortalstorage.ae2.mixins.json}
 * config. Both files sit outside this agent's write scope; the exact lines are in the report.</p>
 */
public final class XianqiaoAe2GridNodes {
    /**
     * Flags for a single-block machine node, copied from AE2's own
     * {@code DriveBlockEntity.createMainNode} ({@code setFlags(GridFlags.REQUIRE_CHANNEL)}).
     * Consequence, not yet verified at runtime: a network without a free channel leaves the
     * manager node inactive.
     */
    private static final GridFlags[] NODE_FLAGS = {GridFlags.REQUIRE_CHANNEL};

    private XianqiaoAe2GridNodes() {}

    /**
     * Registers the in-world node host capability for the Xianqiao Manager.
     *
     * <p>Must be invoked from the AE2-gated {@code RegisterCapabilitiesEvent} listener; NeoForge
     * accepts block-entity capabilities only during that event.</p>
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.XIANQIAO_MANAGER.get(),
                (manager, side) -> hostOf(manager));
        ImmortalStorageMod.LOG.info(
                "[Compat/AE2] Xianqiao manager grid-node capability registered");
    }

    /** Capability lookup body; returns the host only when the AE2 mixin has attached one. */
    public static @Nullable IInWorldGridNodeHost hostOf(XianqiaoManagerBlockEntity manager) {
        if (manager instanceof XianqiaoAe2NodeAccess access) {
            Host host = (Host) access.immortalstorage$ae2NodeHost();
            if (host == null) {
                ImmortalStorageMod.LOG.warn(
                        "[Compat/AE2] capability queried but the mixin attached no host at {}",
                        manager.getBlockPos());
            }
            return host;
        }
        // The interface was not mixed in: AE2 will treat the manager as a plain block.
        ImmortalStorageMod.LOG.warn(
                "[Compat/AE2] capability queried but XianqiaoManagerBlockEntity is not mixed in "
                        + "(the AE2 mixin did not apply); AE2 cannot see {}",
                manager.getBlockPos());
        return null;
    }

    /** Called from the mixin constructor hook; creates the host but never the grid node. */
    public static Host create(XianqiaoManagerBlockEntity manager) {
        return new Host(manager);
    }

    /** Called from the mixin {@code setRemoved} hook. */
    public static void detach(XianqiaoManagerBlockEntity manager) {
        Host host = hostOf(manager) instanceof Host cast ? cast : null;
        if (host != null) host.destroy();
    }

    /** Called from the mixin {@code saveAdditional} hook. */
    public static void save(XianqiaoManagerBlockEntity manager, CompoundTag tag) {
        if (hostOf(manager) instanceof Host host) host.save(tag);
    }

    /** Called from the mixin {@code loadAdditional} hook. */
    public static void load(XianqiaoManagerBlockEntity manager, CompoundTag tag) {
        if (hostOf(manager) instanceof Host host) host.load(tag);
    }

    /** Called from the mixin {@code serverTick} hook: lazy node creation plus service reconciliation. */
    public static void tick(XianqiaoManagerBlockEntity manager) {
        if (!(manager instanceof XianqiaoAe2NodeAccess access)) return;
        Host host = access.immortalstorage$ae2NodeHost() instanceof Host existing ? existing : null;
        if (host == null || host.isDestroyed()) {
            // A block-entity rebuild (chunk reload, a neighbouring break, etc.) calls setRemoved,
            // which destroys the node; without a revive the manager would stay invisible to AE2 for
            // the rest of the session — measured as "adjacent connections drop after one break
            // while cable connections keep working".
            host = new Host(manager);
            access.immortalstorage$ae2SetNodeHost(host);
        }
        host.tick();
    }

    /**
     * One managed grid node plus the two node services that make "connected manager equals
     * connected Xianqiao disk".
     */
    public static final class Host implements IInWorldGridNodeHost {
        private final XianqiaoManagerBlockEntity manager;
        private final IManagedGridNode node;
        private final XianqiaoAe2StorageProvider storageProvider;
        private final XianqiaoAe2CraftingProvider craftingProvider;
        private boolean createRequested;
        private boolean destroyed;

        /** True once {@code setRemoved} destroyed the node; the tick hook then builds a new host. */
        boolean isDestroyed() {
            return destroyed;
        }

        private Host(XianqiaoManagerBlockEntity manager) {
            this.manager = manager;
            this.storageProvider = new XianqiaoAe2StorageProvider(manager);
            this.craftingProvider = new XianqiaoAe2CraftingProvider(manager);
            // A failure here used to abort the whole host construction and — because nothing
            // guarded it — left the block entity without a node while the mixin still believed
            // it had created one. Capture it, log it once and stay node-less instead; the grid
            // simply never sees this manager, which is far better than a crash.
            IManagedGridNode created = null;
            try {
                created = GridHelper.createManagedNode(manager, LISTENER)
                        .setInWorldNode(true)
                        .setExposedOnSides(EnumSet.allOf(Direction.class))
                        .setFlags(NODE_FLAGS)
                        .setVisualRepresentation(ModBlocks.XIANQIAO_MANAGER.get())
                        .addService(IStorageProvider.class, this.storageProvider)
                        .addService(ICraftingProvider.class, this.craftingProvider);
            } catch (Throwable failure) {
                ImmortalStorageMod.LOG.error(
                        "[Compat/AE2] could not create the grid node for the Xianqiao manager; "
                                + "AE2 integration stays disabled for this block", failure);
            }
            this.node = created;
        }

        @Override
        public @Nullable IGridNode getGridNode(Direction direction) {
            return destroyed || node == null ? null : node.getNode();
        }

        /** AE2's own networked block entities report {@code SMART}; the interface default is GLASS. */
        @Override
        public AECableType getCableConnectionType(Direction direction) {
            return AECableType.SMART;
        }

        IManagedGridNode node() {
            return node;
        }

        private void createIfNeeded() {
            if (destroyed || createRequested || node == null) return;
            Level level = manager.getLevel();
            if (level == null) return;
            createRequested = true;
            try {
                node.create(level, manager.getBlockPos());
            } catch (Throwable failure) {
                ImmortalStorageMod.LOG.error(
                        "[Compat/AE2] the grid node refused to join the level; AE2 integration "
                                + "stays disabled for this block", failure);
            }
        }

        private void tick() {
            if (destroyed) return;
            createIfNeeded();
            if (node == null) return;
            try {
                storageProvider.reconcile(node);
                craftingProvider.reconcile(node);
            } catch (Throwable failure) {
                ImmortalStorageMod.LOG.error("[Compat/AE2] node reconciliation failed", failure);
            }
        }

        private void save(CompoundTag tag) {
            if (destroyed) return;
            // 19.2.17 exposes saveToNBT/loadFromNBT; the 26.1 AE2 beta replaced that pair with
            // NeoForge ValueIO (serialize(ValueOutput)/deserialize(ValueInput)). Reflection keeps
            // a single source for both lines: when the older pair is absent the node is simply
            // rebuilt on the next create(), costing one grid join and no player-visible data.
            try {
                node.getClass().getMethod("saveToNBT", CompoundTag.class).invoke(node, tag);
            } catch (ReflectiveOperationException absentOnThisLine) {
                // Intentionally nothing persisted on this target line; see above.
            } catch (RuntimeException unexpected) {
                ImmortalStorageMod.LOG.warn("[Compat/AE2] could not persist the grid node", unexpected);
            }
        }

        private void load(CompoundTag tag) {
            if (destroyed) return;
            try {
                node.getClass().getMethod("loadFromNBT", CompoundTag.class).invoke(node, tag);
            } catch (ReflectiveOperationException absentOnThisLine) {
                // Nothing to restore on this target line; see save() above.
            } catch (RuntimeException unexpected) {
                ImmortalStorageMod.LOG.warn("[Compat/AE2] could not restore the grid node", unexpected);
            }
        }

        private void destroy() {
            if (destroyed) return;
            destroyed = true;
            if (node == null) return;
            try {
                node.destroy();
            } catch (Throwable failure) {
                ImmortalStorageMod.LOG.warn("[Compat/AE2] node teardown failed", failure);
            }
        }
    }

    /**
     * One listener for every manager. {@code onSaveChanges} is the only abstract member;
     * the remaining callbacks are AE2 defaults.
     */
    private static final IGridNodeListener<XianqiaoManagerBlockEntity> LISTENER =
            new IGridNodeListener<>() {
                @Override
                public void onSaveChanges(XianqiaoManagerBlockEntity owner, IGridNode node) {
                    owner.setChanged();
                }
            };

    static {
        // Fail loudly at class load, not in the middle of the first grid tick, when the
        // expected AE2 API line is not the one we compiled against.
        try {
            Class.forName("appeng.api.networking.IInWorldGridNodeHost",
                    false, XianqiaoAe2GridNodes.class.getClassLoader());
        } catch (ClassNotFoundException error) {
            throw new IllegalStateException(
                    "[Compat/AE2] Xianqiao grid-node bridge loaded without AE2 present", error);
        }
        ImmortalStorageMod.LOG.debug("[Compat/AE2] Xianqiao manager grid-node bridge loaded");
    }
}
