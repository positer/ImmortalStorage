package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.source.SourceExchangeOffer;
import com.refinedmods.refinedstorage.api.autocrafting.Pattern;
import com.refinedmods.refinedstorage.api.autocrafting.task.ExternalPatternSink;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.impl.node.patternprovider.PatternProviderNetworkNode;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RS 2.0.9 external-pattern provider for the owner's source-vein exchange offers.
 *
 * <p>VERIFIED (javap -p -c of PatternProviderNetworkNode / AutocraftingNetworkComponentImpl /
 * ExternalTaskPattern):</p>
 * <ul>
 *   <li>{@code PatternProviderNetworkNode(long energyUsage, int patternSlots)} is a
 *       {@code SimpleNetworkNode} that already implements the whole
 *       {@code PatternProvider}/{@code ExternalPatternSink}/{@code StepBehavior} contract.</li>
 *   <li>The network's autocrafting component registers any node implementing
 *       {@code PatternProvider} when its container is added
 *       ({@code AutocraftingNetworkComponentImpl.onContainerAdded}) and, for each
 *       {@code setPattern(slot, pattern)}, records the provider as a sink for that pattern's
 *       {@code PatternLayout} ({@code add(provider, pattern, priority)}).</li>
 *   <li>The dispatch order for an EXTERNAL pattern is: probe {@code accept(pattern, inputs,
 *       SIMULATE)} -> on ACCEPTED extract the ingredients from the network root storage
 *       (EXECUTE) -> {@code accept(pattern, inputs, EXECUTE)}. The provider's
 *       {@code accept(Pattern, Collection, Action)} override is therefore the one place that
 *       knows both the pattern and the already-charged ingredients.</li>
 *   <li>{@code PatternProviderNetworkNode.doWork()} returns early unless the node is active;
 *       RS activates nodes from its own block-entity ticker, which a bridge-owned node does not
 *       have. {@link RsManagerNetworkAttachment} sets it active and ticks it.</li>
 * </ul>
 *
 * <p>DELIVERY: outputs are inserted into {@code StorageNetworkComponent} (the network root
 * storage). {@code ExternalTaskPattern.afterInsert} credits the task when its declared outputs
 * actually land in the root storage, so a direct root insert is the mechanism RS expects.</p>
 *
 * <p>ASSUMPTION (unverified at runtime): RS credits exactly one iteration per accepted call, so the
 * number of batches delivered is derived from the yuan the sink was actually handed
 * ({@code handed / yuanPerBatch}) rather than from a counter.</p>
 */
final class RsExchangeProviderNode extends PatternProviderNetworkNode {
    /** Bounded so a pathological vein count cannot blow up the container list. */
    static final int PATTERN_SLOTS = 64;

    private Map<UUID, SourceExchangeOffer> offersByPattern = Map.of();
    private List<Pattern> published = List.of();
    private boolean truncationLogged;

    RsExchangeProviderNode() {
        super(0L, PATTERN_SLOTS);
        // RS 3.2.1 registers a pattern provider by its sink id and refuses a null one
        // (PatternProviderNetworkNode#getId does Objects.requireNonNull, which crashed the server
        // tick loop when a network was built). RS 2.0.9 has no such class, so the assignment is
        // reflective and simply skipped on that line.
        try {
            Class<?> idType = Class.forName(
                    "com.refinedmods.refinedstorage.api.autocrafting.task.ExternalPatternSinkId");
            Object id = idType.getMethod("create").invoke(null);
            getClass().getMethod("setId", idType).invoke(this, id);
        } catch (ClassNotFoundException olderRs) {
            // 2.0.9: no sink-id concept; nothing to assign.
        } catch (ReflectiveOperationException failure) {
            ImmortalStorageMod.LOG.error(
                    "[Compat/RS] could not assign the pattern-provider sink id; RS autocrafting "
                            + "will reject this provider", failure);
        }
        setStepBehavior(com.refinedmods.refinedstorage.api.autocrafting.task.StepBehavior.DEFAULT);
        setPriority(0);
    }

