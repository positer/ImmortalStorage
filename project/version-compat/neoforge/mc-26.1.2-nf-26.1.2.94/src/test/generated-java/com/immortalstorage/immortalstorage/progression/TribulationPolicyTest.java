package com.immortalstorage.immortalstorage.progression;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TribulationPolicyTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void defaultsUseOneHostileTargetForEachAscendedTransition() {
        assertEquals(Identifier.withDefaultNamespace("zombie"),
                TribulationPolicy.defaultTargetId(6));
        assertEquals(Identifier.withDefaultNamespace("wither_skeleton"),
                TribulationPolicy.defaultTargetId(7));
        assertEquals(Identifier.withDefaultNamespace("vindicator"),
                TribulationPolicy.defaultTargetId(8));
        assertEquals(Identifier.withDefaultNamespace("warden"),
                TribulationPolicy.defaultTargetId(9));
    }

    @Test
    void normalProgressionAndTribulationShareTheConfiguredMaximumStage() {
        assertTrue(TribulationPolicy.allowsNormalAdvance(5, 6, 10));
        assertTrue(TribulationPolicy.allowsNormalAdvance(8, 9, 9));
        assertFalse(TribulationPolicy.allowsNormalAdvance(9, 10, 9));
        assertFalse(TribulationPolicy.canStart(5, 10));
        assertTrue(TribulationPolicy.canStart(6, 10));
        assertFalse(TribulationPolicy.canStart(9, 9));
    }

    @Test
    void higherTribulationsApplyTheSpecifiedPlayerDebuffs() {
        assertFalse(TribulationPolicy.requiresBlindness(7));
        assertTrue(TribulationPolicy.requiresBlindness(8));
        assertTrue(TribulationPolicy.requiresBlindness(9));
        assertFalse(TribulationPolicy.requiresWither(8));
        assertTrue(TribulationPolicy.requiresWither(9));
    }
}
