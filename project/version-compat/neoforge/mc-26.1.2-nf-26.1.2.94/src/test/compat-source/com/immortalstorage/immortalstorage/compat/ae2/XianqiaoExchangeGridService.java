package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.implementations.blockentities.IChestOrDrive;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridService;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.storage.cells.StorageCell;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Grid-local owner election for Xianqiao exchange cells.
 *
 * <p>AE2 enumerates every mounted cell independently. If two link cells for
 * the same owner were active on one grid, the shared backend would be counted
 * and mutated twice. This service keeps one stable winner and leaves all other
 * wrappers inactive. It also observes backend revisions at a low frequency so
 * changes made outside AE2 invalidate AE2's cached network inventory.</p>
 */
public final class XianqiaoExchangeGridService implements IGridService, IGridServiceProvider {
    private static final int RESCAN_INTERVAL_TICKS = 20;
    private static final Comparator<XianqiaoExchangeStorageCell> STABLE_CANDIDATE_ORDER =
            Comparator.comparing(XianqiaoExchangeStorageCell::diskId)
                    .thenComparingInt(System::identityHashCode);

    private final IGrid grid;
    private Set<XianqiaoExchangeStorageCell> knownWrappers = identitySet();
    private Map<UUID, XianqiaoExchangeStorageCell> winners = Map.of();
    private Map<XianqiaoExchangeStorageCell, XianqiaoExchangeStorageCell.RevisionStamp> activeStamps =
            new IdentityHashMap<>();
    private Set<UUID> duplicateOwners = Set.of();

    private boolean dirty = true;
    private int ticksUntilRescan;

    /** Public constructor required by AE2's grid-service factory. */
    public XianqiaoExchangeGridService(IGrid grid) {
        this.grid = Objects.requireNonNull(grid, "grid");
    }

    @Override
    public void addNode(IGridNode node, @Nullable CompoundTag savedData) {
        dirty = true;
        // Close the one-tick double-mount window immediately. A mounted disk
        // and a newly added storage bus may otherwise both advertise and
        // mutate the same owner backend before the periodic reconciliation.
        UUID storageBusOwner = managerOwnerBehindStorageBus(node.getOwner());
        XianqiaoExchangeStorageCell mountedDisk = storageBusOwner == null
                ? null : winners.get(storageBusOwner);
        if (mountedDisk != null && mountedDisk.setActive(false)) {
            mountedDisk.invalidateSnapshotCache();
            grid.getStorageService().invalidateCache();
        }
    }

    @Override
    public void removeNode(IGridNode node) {
        dirty = true;
    }

    @Override
    public void onServerStartTick() {
        if (!dirty && ticksUntilRescan > 0) {
            ticksUntilRescan--;
            return;
        }
        dirty = false;
        ticksUntilRescan = RESCAN_INTERVAL_TICKS - 1;
        reconcileWrappers();
    }

