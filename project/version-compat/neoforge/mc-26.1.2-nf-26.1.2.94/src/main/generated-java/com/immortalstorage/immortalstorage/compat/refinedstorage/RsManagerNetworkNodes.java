package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Refined Storage 2.0.9 bridge that makes the Xianqiao manager block a network member.
 *
 * <h2>How RS 2.0.9 discovers a modded block (recon, VERIFIED by javap)</h2>
 * <ol>
 *   <li>It is <b>not</b> a registry, not a {@code NetworkManager} and not a
 *       {@code NetworkNodeContainer} registry. A block joins a network through the NeoForge block
 *       capability {@code RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability()},
 *       whose type is
 *       {@code net.neoforged.neoforge.capabilities.BlockCapability<NetworkNodeContainerProvider, Direction>}.</li>
 *   <li>RS scans a neighbour through that capability, gets its
 *       {@code Set<InWorldNetworkNodeContainer>} and links the containers into the graph.
 *       {@code NetworkBuilderImpl.initialize(container, ConnectionProvider)} walks outward from a
 *       container via {@code ConnectionStrategy.addOutgoingConnections}.</li>
 *   <li>Containers must be created with {@code RefinedStorageApi.createNetworkNodeContainer(be, node)}
 *       and must expose {@code ConnectionStrategy} (RS's {@code SimpleConnectionStrategy} connects all
 *       six sides) plus a stable {@code keyProvider}.</li>
 *   <li>The node on a container only has to implement RS's
 *       {@code api.network.node.NetworkNode} ({@code getNetwork}/{@code setNetwork}). To be seen as
 *       storage it additionally implements {@code api.network.storage.StorageProvider}.</li>
 * </ol>
 *
 * <h2>Wiring this class needs (OUTSIDE this package)</h2>
 * <p>{@code RegisterCapabilitiesEvent} is dispatched during mod loading, before
 * {@code InterModEnqueueEvent}, which is when {@code RsCompat.initialize()} runs. The capability
 * body therefore has to be wired from the mod constructor exactly like the AE2 line already is:</p>
 * <pre>
 * // CompatManager.initializeOptionalIntegrations
 * if (RS_LOADED) {
 *     modBus.addListener(CompatManager::initializeRs);
 *     modBus.addListener(CompatManager::registerRsCapabilities);   // add
 * }
 *
 * private static void registerRsCapabilities(RegisterCapabilitiesEvent event) {
 *     invokeOptionalMethod(
 *             "com.immortalstorage.immortalstorage.compat.refinedstorage.RsManagerNetworkNodes",
 *             "registerCapabilities",
 *             new Class&lt;?&gt;[]{RegisterCapabilitiesEvent.class}, event);
 * }
 * </pre>
 * <p>Without that wiring the block is never discovered from RS's side. The tick driver below still
 * re-joins any manager it already knows about, and
 * {@link #noteManager(XianqiaoManagerBlockEntity)} is the single call that would let
 * {@code XianqiaoManagerBlockEntity#serverTick} register one instead - the two wiring options are
 * mutually exclusive and the Lead should pick the capability one.</p>
 */
public final class RsManagerNetworkNodes {
    private static final Map<GlobalPos, RsManagerNetworkAttachment> ATTACHMENTS = new HashMap<>();
    /** Managers whose capability was asked before an owner claimed them. */
    private static final Map<GlobalPos, XianqiaoManagerBlockEntity> PENDING = new HashMap<>();

    private static boolean installed;
    private static volatile boolean capabilityWired;

    private RsManagerNetworkNodes() {
    }

    /**
     * Capability body. Returns {@code null} while the manager has no owner: an unclaimed manager has
     * no storage to share, and RS re-queries the capability when it rebuilds the network.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
                ModBlockEntities.XIANQIAO_MANAGER.get(),
                (manager, side) -> providerFor(manager));
        capabilityWired = true;
        ImmortalStorageMod.LOG.info(
                "[Compat/RS] Xianqiao manager network-node capability registered");
    }

    /**
     * Installs the server tick driver. Called from {@link RsCompat#initialize()} so it only ever runs
     * with RS present.
     */
    public static synchronized void initialize() {
        if (installed) return;
        installed = true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                RsManagerNetworkNodes::onServerTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                RsManagerNetworkNodes::onServerStopped);
        com.immortalstorage.immortalstorage.compat.XianqiaoManagerCompatHooks.register(
                RsManagerNetworkNodes::noteManager);
        ImmortalStorageMod.LOG.info(
                "[Compat/RS] Xianqiao manager network-node bridge installed (capability={})",
                capabilityWired);
    }

    /** True once the block capability is registered; useful for start-up diagnostics only. */
    public static boolean capabilityWired() {
        return capabilityWired;
    }

    /**
     * Alternative discovery hook, for the case where wiring {@link #registerCapabilities} is not
     * possible: a single call from the manager's own server tick. Kept public so the block entity can
     * stay RS-free while still feeding this bridge.
     */
    public static void noteManager(XianqiaoManagerBlockEntity manager) {
        if (!(manager != null && manager.getLevel() instanceof ServerLevel level)) return;
        GlobalPos key = GlobalPos.of(level.dimension(), manager.getBlockPos());
        synchronized (RsManagerNetworkNodes.class) {
            if (ATTACHMENTS.containsKey(key)) return;
        }
        providerFor(manager);
    }

    static synchronized @Nullable NetworkNodeContainerProvider providerFor(
            XianqiaoManagerBlockEntity manager) {
        if (!(manager.getLevel() instanceof ServerLevel level)) return null;
        GlobalPos key = GlobalPos.of(level.dimension(), manager.getBlockPos());
        var owner = manager.getOwner();
        if (owner == null) {
            PENDING.put(key, manager);
            return null;
        }
        RsManagerNetworkAttachment existing = ATTACHMENTS.get(key);
        if (existing != null && !existing.isDisposed()) {
            if (existing.owner().equals(owner)) return existing.provider();
            detach(key, existing);
        }
        PENDING.remove(key);
        RsManagerNetworkAttachment created = RsManagerNetworkAttachment.open(manager);
        if (created == null) return null;
        ATTACHMENTS.put(key, created);
        created.join();
        ImmortalStorageMod.LOG.info(
                "[Compat/RS] Xianqiao manager at {} joined as a network member for owner {}",
                manager.getBlockPos(), owner);
        return created.provider();
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().overworld();
        long gameTime = overworld == null ? 0L : overworld.getGameTime();
        List<GlobalPos> stale = new ArrayList<>();
        List<RsManagerNetworkAttachment> ticked;
        synchronized (RsManagerNetworkNodes.class) {
            claimPending();
            ticked = new ArrayList<>(ATTACHMENTS.values());
            for (Map.Entry<GlobalPos, RsManagerNetworkAttachment> entry : ATTACHMENTS.entrySet()) {
                if (entry.getValue().isDisposed()) stale.add(entry.getKey());
            }
        }
        for (RsManagerNetworkAttachment attachment : ticked) {
            try {
                attachment.tick(gameTime);
            } catch (RuntimeException error) {
                ImmortalStorageMod.LOG.error(
                        "[Compat/RS] Xianqiao manager node tick failed at {}",
                        attachment.manager().getBlockPos(), error);
            }
        }
        if (stale.isEmpty()) return;
        synchronized (RsManagerNetworkNodes.class) {
            for (GlobalPos key : stale) {
                RsManagerNetworkAttachment attachment = ATTACHMENTS.remove(key);
                if (attachment != null) attachment.dispose();
            }
        }
    }

    /** Promotes managers whose owner was claimed after RS first asked for the capability. */
    private static void claimPending() {
        if (PENDING.isEmpty()) return;
        List<XianqiaoManagerBlockEntity> claimed = new ArrayList<>();
        PENDING.entrySet().removeIf(entry -> {
            XianqiaoManagerBlockEntity manager = entry.getValue();
            if (manager.isRemoved()) return true;
            if (manager.getOwner() == null) return false;
            claimed.add(manager);
            return true;
        });
        for (XianqiaoManagerBlockEntity manager : claimed) providerFor(manager);
    }

    private static void detach(GlobalPos key, RsManagerNetworkAttachment attachment) {
        ATTACHMENTS.remove(key);
        attachment.dispose();
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        List<RsManagerNetworkAttachment> all;
        synchronized (RsManagerNetworkNodes.class) {
            all = new ArrayList<>(ATTACHMENTS.values());
            ATTACHMENTS.clear();
            PENDING.clear();
        }
        for (RsManagerNetworkAttachment attachment : all) {
            try {
                attachment.dispose();
            } catch (RuntimeException error) {
                ImmortalStorageMod.LOG.warn(
                        "[Compat/RS] Xianqiao manager detach failed on stop: {}", error.toString());
            }
        }
    }
}
