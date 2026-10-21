package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.immortalstorage.immortalstorage.source.SourceExchangeOffer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One AE2 processing pattern projected from a {@link SourceExchangeOffer}.
 *
 * <p>Shape: {@code yuanPerBatch x immortalstorage:immortal_yuan -> outputsPerBatch x resource}.
 * The class is a pure API-level implementation of {@code appeng.api.crafting.IPatternDetails};
 * no AE2 implementation class is referenced, which keeps the 26.1.2 backport limited to whatever
 * the public API keeps.</p>
 *
 * <p>Verified against AE2 19.2.17 ({@code javap}):</p>
 * <ul>
 *   <li>{@code IPatternDetails { AEItemKey getDefinition(); IInput[] getInputs(); default
 *       GenericStack getPrimaryOutput(); List&lt;GenericStack&gt; getOutputs(); ... }} - the default
 *       {@code getPrimaryOutput()} returns {@code getOutputs().get(0)}, which is what AE2 indexes
 *       craftable outputs by.</li>
 *   <li>{@code IPatternDetails.IInput { GenericStack[] getPossibleInputs(); long getMultiplier();
 *       boolean isValid(AEKey, Level); AEKey getRemainingKey(AEKey); }} - AE2's own
 *       {@code AEProcessingPattern$Input} uses a one-count template plus the real amount as the
 *       multiplier and returns {@code null} from {@code getRemainingKey} (consumed input).</li>
 * </ul>
 *
 * <p><b>Unverified assumption:</b> {@code getDefinition()} is treated as display-only (pattern
 * icon). AE2 indexes providers by {@code getPrimaryOutput()} and pushes through
 * {@code getInputs()}/{@code getOutputs()}; the definition is used for terminal tooltips. The
 * Xianqiao Manager block item is therefore used as the icon.</p>
 *
 * <p><b>Owner is part of the identity.</b> A drive can hold disks of several owners at once and
 * one node publishes all of their offers, so two owners whose output and price happen to match
 * would otherwise produce equal patterns. AE2 indexes patterns by their equality through
 * {@code NetworkCraftingProviders}, and {@code pushPattern} selects the settlement wallet from
 * the pattern instance, so the owner has to survive that index untouched.</p>
 */
final class XianqiaoAe2ExchangePattern implements IPatternDetails {
    private final UUID owner;
    private final ResourceLocation outputId;
    private final boolean fluid;
    private final long yuanPerBatch;
    private final long outputsPerBatch;
    private final AEItemKey definition;
    private final List<GenericStack> outputs;
    private final IInput[] inputs;

    private XianqiaoAe2ExchangePattern(SourceExchangeOffer offer, AEItemKey definition,
                                       AEKey outputKey, AEItemKey yuan, UUID owner) {
        this.owner = owner;
        this.outputId = offer.outputId();
        this.fluid = offer.fluid();
        this.yuanPerBatch = offer.yuanPerBatch();
        this.outputsPerBatch = offer.outputsPerBatch();
        this.definition = definition;
        this.outputs = List.of(new GenericStack(outputKey, offer.outputsPerBatch()));
        this.inputs = offer.yuanPerBatch() > 0L
                ? new IInput[]{new YuanInput(yuan, offer.yuanPerBatch())}
                : new IInput[0];
    }

    static @Nullable XianqiaoAe2ExchangePattern of(SourceExchangeOffer offer, AEItemKey definition,
                                                  @Nullable AEItemKey yuan, UUID owner) {
        if (offer == null || definition == null || owner == null) return null;
        // A zero-cost offer has no representable AE2 input; an input-less pattern would make the
        // network mint resources for free (free veins report Long.MAX_VALUE availability), so paid
        // offers only. See XianqiaoAe2CraftingProvider for the full rationale.
        if (offer.yuanPerBatch() <= 0L || yuan == null) return null;
        AEKey outputKey = outputKey(offer);
        if (outputKey == null) return null;
        return new XianqiaoAe2ExchangePattern(offer, definition, outputKey, yuan, owner);
    }

    /** Resolves an offer's output into an AE2 key, or {@code null} when the id is not registered. */
    static @Nullable AEKey outputKey(SourceExchangeOffer offer) {
        if (offer == null) return null;
        if (offer.fluid()) {
            Fluid value = BuiltInRegistries.FLUID.get(offer.outputId());
            return value == null || value == Fluids.EMPTY ? null : AEFluidKey.of(value);
        }
        Item value = BuiltInRegistries.ITEM.get(offer.outputId());
        return value == null || value == Items.AIR ? null : AEItemKey.of(value);
    }

    ResourceLocation outputId() {
        return outputId;
    }

    boolean fluid() {
        return fluid;
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs.clone();
    }

    @Override
    public List<GenericStack> getOutputs() {
        return outputs;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof XianqiaoAe2ExchangePattern pattern)) return false;
        return owner.equals(pattern.owner)
                && fluid == pattern.fluid
                && yuanPerBatch == pattern.yuanPerBatch
                && outputsPerBatch == pattern.outputsPerBatch
                && outputId.equals(pattern.outputId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, outputId, fluid, yuanPerBatch, outputsPerBatch);
    }

    @Override
    public String toString() {
        return "XianqiaoExchangePattern[" + owner + ' ' + outputId + (fluid ? " (fluid)" : "")
                + " " + yuanPerBatch + " yuan -> " + outputsPerBatch + "]";
    }

    /** The single consumed yuan slot; mirrors {@code AEProcessingPattern$Input} semantics. */
    private record YuanInput(GenericStack[] template, long multiplier) implements IInput {
        YuanInput(AEItemKey yuan, long amount) {
            this(new GenericStack[]{new GenericStack(yuan, 1L)}, amount);
        }

        @Override
        public GenericStack[] getPossibleInputs() {
            return template.clone();
        }

        @Override
        public long getMultiplier() {
            return multiplier;
        }

        @Override
        public boolean isValid(AEKey what, Level level) {
            return what != null && what.matches(template[0]);
        }

        @Override
        public @Nullable AEKey getRemainingKey(AEKey what) {
            return null;
        }
    }
}
