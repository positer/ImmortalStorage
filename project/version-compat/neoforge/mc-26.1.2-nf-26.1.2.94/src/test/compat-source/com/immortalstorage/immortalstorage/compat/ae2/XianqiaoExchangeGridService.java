package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.implementations.blockentities.IChestOrDrive;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridService;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.storage.cells.StorageCell;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.nbt.CompoundTag;
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
            desiredActive.add(winner);
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
