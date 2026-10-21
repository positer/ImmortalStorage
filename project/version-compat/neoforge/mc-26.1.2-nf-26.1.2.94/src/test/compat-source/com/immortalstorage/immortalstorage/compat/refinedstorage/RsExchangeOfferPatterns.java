package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.immortalstorage.source.SourceExchangeOffer;
import com.refinedmods.refinedstorage.api.autocrafting.Ingredient;
import com.refinedmods.refinedstorage.api.autocrafting.Pattern;
import com.refinedmods.refinedstorage.api.autocrafting.PatternLayout;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Pure projection of {@code SourceExchangeOffer} to RS 2.0.9 {@code Pattern}s.
 *
 * <p>VERIFIED constructors (javap): {@code Pattern(UUID id, PatternLayout layout)},
 * {@code PatternLayout.external(List<Ingredient>, List<ResourceAmount>)},
 * {@code Ingredient(long amount, List<ResourceKey> inputs)},
 * {@code ResourceAmount(ResourceKey, long)}.</p>
 *
 * <p>A pattern is one batch: ingredient = {@code yuanPerBatch} immortal yuan,
 * output = {@code outputsPerBatch} of the offer's resource. The pattern id is derived from the
 * full offer tuple, so a definition reload that changes a rate produces a new id and RS removes
 * the stale pattern instead of silently repricing it.</p>
 *
 * <p>Free offers ({@code yuanPerBatch == 0}) are deliberately NOT published as patterns: an
 * RS external pattern with no ingredients would mint resources at the sink without ever touching
 * the vein's own cache, which is an infinite-resource exploit. Free veins stay reachable as
 * storage only.</p>
 */
final class RsExchangeOfferPatterns {
    private RsExchangeOfferPatterns() {
    }

    /** The produced resource, or {@code null} when the id no longer resolves on this server. */
    static @Nullable ResourceKey output(SourceExchangeOffer offer) {
        if (offer == null) return null;
        Identifier id = offer.outputId();
        if (offer.fluid()) {
            var fluid = BuiltInRegistries.FLUID.get(id).map(net.minecraft.core.Holder.Reference::value).orElse(null);
            if (fluid == null) return null;
            FluidStack stack = new FluidStack(
                    BuiltInRegistries.FLUID.wrapAsHolder(fluid), 1, DataComponentPatch.EMPTY);
            return new FluidResource(stack.getFluid(), stack.getComponentsPatch());
        }
        Item item = BuiltInRegistries.ITEM.get(id).map(net.minecraft.core.Holder.Reference::value).orElse(null);
        if (item == null) return null;
        return ItemResource.ofItemStack(new ItemStack(item));
    }

    /** One EXTERNAL batch pattern, or {@code null} for free/unresolvable offers. */
    static @Nullable Pattern pattern(SourceExchangeOffer offer, @Nullable ResourceKey yuan) {
        if (offer == null || yuan == null || !offer.paid()) return null;
        ResourceKey output = output(offer);
        if (output == null) return null;
        Ingredient ingredient = new Ingredient(offer.yuanPerBatch(), List.of(yuan));
        PatternLayout layout = PatternLayout.external(
                List.of(ingredient),
                List.of(new ResourceAmount(output, offer.outputsPerBatch())));
        return new Pattern(patternId(offer), layout);
    }

    /** Stable id over every field that affects price or yield. */
    static UUID patternId(SourceExchangeOffer offer) {
        String key = "immortalstorage:rs_exchange\n"
                + offer.sourceId() + "\n"
                + offer.outputId() + "\n"
                + offer.fluid() + "\n"
                + offer.yuanPerBatch() + "\n"
                + offer.outputsPerBatch();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }
}