    private void reconcileWrappers() {
        Set<XianqiaoExchangeStorageCell> discovered = discoverActiveDriveWrappers();
        Set<UUID> storageBusOwners = discoverManagerStorageBusOwners();
        Map<UUID, List<XianqiaoExchangeStorageCell>> byOwner = new HashMap<>();
        for (XianqiaoExchangeStorageCell wrapper : discovered) {
            byOwner.computeIfAbsent(wrapper.owner(), ignored -> new ArrayList<>()).add(wrapper);
        }

        Map<UUID, XianqiaoExchangeStorageCell> nextWinners = new HashMap<>();
        Set<UUID> nextDuplicateOwners = new HashSet<>();
        Set<XianqiaoExchangeStorageCell> desiredActive = identitySet();
        for (Map.Entry<UUID, List<XianqiaoExchangeStorageCell>> entry : byOwner.entrySet()) {
            if (entry.getValue().size() > 1) {
                nextDuplicateOwners.add(entry.getKey());
                if (!duplicateOwners.contains(entry.getKey())) {
                    ImmortalStorageMod.LOG.warn("[Compat/AE2] ME grid has {} Xianqiao exchange cells for owner {}; only one is active",
                            entry.getValue().size(), entry.getKey());
                }
            }
            XianqiaoExchangeStorageCell previous = winners.get(entry.getKey());
            XianqiaoExchangeStorageCell winner = containsIdentity(entry.getValue(), previous)
                    ? previous
                    : entry.getValue().stream().min(STABLE_CANDIDATE_ORDER).orElseThrow();
            nextWinners.put(entry.getKey(), winner);
            if (diskWrapperMayMount(entry.getKey(), storageBusOwners)) desiredActive.add(winner);
        }

        boolean invalidate = false;
        Set<XianqiaoExchangeStorageCell> allWrappers = identitySet();
        allWrappers.addAll(knownWrappers);
        allWrappers.addAll(discovered);
        for (XianqiaoExchangeStorageCell wrapper : allWrappers) {
            invalidate |= wrapper.setActive(desiredActive.contains(wrapper));
        }

        Map<XianqiaoExchangeStorageCell, XianqiaoExchangeStorageCell.RevisionStamp> nextStamps =
                new IdentityHashMap<>();
        for (XianqiaoExchangeStorageCell wrapper : desiredActive) {
            XianqiaoExchangeStorageCell.RevisionStamp stamp = wrapper.revisionStamp();
            nextStamps.put(wrapper, stamp);
            if (!stamp.equals(activeStamps.get(wrapper))) {
                wrapper.invalidateSnapshotCache();
                invalidate = true;
            }
        }
        if (activeStamps.size() != nextStamps.size()) invalidate = true;

        knownWrappers = discovered;
        winners = nextWinners;
        activeStamps = nextStamps;
        duplicateOwners = Set.copyOf(nextDuplicateOwners);

        if (invalidate) grid.getStorageService().invalidateCache();
    }

    static boolean diskWrapperMayMount(UUID owner, Set<UUID> storageBusOwners) {
        return !storageBusOwners.contains(owner);
    }

    /**
     * A disk cell and a storage bus can otherwise mount the exact same
     * PersonalStorage backend twice. Prefer the physical manager's storage-bus
     * mount on this grid and leave its disk wrapper inactive until that bus is
     * removed.
     */
    private Set<UUID> discoverManagerStorageBusOwners() {
        Set<UUID> owners = new HashSet<>();
        for (IGridNode node : grid.getNodes()) {
            if (!node.isActive()) continue;
            UUID owner = managerOwnerBehindStorageBus(node.getOwner());
            if (owner != null) owners.add(owner);
        }
        return owners;
    }

    private static @Nullable UUID managerOwnerBehindStorageBus(@Nullable Object machine) {
        if (machine == null || !machine.getClass().getName().endsWith(".StorageBusPart")) return null;
        try {
            Level level = (Level) machine.getClass().getMethod("getLevel").invoke(machine);
            Direction side = (Direction) machine.getClass().getMethod("getSide").invoke(machine);
            BlockEntity host = (BlockEntity) machine.getClass().getMethod("getBlockEntity").invoke(machine);
            if (level == null || side == null || host == null) return null;
            BlockEntity target = level.getBlockEntity(host.getBlockPos().relative(side));
            return target instanceof XianqiaoManagerBlockEntity manager ? manager.getOwner() : null;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }

    private Set<XianqiaoExchangeStorageCell> discoverActiveDriveWrappers() {
        Set<XianqiaoExchangeStorageCell> wrappers = identitySet();
        // AE2 19.2.17 indexes IGrid#getActiveMachines by the owner's exact
        // concrete class, not by implemented interfaces. Asking for
        // IChestOrDrive therefore returns an empty set for DriveBlockEntity.
        // Source: https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5/src/main/java/appeng/me/Grid.java
        for (IGridNode node : grid.getNodes()) {
            if (!node.isActive() || !(node.getOwner() instanceof IChestOrDrive drive)) continue;
            for (int slot = 0; slot < drive.getCellCount(); slot++) {
                StorageCell cell = drive.getOriginalCellInventory(slot);
                if (cell instanceof XianqiaoExchangeStorageCell wrapper) wrappers.add(wrapper);
            }
        }
        return wrappers;
    }

    private static boolean containsIdentity(
            List<XianqiaoExchangeStorageCell> candidates,
            @Nullable XianqiaoExchangeStorageCell target) {
        if (target == null) return false;
        for (XianqiaoExchangeStorageCell candidate : candidates) {
            if (candidate == target) return true;
        }
        return false;
    }

    private static Set<XianqiaoExchangeStorageCell> identitySet() {
        return java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
