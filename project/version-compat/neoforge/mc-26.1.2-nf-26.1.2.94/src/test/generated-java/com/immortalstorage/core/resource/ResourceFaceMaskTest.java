package com.immortalstorage.core.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResourceFaceMaskTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void eachOfTheSixFacesCanBeEnabledIndependently() {
        ResourceFaceMask mask = ResourceFaceMask.none()
                .with(ResourceFace.UP, true)
                .with(ResourceFace.SOUTH, true);

        assertTrue(mask.includes(ResourceFace.UP));
        assertTrue(mask.includes(ResourceFace.SOUTH));
        assertFalse(mask.includes(ResourceFace.DOWN));
        assertFalse(mask.includes(ResourceFace.NORTH));
        assertFalse(mask.includes(ResourceFace.WEST));
        assertFalse(mask.includes(ResourceFace.EAST));
        assertEquals((1 << ResourceFace.UP.ordinal()) | (1 << ResourceFace.SOUTH.ordinal()),
                mask.bits());
    }

    @Test
    void persistedBitsAreClampedToSixFacesAndRoundTrip() {
        ResourceFaceMask decoded = ResourceFaceMask.fromBits(0xFFFF);
        assertEquals(0x3F, decoded.bits());
        assertEquals(ResourceFaceMask.all(), decoded);
        assertEquals(ResourceFaceMask.none(), ResourceFaceMask.fromBits(-64));
    }

    @Test
    void changingOneFaceDoesNotMutateThePreviousValue() {
        ResourceFaceMask all = ResourceFaceMask.all();
        ResourceFaceMask withoutEast = all.with(ResourceFace.EAST, false);

        assertTrue(all.includes(ResourceFace.EAST));
        assertFalse(withoutEast.includes(ResourceFace.EAST));
        assertEquals(0x1F, withoutEast.bits());
    }
}