    /** Republishes the pattern table; a no-op when nothing changed. */
    void publish(List<SourceExchangeOffer> offers, @Nullable ResourceKey yuan) {
        Map<UUID, SourceExchangeOffer> nextOffers = new LinkedHashMap<>();
        List<Pattern> nextPatterns = new ArrayList<>();
        if (yuan != null) {
            for (SourceExchangeOffer offer : offers) {
                Pattern pattern = RsExchangeOfferPatterns.pattern(offer, yuan);
                if (pattern == null) continue;
                if (nextPatterns.size() >= PATTERN_SLOTS) {
                    if (!truncationLogged) {
                        truncationLogged = true;
                        ImmortalStorageMod.LOG.warn(
                                "[Compat/RS] Xianqiao exchange publishes at most {} patterns; the rest stay storage-only",
                                PATTERN_SLOTS);
                    }
                    break;
                }
                nextPatterns.add(pattern);
                nextOffers.put(pattern.id(), offer);
            }
        }
        if (nextOffers.keySet().equals(offersByPattern.keySet())) return;
        offersByPattern = Map.copyOf(nextOffers);
        published = List.copyOf(nextPatterns);
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            setPattern(slot, slot < published.size() ? published.get(slot) : null);
        }
    }

    int publishedPatternCount() {
        return published.size();
    }

    @Override
    public ExternalPatternSink.Result accept(
            Pattern pattern, Collection<ResourceAmount> inputs, Action action) {
        if (pattern == null || action == null) return ExternalPatternSink.Result.SKIPPED;
        SourceExchangeOffer offer = offersByPattern.get(pattern.id());
        if (offer == null) return ExternalPatternSink.Result.SKIPPED;
        Network network = getNetwork();
        if (network == null) return ExternalPatternSink.Result.REJECTED;
        StorageNetworkComponent storage;
        try {
            storage = network.getComponent(StorageNetworkComponent.class);
        } catch (RuntimeException absent) {
            return ExternalPatternSink.Result.REJECTED;
        }
        if (storage == null) return ExternalPatternSink.Result.REJECTED;
        ResourceKey output = RsExchangeOfferPatterns.output(offer);
        if (output == null) return ExternalPatternSink.Result.SKIPPED;

        long deliverable = deliverableFor(offer, inputs);
        long simulated = storage.insert(output, deliverable, Action.SIMULATE, Actor.EMPTY);
        if (simulated < deliverable) return ExternalPatternSink.Result.REJECTED;
        if (action == Action.SIMULATE) return ExternalPatternSink.Result.ACCEPTED;

        long delivered = storage.insert(output, deliverable, Action.EXECUTE, Actor.EMPTY);
        if (delivered < deliverable) {
            // The ingredient extraction already happened; the honest outcome is a loud failure.
            ImmortalStorageMod.LOG.error(
                    "[Compat/RS] Xianqiao exchange delivered {}/{} of {} for offer {}",
                    delivered, deliverable, offer.outputId(), offer.sourceId());
            return ExternalPatternSink.Result.REJECTED;
        }
        return ExternalPatternSink.Result.ACCEPTED;
    }

    /**
     * How many batches one sink call covers: RS may hand a sink several iterations at once, so the
     * delivered amount must scale with the yuan actually received rather than assume a single batch.
     * Extracted so a GameTest can pin the arithmetic without a live grid.
     */
    static long batchesFor(@org.jetbrains.annotations.Nullable SourceExchangeOffer offer,
                           @org.jetbrains.annotations.Nullable Collection<ResourceAmount> inputs) {
        if (offer == null || inputs == null) return 0L;
        long perBatch = Math.max(1L, offer.yuanPerBatch());
        return Math.max(0L, handedYuan(inputs) / perBatch);
    }

    /** One call's deliverable output count; {@code 0} when the inputs cover no whole batch. */
    static long deliverableFor(@org.jetbrains.annotations.Nullable SourceExchangeOffer offer,
                               @org.jetbrains.annotations.Nullable Collection<ResourceAmount> inputs) {
        long batches = batchesFor(offer, inputs);
        if (offer == null || batches <= 0L) return 0L;
        long perBatchOutputs = Math.max(0L, offer.outputsPerBatch());
        return batches > Long.MAX_VALUE / Math.max(1L, perBatchOutputs)
                ? Long.MAX_VALUE
                : batches * perBatchOutputs;
    }

    private static long handedYuan(Collection<ResourceAmount> inputs) {
        long total = 0L;
        if (inputs == null) return 0L;
        for (ResourceAmount amount : inputs) {
            if (amount == null || amount.amount() <= 0L) continue;
            total = total > Long.MAX_VALUE - amount.amount() ? Long.MAX_VALUE : total + amount.amount();
        }
        return total;
    }
}
