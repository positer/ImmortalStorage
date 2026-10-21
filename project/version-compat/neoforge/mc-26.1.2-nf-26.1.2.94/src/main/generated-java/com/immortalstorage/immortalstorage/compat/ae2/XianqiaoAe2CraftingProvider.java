package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity;
import com.immortalstorage.immortalstorage.network.storage.SourceVeinStorageIndex;
import com.immortalstorage.immortalstorage.source.SourceExchangeOffer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes the owner's source-vein exchange offers as AE2 crafting patterns.
 *
 * <p><b>The only data source is
 * {@link SourceVeinStorageIndex#exchangeOffers(net.minecraft.server.MinecraftServer, java.util.UUID)}</b>: no connected
 * vein means no offer, and the projected list changes whenever a vein or manager member registers
 * or unregisters. Nothing here invents a rate.</p>
 *
 * <p>Verified against AE2 19.2.17 ({@code javap}):</p>
 * <ul>
 *   <li>{@code ICraftingProvider extends IGridNodeService}: {@code getAvailablePatterns()},
 *       {@code pushPattern(IPatternDetails, KeyCounter[])}, {@code isBusy()}, default
 *       {@code getPatternPriority()} / {@code getEmitableItems()}, static
 *       {@code requestUpdate(IManagedGridNode)}.</li>
 *   <li>A node service is discovered through {@code IGridNode.getService(ICraftingProvider.class)}
 *       by {@code NetworkCraftingProviders.addProvider(IGridNode)}; patterns are indexed by
 *       {@code IPatternDetails.getPrimaryOutput()}.</li>
 *   <li>The CPU extracts exactly one pattern's inputs per push
 *       ({@code CraftingCpuHelper.extractPatternInputs} uses {@code IInput.getMultiplier()}), so one
 *       {@code pushPattern} call must produce exactly one batch.</li>
 * </ul>
 *
 * <p><b>Payment model (implementation decision, flagged for review).</b> The source-vein conversion
 * charges the owner's Immortal Yuan through
 * {@code SourceChargeRegistry.IMMORTAL_YUAN}, which counts and removes real
 * {@code immortalstorage:immortal_yuan} items in Xianqiao/player storage - it is not an AE2-side
 * counter. A pushed pattern input therefore has to become wallet yuan before the vein can charge
 * it: the shared settlement helper deposits the pushed yuan with
 * {@code ImmortalStoragePlayerData#depositImmortalYuan(long)} and then extracts the resource,
 * which charges the same wallet. See {@link XianqiaoAe2ExchangePush}.</p>
 *
 * <p><b>Owner in pattern identity:</b> patterns now carry the owner, because the same drive-hosted
 * provider can publish several owners at once and a push must never settle against the wrong
 * wallet. The manager host only ever carries its own owner.</p> Net wallet movement is zero when the vein has to convert, and
 * non-negative (an earlier prepayment is refunded) when the vein already holds cached units.</p>
 *
 * <p><b>Known gaps (TODO, deliberately not guessed):</b></p>
 * <ol>
 *   <li>If {@code depositImmortalYuan} accepts less than requested the push is refused after the
 *       partial deposit; the caller's inputs are untouched and the wallet keeps the accepted part.
 *       A cap pre-check narrows but cannot fully close that window. Fix: a wallet-level
 *       reserve/settle pair, i.e. expose the {@code SourceChargeRegistry} reservation to AE2.</li>
 *   <li>If the vein extracts less than one batch after a successful simulate the deposit is not
 *       refunded. Same fix.</li>
 *   <li>Zero-cost (free) offers are not published: an input-less AE2 pattern would let a network
 *       mint resources for free, and free veins report {@code Long.MAX_VALUE} availability.</li>
 * </ol>
 */
final class XianqiaoAe2CraftingProvider implements ICraftingProvider {
    private static final int RESCAN_INTERVAL_TICKS = 20;

    private final XianqiaoManagerBlockEntity manager;
    private volatile List<IPatternDetails> patterns = List.of();
    private volatile Map<IPatternDetails, SourceExchangeOffer> offersByPattern = Map.of();
    private @Nullable IManagedGridNode node;
    private List<String> signature = List.of();
    private long nextScanTick;

    XianqiaoAe2CraftingProvider(XianqiaoManagerBlockEntity manager) {
        this.manager = manager;
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return patterns;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    /**
     * Re-projects the offer list and tells AE2 to re-index when it changed.
     *
     * <p>Called once per manager tick; the body is throttled to one scan per second because
     * {@code exchangeOffers} allocates a fresh list.</p>
     */
    void reconcile(IManagedGridNode node) {
        this.node = node;
        Level level = manager.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        long now = serverLevel.getGameTime();
        if (now < nextScanTick) return;
        nextScanTick = now + RESCAN_INTERVAL_TICKS;

        UUID owner = manager.getOwner();
        List<SourceExchangeOffer> offers = owner == null || manager.isRemoved()
                ? List.of()
                : SourceVeinStorageIndex.exchangeOffers(serverLevel.getServer(), owner);
        List<String> nextSignature = signatureOf(offers);
        if (nextSignature.equals(signature)) return;
        signature = nextSignature;
        rebuild(offers, owner);
        ICraftingProvider.requestUpdate(node);
    }

    private void rebuild(List<SourceExchangeOffer> offers, UUID owner) {
        AEItemKey definition = XianqiaoAe2ExchangePush.definitionKey();
        AEItemKey yuan = XianqiaoAe2ExchangePush.yuanKey();
        List<IPatternDetails> nextPatterns = new ArrayList<>(offers.size());
        Map<IPatternDetails, SourceExchangeOffer> nextOffers = new HashMap<>();
        int skippedFree = 0;
        for (SourceExchangeOffer offer : offers) {
            XianqiaoAe2ExchangePattern pattern =
                    XianqiaoAe2ExchangePattern.of(offer, definition, yuan, owner);
            if (pattern == null) {
                if (offer != null && offer.yuanPerBatch() <= 0L) skippedFree++;
                continue;
            }
            nextPatterns.add(pattern);
            nextOffers.put(pattern, offer);
        }
        patterns = List.copyOf(nextPatterns);
        offersByPattern = Map.copyOf(nextOffers);
        if (skippedFree > 0) {
            ImmortalStorageMod.LOG.debug(
                    "[Compat/AE2] {} free source offers are not published as AE2 patterns", skippedFree);
        }
    }

    private static List<String> signatureOf(List<SourceExchangeOffer> offers) {
        List<String> result = new ArrayList<>(offers.size());
        for (SourceExchangeOffer offer : offers) {
            if (offer == null) continue;
            result.add(offer.sourceId() + '|' + offer.outputId() + '|' + offer.fluid()
                    + '|' + offer.yuanPerBatch() + '|' + offer.outputsPerBatch());
        }
        return List.copyOf(result);
    }

    /**
     * Settlement is shared with the drive host so the manager and a mounted disk can never charge
     * in two different orders; see {@link XianqiaoAe2ExchangePush#push}.
     */
    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
        SourceExchangeOffer offer = offersByPattern.get(details);
        if (offer == null) return false;
        UUID owner = manager.getOwner();
        if (owner == null || manager.isRemoved()) return false;
        return XianqiaoAe2ExchangePush.push(node, owner, manager.getLevel(), offer, inputs);
    }
}
