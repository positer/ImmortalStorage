package com.immortalstorage.core.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExternalResourceChannelsTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void interfaceCacheLimitsMatchResourceScale() {
        assertEquals(100_000_000L, ExternalResourceChannels.cacheLimit(ExternalResourceChannels.FE));
        assertEquals(1_000_000L, ExternalResourceChannels.cacheLimit(ExternalResourceChannels.BOTANIA_MANA));
        assertEquals(10_000L, ExternalResourceChannels.cacheLimit(ExternalResourceChannels.ARS_NOUVEAU_SOURCE));
        assertEquals(1_350L, ExternalResourceChannels.cacheLimit(
                ExternalResourceChannels.INDUSTRIAL_FOREGOING_SOUL));
        assertEquals(1_000_000L, ExternalResourceChannels.cacheLimit(
                ExternalResourceChannels.mekanismChemical("mekanism:oxygen")));
    }
    @Test
    void blockDiscoveredManaAndSourceBypassAllSidedTransferConfiguration() {
        assertTrue(ExternalResourceChannels.usesDirectionlessBlockInteraction(
                ExternalResourceChannels.BOTANIA_MANA));
        assertTrue(ExternalResourceChannels.usesDirectionlessBlockInteraction(
                ExternalResourceChannels.ARS_NOUVEAU_SOURCE));
    }

    @Test
    void capabilitiesWithPhysicalDirectionsRemainSided() {
        assertFalse(ExternalResourceChannels.usesDirectionlessBlockInteraction(
                ExternalResourceChannels.FE));
        assertFalse(ExternalResourceChannels.usesDirectionlessBlockInteraction(
                ExternalResourceChannels.INDUSTRIAL_FOREGOING_SOUL));
        assertFalse(ExternalResourceChannels.usesDirectionlessBlockInteraction(
                ExternalResourceChannels.mekanismChemical("mekanism:oxygen")));
    }
}
