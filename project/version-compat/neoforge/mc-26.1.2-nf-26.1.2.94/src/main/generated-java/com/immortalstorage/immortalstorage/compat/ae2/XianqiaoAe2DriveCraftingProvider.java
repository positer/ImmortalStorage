package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.StorageCell;
import appeng.blockentity.storage.DriveBlockEntity;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.network.storage.SourceVeinStorageIndex;
import com.immortalstorage.immortalstorage.source.SourceExchangeOffer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes every exchange offer of every Xianqiao disk mounted in one AE2 drive.
 *
 * <p>This is the "a disk in a drive also provides the exchange recipes" half of the AE2
 * integration. The disk already mounts its owner's storage through
 * {@link XianqiaoExchangeStorageCell}; without this provider the same disk would store resources
 * but offer no crafting pattern, so a terminal could read the network yet never craft from a vein.</p>
 *
 * <p><b>Why a drive node service and not a per-disk registration.</b> AE2 discovers crafting
 * providers exclusively through {@code IGridNode.getService(ICraftingProvider.class)}
 * ({@code NetworkCraftingProviders.addProvider} bytecode, both target lines), and a managed node
 * accepts services only before {@code create(...)}: {@code ManagedGridNode.addService} delegates
 * to {@code getInitData()}, which throws {@code IllegalStateException} once {@code create(...)} has
 * cleared the init data (verified with {@code javap -c} on 19.2.17 and 26.1.10). A cell cannot add
 * a service to the drive's node after the drive joined the grid, so the service has to be attached
 * while the drive block entity is still under construction - see
 * {@code XianqiaoDriveAe2CraftingMixin} and {@link XianqiaoAe2DriveCrafting#attach}.</p>
 *
 * <p><b>Several disks, several owners.</b> One drive holds any mix of owners, but an AE2 node has
 * exactly one instance per service class, so this provider aggregates all mounted owners into one
 * pattern list. Each entry remembers its owner ({@link OwnedOffer}); without that, two owners with
 * an identical output and price would collapse into one equal pattern and a push could settle
 * against the wrong wallet. {@link XianqiaoAe2ExchangePattern} therefore carries the owner in its
 * identity.</p>
 *
 * <p><b>Refresh cadence.</b> A drive has no server tick of its own, so the offer list is rebuilt
 * (a) when the drive re-reads its slots ({@code updateState} hook), and (b) once per
 * {@code XianqiaoExchangeGridService} reconciliation (~20 ticks), which is what makes a newly
 * connected source vein appear without touching the drive. A rebuild that produces the same
 * signature never calls {@link ICraftingProvider#requestUpdate}.</p>
 *
 * <p>Verified API surface (identical on both target lines): {@code ICraftingProvider} exposes
 * {@code getAvailablePatterns()}, {@code pushPattern(IPatternDetails, KeyCounter[])},
 * {@code isBusy()} and the static {@code requestUpdate(IManagedGridNode)}; the drive exposes
 * {@code getCellCount()}, {@code getOriginalCellInventory(int)} and {@code getLevel()}.</p>
 *
 * <p><b>Deliberate scope, stated so review can challenge it:</b> a mounted disk publishes its
 * owner's offers whether or not that wrapper is the grid's elected active one. The storage half of
 * the integration already has a single-winner election ({@code XianqiaoExchangeGridService}), and a
 * storage bus pointing at the manager keeps the drive wrapper inactive; publishing the patterns
 * anyway keeps "disk in drive means craftable" true. If a manager and a disk of the same owner sit
 * on one grid, both hosts publish the same offers: AE2 keeps one crafting provider list per equal
 * pattern and pushes to one provider per batch, and each push settles a complete batch on its own,
 * so the duplication costs nothing correctness-wise. Deduplicating it would need a second election
 * for recipes, which is not implemented here.</p>
 */
final class XianqiaoAe2DriveCraftingProvider implements ICraftingProvider {
    private static final int RESCAN_INTERVAL_TICKS = 20;

    private final DriveBlockEntity drive;
    private final IManagedGridNode node;
    private volatile List<IPatternDetails> patterns = List.of();
    private volatile Map<IPatternDetails, OwnedOffer> offersByPattern = Map.of();
    private List<String> signature = List.of();
    private boolean dirty = true;
    private long nextScanTick;

    XianqiaoAe2DriveCraftingProvider(DriveBlockEntity drive, IManagedGridNode node) {
        this.drive = drive;
        this.node = node;
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return patterns;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
        OwnedOffer owned = offersByPattern.get(details);
        if (owned == null) return false;
        if (drive.isRemoved()) return false;
        return XianqiaoAe2ExchangePush.push(node, owned.owner(), drive.getLevel(), owned.offer(), inputs);
    }

    /**
     * Called from the drive's slot-read hook. The slot inventory may still be mid-update here, so
     * this only recognises a Xianqiao disk and marks the offer list stale; the rebuild itself runs
     * from {@link #refresh()} once the drive's state pass completed.
     */
    void noteSlotRead(int slot) {
        StorageCell cell = drive.getOriginalCellInventory(slot);
        if (cell instanceof XianqiaoExchangeStorageCell) {
            dirty = true;
        }
    }

    /** Called after the drive rebuilt its slot watchers; a slot change is an immediate rebuild. */
    void refresh() {
        refreshIfDue(true);
    }

    /** Called from {@code XianqiaoExchangeGridService}'s reconciliation pass (server tick). */
    void refreshFromGridTick() {
        refreshIfDue(false);
    }

    private void refreshIfDue(boolean forced) {
        Level level = drive.getLevel();
        if (!(level instanceof ServerLevel serverLevel) || drive.isRemoved()) {
            clear();
            return;
        }
        // A drive that is not part of the grid (no channel yet, or unpowered) must not advertise
        // recipes it cannot execute: pushPattern would fail for lack of a grid. The grid service
        // keeps calling this, so the list appears within one reconciliation once the node is up.
        if (node.getNode() == null || !node.isActive()) {
            clear();
            return;
        }
        long now = serverLevel.getGameTime();
        if (!dirty && !forced && now < nextScanTick) return;
        nextScanTick = now + RESCAN_INTERVAL_TICKS;
        dirty = false;

        List<UUID> owners = mountedOwners();
        if (owners.isEmpty()) {
            clear();
            return;
        }
        Map<UUID, List<SourceExchangeOffer>> byOwner = new LinkedHashMap<>();
        for (UUID owner : owners) {
            byOwner.put(owner, SourceVeinStorageIndex.exchangeOffers(serverLevel.getServer(), owner));
        }
        List<String> nextSignature = signatureOf(byOwner);
        if (nextSignature.equals(signature)) return;
        signature = nextSignature;
        rebuild(byOwner);
        ICraftingProvider.requestUpdate(node);
    }

    private List<UUID> mountedOwners() {
        LinkedHashSet<UUID> owners = new LinkedHashSet<>();
        int slots = drive.getCellCount();
        for (int slot = 0; slot < slots; slot++) {
            if (drive.getOriginalCellInventory(slot) instanceof XianqiaoExchangeStorageCell cell) {
                owners.add(cell.owner());
            }
        }
        return List.copyOf(owners);
    }

    private void rebuild(Map<UUID, List<SourceExchangeOffer>> byOwner) {
        AEItemKey definition = XianqiaoAe2ExchangePush.definitionKey();
        AEItemKey yuan = XianqiaoAe2ExchangePush.yuanKey();
        List<IPatternDetails> nextPatterns = new ArrayList<>();
        Map<IPatternDetails, OwnedOffer> nextOffers = new HashMap<>();
        int skippedFree = 0;
        for (Map.Entry<UUID, List<SourceExchangeOffer>> entry : byOwner.entrySet()) {
            UUID owner = entry.getKey();
            for (SourceExchangeOffer offer : entry.getValue()) {
                XianqiaoAe2ExchangePattern pattern =
                        XianqiaoAe2ExchangePattern.of(offer, definition, yuan, owner);
                if (pattern == null) {
                    if (offer != null && offer.yuanPerBatch() <= 0L) skippedFree++;
                    continue;
                }
                nextPatterns.add(pattern);
                nextOffers.put(pattern, new OwnedOffer(owner, offer));
            }
        }
        patterns = List.copyOf(nextPatterns);
        offersByPattern = Map.copyOf(nextOffers);
        if (skippedFree > 0) {
            ImmortalStorageMod.LOG.debug(
                    "[Compat/AE2] {} free source offers are not published from a drive", skippedFree);
        }
    }

    private static List<String> signatureOf(Map<UUID, List<SourceExchangeOffer>> byOwner) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<UUID, List<SourceExchangeOffer>> entry : byOwner.entrySet()) {
            String owner = entry.getKey().toString();
            for (SourceExchangeOffer offer : entry.getValue()) {
                if (offer == null) continue;
                result.add(owner + '|' + offer.sourceId() + '|' + offer.outputId() + '|'
                        + offer.fluid() + '|' + offer.yuanPerBatch() + '|' + offer.outputsPerBatch());
            }
        }
        return List.copyOf(result);
    }

    private void clear() {
        if (signature.isEmpty() && patterns.isEmpty()) return;
        signature = List.of();
        patterns = List.of();
        offersByPattern = Map.of();
        ICraftingProvider.requestUpdate(node);
    }

    /** One pattern plus the owner whose wallet and Xianqiao storage settle it. */
    private record OwnedOffer(UUID owner, SourceExchangeOffer offer) {}
}
