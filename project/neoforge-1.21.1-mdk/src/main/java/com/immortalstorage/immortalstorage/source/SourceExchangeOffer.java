package com.immortalstorage.immortalstorage.source;

import net.minecraft.resources.ResourceLocation;

/**
 * One exchange offer projected from a source vein the owner currently connects.
 *
 * <p>2026-10-21 user ruling: the exchange recipes an AE2/RS network sees must depend
 * entirely on which source veins are actually connected to Xianqiao. This record is the
 * single shape both the AE2 and the RS providers consume, so neither of them invents an
 * offer list of its own.</p>
 *
 * @param sourceId        stable id of the contributing vein/member
 * @param outputId        the resource produced
 * @param fluid           whether {@code outputId} names a fluid instead of an item
 * @param yuanPerBatch    Immortal Yuan spent per production batch
 * @param outputsPerBatch units produced per batch
 */
public record SourceExchangeOffer(String sourceId, ResourceLocation outputId, boolean fluid,
                                  long yuanPerBatch, long outputsPerBatch) {
    public SourceExchangeOffer {
        if (sourceId == null || sourceId.isBlank()) throw new IllegalArgumentException("sourceId is required");
        if (outputId == null) throw new IllegalArgumentException("outputId is required");
        if (yuanPerBatch < 0L) throw new IllegalArgumentException("yuanPerBatch must not be negative");
        if (outputsPerBatch <= 0L) throw new IllegalArgumentException("outputsPerBatch must be positive");
    }

    /** True when this vein converts the owner's yuan (a paid definition). */
    public boolean paid() {
        return yuanPerBatch > 0L;
    }
}
