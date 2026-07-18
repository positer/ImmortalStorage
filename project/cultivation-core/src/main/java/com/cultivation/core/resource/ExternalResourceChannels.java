package com.cultivation.core.resource;

/**
 * Stable, loader-neutral identities for independently persisted optional
 * resource systems. A channel must never be reused as an alias for another
 * mod's resource type.
 */
public final class ExternalResourceChannels {
    public static final ResourceChannelKey FE =
            new ResourceChannelKey("energy", "neoforge:fe");
    public static final ResourceChannelKey BOTANIA_MANA =
            new ResourceChannelKey("botania_mana", "botania:mana");
    public static final ResourceChannelKey ARS_NOUVEAU_SOURCE =
            new ResourceChannelKey("ars_nouveau_source", "ars_nouveau:source");
    public static final ResourceChannelKey IRONS_SPELLBOOKS_MANA =
            new ResourceChannelKey("irons_spellbooks_mana", "irons_spellbooks:mana");
    public static final ResourceChannelKey INDUSTRIAL_FOREGOING_SOUL =
            new ResourceChannelKey("industrial_foregoing_soul", "industrialforegoingsouls:soul");

    private ExternalResourceChannels() {
    }
}
