package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.implementations.blockentities.IChestOrDrive;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.cells.StorageCell;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Mounts the owner's Xianqiao storage into the grid, which is the "connected manager equals
 * connected Xianqiao disk" half of the AE2 integration.
 *
 * <p>Verified against AE2 19.2.17 ({@code javap}):</p>
 * <ul>
 *   <li>{@code IStorageProvider extends IGridNodeService { void mountInventories(IStorageMounts); }}
 *       plus static {@code requestUpdate(IManagedGridNode)}.</li>
 *   <li>{@code StorageService.addNode} resolves the provider with
 *       {@code IGridNode.getService(IStorageProvider.class)} and mounts whatever
 *       {@code mountInventories} registers; {@code IStorageMounts} has
 *       {@code mount(MEStorage)} (default priority) and {@code mount(MEStorage, int)}.</li>
 * </ul>
 *
 * <p><b>Owner election (why this file is not simply "mount always").</b>
 * {@code XianqiaoExchangeGridService} already elects a single active
 * {@code XianqiaoExchangeStorageCell} per owner among drive-mounted cells and storage buses. A node
 * mount is a third path that service does not enumerate, so this provider yields whenever another
 * path already exposes the same owner storage on this grid:</p>
 * <ol>
 *   <li>an active drive node holding an exchange cell for the owner;</li>
 *   <li>a storage bus pointing at a manager of the owner (mirrors
 *       {@code XianqiaoExchangeGridService#managerOwnerBehindStorageBus}, which is package-private
 *       there and therefore re-expressed here with the same reflection);</li>
 *   <li>another manager of the same owner with a smaller block position (stable election).</li>
 * </ol>
 *
 * <p><b>Known residual window (TODO, needs the shared election to close):</b> a drive cell mounted
 * <i>after</i> this node mounted is not seen by {@code XianqiaoExchangeGridService} as a conflict, so
 * both are active for up to one rescan (20 ticks). Amounts are then reported twice while real
 * extraction stays bounded by the storage backend. The permanent fix is to let
 * {@code XianqiaoExchangeGridService} treat this provider's wrapper as a candidate - the exact
 * patch is in the report, because that file is outside this agent's write scope.</p>
 */
final class XianqiaoAe2StorageProvider implements IStorageProvider {
    private static final int RESCAN_INTERVAL_TICKS = 20;
    private static final String MANAGER_LABEL = "block.immortalstorage.xianqiao_manager";

    private final XianqiaoManagerBlockEntity manager;
    private @Nullable XianqiaoExchangeStorageCell mounted;
    private @Nullable UUID mountedOwner;
    private boolean lastWanted = true;
    private long nextScanTick;

    XianqiaoAe2StorageProvider(XianqiaoManagerBlockEntity manager) {
        this.manager = manager;
    }

    @Override
    public void mountInventories(IStorageMounts mounts) {
        XianqiaoExchangeStorageCell storage = mountForCurrentOwner();
        if (storage == null) return;
        mounts.mount(storage);
    }

    /** Re-evaluates the election once per second and asks AE2 to re-mount when it changed. */
    void reconcile(IManagedGridNode node) {
        Level level = manager.getLevel();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        long now = serverLevel.getGameTime();
        if (now < nextScanTick) return;
        nextScanTick = now + RESCAN_INTERVAL_TICKS;

        boolean wanted = shouldMount(node);
        if (wanted == lastWanted) return;
        lastWanted = wanted;
        IStorageProvider.requestUpdate(node);
    }

    private boolean shouldMount(IManagedGridNode node) {
        UUID owner = manager.getOwner();
        if (owner == null || manager.isRemoved()) return false;
        IGrid grid = node.getGrid();
        if (grid == null) return false;
        IGridNode self = node.getNode();
        long selfPos = manager.getBlockPos().asLong();
        for (IGridNode other : grid.getNodes()) {
            if (other == self) continue;
            Object machine = other.getOwner();
            if (machine instanceof XianqiaoManagerBlockEntity otherManager) {
                if (owner.equals(otherManager.getOwner())
                        && otherManager.getBlockPos().asLong() < selfPos) return false;
                continue;
            }
            if (!other.isActive()) continue;
            if (machine instanceof IChestOrDrive drive && exposesOwnerCell(drive, owner)) return false;
            if (storageBusTargetsManagerOf(machine, owner)) return false;
        }
        return true;
    }

    private static boolean exposesOwnerCell(IChestOrDrive drive, UUID owner) {
        for (int slot = 0; slot < drive.getCellCount(); slot++) {
            StorageCell cell = drive.getOriginalCellInventory(slot);
            if (cell instanceof XianqiaoExchangeStorageCell wrapper && owner.equals(wrapper.owner())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Re-expressed from {@code XianqiaoExchangeGridService#managerOwnerBehindStorageBus}; AE2 does
     * not expose the storage bus part type in its public API in 19.2.17.
     */
    private static boolean storageBusTargetsManagerOf(@Nullable Object machine, UUID owner) {
        if (machine == null || !machine.getClass().getName().endsWith(".StorageBusPart")) return false;
        try {
            Level level = (Level) machine.getClass().getMethod("getLevel").invoke(machine);
            Direction side = (Direction) machine.getClass().getMethod("getSide").invoke(machine);
            BlockEntity host = (BlockEntity) machine.getClass().getMethod("getBlockEntity").invoke(machine);
            if (level == null || side == null || host == null) return false;
            BlockEntity target = level.getBlockEntity(host.getBlockPos().relative(side));
            return target instanceof XianqiaoManagerBlockEntity other && owner.equals(other.getOwner());
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return false;
        }
    }

    private @Nullable XianqiaoExchangeStorageCell mountForCurrentOwner() {
        UUID owner = manager.getOwner();
        if (owner == null || manager.isRemoved()) return null;
        XianqiaoExchangeStorageCell existing = mounted;
        if (existing != null && owner.equals(mountedOwner)) return existing;
        UUID identity = UUID.nameUUIDFromBytes(("xianqiao-manager-node:" + owner + ':'
                + manager.getBlockPos().asLong()).getBytes(StandardCharsets.UTF_8));
        XianqiaoExchangeStorageCell created = new XianqiaoExchangeStorageCell(
                owner, identity, Component.translatable(MANAGER_LABEL));
        created.setActive(true);
        mounted = created;
        mountedOwner = owner;
        return created;
    }
}
