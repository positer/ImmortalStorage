package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.item.custom.XianqiaoRsExchangeDiskItem;
import com.immortalstorage.immortalstorage.network.storage.SourceVeinStorageIndex;
import com.immortalstorage.immortalstorage.source.SourceExchangeOffer;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.storage.AbstractDiskContainerBlockEntity;
import com.refinedmods.refinedstorage.common.support.network.SimpleConnectionStrategy;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Refined Storage exchange-pattern providers hosted by the RS <b>disk drive</b>.
 *
 * <h2>What this adds</h2>
 * <p>A Xianqiao disk mounted in RS's disk drive already contributes storage through
 * {@link XianqiaoRsStorage} / {@link RsXianqiaoStorageType}, but it contributes no crafting
 * patterns, so the owner's source-vein exchange offers are invisible to RS autocrafting. This
 * bridge hangs one {@link RsExchangeProviderNode} (a {@code PatternProviderNetworkNode}) on the
 * drive block itself for as long as a bound Xianqiao disk sits in a drive slot, and publishes that
 * disk owner's offers as EXTERNAL patterns - the same projection
 * {@link RsManagerNetworkAttachment} already performs for the Xianqiao manager block.</p>
 *
 * <h2>VERIFIED RS API (javap -p on refinedstorage-neoforge-2.0.9.jar and -3.2.1.jar)</h2>
 * <ul>
 *   <li>{@code AbstractDiskContainerBlockEntity#setLevel(Level)} is declared in both lines with the
 *       identical descriptor {@code (Lnet/minecraft/world/level/Level;)V}; it is the only drive
 *       life-cycle method both lines share. It must <b>not</b> be confused with
 *       {@code onDiskChanged}, which is {@code onDiskChanged(int)} on 2.0.9 and
 *       {@code onDiskChanged()} on 3.2.1 - a slot-change injection point that cannot be written
 *       once for both lines.</li>
 *   <li>{@code AbstractDiskContainerBlockEntity#getDiskInventory()} returns RS's
 *       {@code FilteredContainer} (a vanilla {@code SimpleContainer}) on both lines; its
 *       {@code getContainerSize()} is 8 - {@code AbstractDiskDriveBlockEntity.AMOUNT_OF_DISKS} has
 *       {@code ConstantValue: int 8} in both jars, and the drive sizes its {@code DiskInventory}
 *       from {@code mainNetworkNode.getSize()}.</li>
 *   <li>{@code AbstractNetworkNodeContainerBlockEntity#getContainerProvider()} (public final,
 *       inherited by the drive) exposes the block's own {@code NetworkNodeContainerProvider}, and
 *       {@code NetworkNodeContainerProvider#addContainer(InWorldNetworkNodeContainer)} is the only
 *       mutator it has. There is no {@code removeContainer}, and
 *       {@code NetworkNodeContainerProviderImpl#getContainers()} hands out an unmodifiable view -
 *       which is why a container is never taken back out here (see LIMITS below).</li>
 *   <li>{@code RefinedStorageApi#createNetworkNodeContainer(BlockEntity, NetworkNode)} +
 *       {@code name/priority/connectionStrategy/keyProvider/build} and
 *       {@code initializeNetworkNodeContainer(container, Level, Runnable)} are byte-identical on
 *       both lines, exactly as {@link RsManagerNetworkAttachment} already relies on.</li>
 *   <li>{@code NetworkNodeContainerProvider#initialize(Level, Runnable)} (the default method) walks
 *       every container of the provider and calls
 *       {@code RefinedStorageApi#initializeNetworkNodeContainer} on it; a drive that re-enters the
 *       level therefore re-joins this bridge's containers with no extra code.</li>
 * </ul>
 *
 * <h2>LIMITS (measured / reasoned, not hidden)</h2>
 * <ul>
 *   <li><b>The container is not removed when the disk leaves.</b> RS 2.0.9/3.2.1 expose
 *       {@code addContainer} only and the stock provider returns an unmodifiable set, so the only
 *       ways out are reflection into RS internals or a replacement provider class. Instead the
 *       slot's node publishes zero patterns ({@code publish(List.of(), null)}), which unregisters
 *       every pattern exactly as an emptied pattern provider does. The residue is at most one inert
 *       node per drive slot ever used, with 0 energy usage and no patterns.</li>
 *   <li><b>Yuan is not supplied by this bridge.</b> An RS EXTERNAL pattern is charged by extracting
 *       its declared ingredients from the <i>network root storage</i> before the sink is called, and
 *       {@link XianqiaoRsStorage} only exposes the Xianqiao's real item/fluid stock - the immortal
 *       yuan account is not part of it. The manager bridge solves that with
 *       {@link RsManagerCurrencyStorage} ({@code xianqiao_manager_yuan}). A drive-only network
 *       therefore publishes the patterns but cannot start a craft until some node on the same
 *       network advertises the yuan item resource. Wiring one currency node per drive would be the
 *       follow-up change if the user wants drive-only exchange.</li>
 *   <li><b>Container keys repeat across drives.</b> The key is
 *       {@code xianqiao_drive_exchange_<slot>} as specified, so two drives of one network that
 *       hold a Xianqiao disk in the same slot index produce the same key. Verified effect: RS's
 *       {@code GraphNetworkComponentImpl#addToIndex} stores containers in
 *       {@code Map<Object, NetworkNodeContainer> byKeyIndex} with {@code put}, so the last one wins
 *       for key-addressed lookup ({@code getContainer(key)}); network membership, storage,
 *       autocrafting registration and the published patterns are indexed by container identity and
 *       are unaffected. Nothing in RS asks for a pattern provider by key.</li>
 *   <li><b>Unbound disks publish nothing</b> - the same rule as
 *       {@code XianqiaoRsStorageContainerItem#resolve}: without an owner and a disk id there is no
 *       storage and no offer.</li>
 * </ul>
 *
 * <p>Lifecycle: the drive is registered from the mixin on {@code setLevel} (server side only) and
 * reconciled once per second from the server tick, because a disk can be inserted or pulled out
 * without the drive block entity ever being re-created.</p>
 */
public final class RsDriveExchange {
    /** Container key prefix; {@code <prefix><slot>} is stable for the lifetime of one drive slot. */
    static final String CONTAINER_PREFIX = "xianqiao_drive_exchange_";

    /** Offer refresh period; the vein set changes as veins register with the owner's directory. */
    private static final int REFRESH_TICKS = 20;
    /** Retry period while the drive is not connected to any network yet. */
    private static final int JOIN_RETRY_TICKS = 20;
    /** Back-off after a failed container build, so a broken RS API cannot warn once per second. */
    private static final int ATTACH_RETRY_TICKS = 200;
    private static final int NODE_PRIORITY = 0;

    /** RS calls this once a container joined a network; the tick republishes, so there is nothing to do. */
    private static final Runnable NO_NOTIFICATION = () -> { };

    private static final Map<GlobalPos, Drive> DRIVES = new HashMap<>();

    private static boolean installed;
    private static @Nullable ResourceKey yuanResource;

    private RsDriveExchange() {
    }

    /**
     * Installs the server tick driver. Called lazily from {@link #note(BlockEntity, ServerLevel)}
     * the first time a disk drive appears, so a world without one never pays for the listener and
     * this bridge needs no extra bootstrap hook outside its own files.
     */
    private static synchronized void initialize() {
        if (installed) return;
        installed = true;
        NeoForge.EVENT_BUS.addListener(RsDriveExchange::onServerTick);
        NeoForge.EVENT_BUS.addListener(RsDriveExchange::onServerStopped);
        ImmortalStorageMod.LOG.info(
                "[Compat/RS] Disk-drive exchange nodes installed (container key {}<slot>)",
                CONTAINER_PREFIX);
    }

    /**
     * Registers one disk drive for reconciliation. Called from
     * {@code DiskDriveExchangeMixin} on the drive's {@code setLevel}, i.e. once per block entity
     * instance, never per tick.
     *
     * @param blockEntity the drive block entity (RS {@code AbstractDiskContainerBlockEntity})
     * @param level       the server level the entity now belongs to
     */
    public static void note(BlockEntity blockEntity, ServerLevel level) {
        if (!(blockEntity instanceof AbstractDiskContainerBlockEntity<?> drive)) return;
        initialize();
        GlobalPos key = GlobalPos.of(level.dimension(), blockEntity.getBlockPos());
        synchronized (RsDriveExchange.class) {
            Drive existing = DRIVES.get(key);
            if (existing != null && existing.blockEntity == blockEntity) return;
            if (existing != null) existing.dispose();
            DRIVES.put(key, new Drive(key, drive, level));
        }
        ImmortalStorageMod.LOG.debug(
                "[Compat/RS] Watching Refined Storage disk drive at {} for Xianqiao disks", key);
    }

    /** The network ingredient identity every exchange pattern is priced in. */
    private static ResourceKey yuanResource() {
        ResourceKey cached = yuanResource;
        if (cached == null) {
            cached = ItemResource.ofItemStack(new ItemStack(ModItems.IMMORTAL_YUAN.get()));
            yuanResource = cached;
        }
        return cached;
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        List<Drive> drives;
        synchronized (RsDriveExchange.class) {
            if (DRIVES.isEmpty()) return;
            drives = new ArrayList<>(DRIVES.values());
        }
        List<Drive> stale = new ArrayList<>(1);
        for (Drive drive : drives) {
            try {
                drive.tick();
            } catch (RuntimeException error) {
                ImmortalStorageMod.LOG.error(
                        "[Compat/RS] Disk-drive exchange node tick failed at {}", drive.key, error);
            }
            if (drive.isGone()) stale.add(drive);
        }
        if (stale.isEmpty()) return;
        synchronized (RsDriveExchange.class) {
            for (Drive drive : stale) {
                if (DRIVES.get(drive.key) == drive) DRIVES.remove(drive.key);
            }
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        List<Drive> drives;
        synchronized (RsDriveExchange.class) {
            drives = new ArrayList<>(DRIVES.values());
            DRIVES.clear();
        }
        for (Drive drive : drives) {
            try {
                drive.dispose();
            } catch (RuntimeException error) {
                ImmortalStorageMod.LOG.warn(
                        "[Compat/RS] Disk-drive exchange detach failed on stop: {}", error.toString());
            }
        }
    }

    /** One disk drive: one exchange node per occupied drive slot. */
    private static final class Drive {
        private final GlobalPos key;
        private final AbstractDiskContainerBlockEntity<?> blockEntity;
        private final ServerLevel level;
        private final SlotState[] slots;
        private long nextReconcile;
        private boolean disposed;

        private Drive(
                GlobalPos key, AbstractDiskContainerBlockEntity<?> blockEntity, ServerLevel level) {
            this.key = key;
            this.blockEntity = blockEntity;
            this.level = level;
            int diskSlots;
            try {
                diskSlots = blockEntity.getDiskInventory().getContainerSize();
            } catch (RuntimeException error) {
                // A drive whose inventory cannot be sized stays storage-only instead of throwing
                // inside the block entity's own setLevel call.
                diskSlots = 0;
            }
            this.slots = new SlotState[Math.max(0, diskSlots)];
            for (int slot = 0; slot < slots.length; slot++) slots[slot] = new SlotState(slot);
        }

        private void tick() {
            if (disposed) return;
            if (blockEntity.isRemoved()) {
                dispose();
                return;
            }
            long gameTime = level.getGameTime();
            if (gameTime >= nextReconcile) {
                nextReconcile = gameTime + REFRESH_TICKS;
                reconcile();
            }
            for (SlotState slot : slots) slot.tick(this);
        }

        /**
         * Re-reads the drive inventory and keeps one exchange node in step with every occupied
         * slot. A slot holding a disk of a different owner replaces the published offer set instead
         * of leaving the previous owner's patterns behind.
         */
        private void reconcile() {
            if (level.getBlockEntity(blockEntity.getBlockPos()) != blockEntity) {
                // The block entity was swapped out (unload/reload) without this bridge seeing it.
                dispose();
                return;
            }
            var inventory = blockEntity.getDiskInventory();
            int slotsToCheck = Math.min(slots.length, inventory.getContainerSize());
            for (int slot = 0; slot < slotsToCheck; slot++) {
                ItemStack stack = inventory.getItem(slot);
                UUID owner = null;
                UUID disk = null;
                if (stack.getItem() instanceof XianqiaoRsExchangeDiskItem) {
                    owner = XianqiaoRsExchangeDiskItem.owner(stack).orElse(null);
                    disk = XianqiaoRsExchangeDiskItem.diskId(stack).orElse(null);
                }
                slots[slot].sync(this, owner, disk);
            }
            for (int slot = slotsToCheck; slot < slots.length; slot++) slots[slot].clear();
            for (SlotState slot : slots) slot.refresh(this);
        }

        private void dispose() {
            if (disposed) return;
            disposed = true;
            for (SlotState slot : slots) slot.dispose();
        }

        private boolean isGone() {
            return disposed || blockEntity.isRemoved();
        }
    }

    /** One drive slot's exchange node: attached lazily, republished while a disk is present. */
    private static final class SlotState {
        private final int slot;
        private @Nullable UUID owner;
        private @Nullable UUID disk;
        private @Nullable RsExchangeProviderNode node;
        private @Nullable InWorldNetworkNodeContainer container;
        private long nextJoin;
        private long nextAttach;

        private SlotState(int slot) {
            this.slot = slot;
        }

        private void sync(Drive drive, @Nullable UUID diskOwner, @Nullable UUID diskId) {
            if (diskOwner == null || diskId == null) {
                clear();
                return;
            }
            if (diskOwner.equals(owner) && diskId.equals(disk)) return;
            clear();
            if (node == null && !attach(drive)) return;
            owner = diskOwner;
            disk = diskId;
            activate();
            publish(drive.level, diskOwner);
        }

        /**
         * Builds this slot's node and adds it to the drive's own container provider. The node is
         * created once per slot and then kept: see the LIMITS note on {@link RsDriveExchange}.
         *
         * <p>The provider mutation and the network join are deliberately two steps: once the
         * container is in the provider it must <b>not</b> be retried (a retry would add a second
         * container for the same slot), so a failed join only leaves the node unattached until the
         * tick retries it.</p>
         */
        private boolean attach(Drive drive) {
            long now = drive.level.getGameTime();
            // Only reached while this slot has no node, so a rejected build never double-adds.
            if (now < nextAttach) return false;
            String key = CONTAINER_PREFIX + slot;
            RsExchangeProviderNode created = new RsExchangeProviderNode();
            InWorldNetworkNodeContainer built;
            try {
                built = RefinedStorageApi.INSTANCE
                        .createNetworkNodeContainer(drive.blockEntity, created)
                        .name(key)
                        .priority(NODE_PRIORITY)
                        .connectionStrategy(new SimpleConnectionStrategy(drive.blockEntity.getBlockPos()))
                        .keyProvider(() -> key)
                        .build();
                drive.blockEntity.getContainerProvider().addContainer(built);
            } catch (RuntimeException error) {
                nextAttach = now + ATTACH_RETRY_TICKS;
                ImmortalStorageMod.LOG.warn(
                        "[Compat/RS] Unable to attach the exchange node to the disk drive at {} slot {}: {}",
                        drive.blockEntity.getBlockPos(), slot, error.toString());
                return false;
            }
            node = created;
            container = built;
            join(drive.level);
            return true;
        }

        private void tick(Drive drive) {
            if (node == null || owner == null) return;
            if (node.getNetwork() == null) {
                long gameTime = drive.level.getGameTime();
                if (gameTime >= nextJoin) {
                    nextJoin = gameTime + JOIN_RETRY_TICKS;
                    join(drive.level);
                }
                return;
            }
            activate();
            node.doWork();
        }

        private void refresh(Drive drive) {
            if (node == null || owner == null) return;
            publish(drive.level, owner);
        }

        /** Republishes the owner's current offers; a no-op when the published id set is unchanged. */
        private void publish(ServerLevel level, UUID diskOwner) {
            RsExchangeProviderNode current = node;
            if (current == null) return;
            MinecraftServer server = com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level);
            List<SourceExchangeOffer> offers = server == null
                    ? List.of() : SourceVeinStorageIndex.exchangeOffers(server, diskOwner);
            current.publish(offers, yuanResource());
        }

        private void join(ServerLevel level) {
            InWorldNetworkNodeContainer current = container;
            if (current == null) return;
            try {
                RefinedStorageApi.INSTANCE.initializeNetworkNodeContainer(
                        current, level, NO_NOTIFICATION);
            } catch (RuntimeException error) {
                ImmortalStorageMod.LOG.debug(
                        "[Compat/RS] Unable to re-join the disk-drive exchange node in slot {}: {}",
                        slot, error.toString());
            }
        }

        /** RS activates nodes from its own block-entity ticker; a bridge-owned node has none. */
        private void activate() {
            RsExchangeProviderNode current = node;
            if (current == null) return;
            if (current.getNetwork() != null && !current.isActive()) current.setActive(true);
        }

        /** Drops every published pattern; the node itself stays attached (see LIMITS). */
        private void clear() {
            if (owner == null && disk == null) return;
            owner = null;
            disk = null;
            RsExchangeProviderNode current = node;
            if (current != null) current.publish(List.of(), null);
        }

        private void dispose() {
            clear();
            node = null;
            container = null;
        }
    }
}
