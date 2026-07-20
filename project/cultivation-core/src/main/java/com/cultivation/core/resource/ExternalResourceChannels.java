package com.cultivation.core.resource;

/**
 * Stable, loader-neutral identities for independently persisted optional
 * resource systems. A channel must never be reused as an alias for another
 * mod's resource type.
 */
public final class ExternalResourceChannels {
    public static final String MEKANISM_CHEMICAL_CHANNEL = "mekanism_chemical";
    public static final ResourceChannelKey FE =
            new ResourceChannelKey("energy", "neoforge:fe");
    public static final ResourceChannelKey BOTANIA_MANA =
            new ResourceChannelKey("botania_mana", "botania:mana");
    public static final ResourceChannelKey ARS_NOUVEAU_SOURCE =
            new ResourceChannelKey("ars_nouveau_source", "ars_nouveau:source");
    public static final ResourceChannelKey INDUSTRIAL_FOREGOING_SOUL =
            new ResourceChannelKey("industrial_foregoing_soul", "industrialforegoingsouls:soul");

    /** Stable identity for one Mekanism chemical registry entry. */
    public static ResourceChannelKey mekanismChemical(String chemicalId) {
        return new ResourceChannelKey(MEKANISM_CHEMICAL_CHANNEL, chemicalId);
    }

    /** Maximum amount one interface cache slot may request for each resource family. */
    public static long cacheLimit(ResourceChannelKey key) {
        if (key == null) return 0L;
        if (FE.equals(key)) return 100_000_000L;
        if (BOTANIA_MANA.equals(key)) return 1_000_000L;
        if (ARS_NOUVEAU_SOURCE.equals(key)) return 10_000L;
        if (INDUSTRIAL_FOREGOING_SOUL.equals(key)) return 1_350L;
        if (MEKANISM_CHEMICAL_CHANNEL.equals(key.channel())) return 1_000_000L;
        return 1_000_000L;
    }

    public static long clampCacheAmount(ResourceChannelKey key, long requested) {
        return Math.min(cacheLimit(key), Math.max(0L, requested));
    }

    /**
     * Returns whether the target mod discovers and operates the resource at the
     * block itself without supplying a physical face.
     *
     * <p>These channels must transact directly with the interface cache. UI
     * face modes, per-target face masks and the active push/pull switches are
     * deliberately irrelevant because the target API has no sided operation
     * to which those settings could be applied.</p>
     */
    public static boolean usesDirectionlessBlockInteraction(ResourceChannelKey key) {
        return BOTANIA_MANA.equals(key) || ARS_NOUVEAU_SOURCE.equals(key);
    }

    private ExternalResourceChannels() {
    }
}
