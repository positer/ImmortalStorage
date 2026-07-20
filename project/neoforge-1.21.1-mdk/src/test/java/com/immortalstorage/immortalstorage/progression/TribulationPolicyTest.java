package com.immortalstorage.immortalstorage.progression;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TribulationPolicyTest {
    @Test
    void defaultsUseOneHostileTargetForEachAscendedTransition() {
        assertEquals(ResourceLocation.withDefaultNamespace("zombie"),
                TribulationPolicy.defaultTargetId(6));
        assertEquals(ResourceLocation.withDefaultNamespace("wither_skeleton"),
                TribulationPolicy.defaultTargetId(7));
        assertEquals(ResourceLocation.withDefaultNamespace("vindicator"),
                TribulationPolicy.defaultTargetId(8));
        assertEquals(ResourceLocation.withDefaultNamespace("warden"),
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
