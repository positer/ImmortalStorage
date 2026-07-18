package com.cultivation.core.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExternalResourceCacheSlotTest {
    private static final ResourceChannelKey HYDROGEN =
            new ResourceChannelKey("mekanism_chemical", "mekanism:hydrogen");

    @Test
    void slotKeepsIdentityTargetCacheAndOutputFacesTogether() {
        ExternalResourceCacheSlot slot = new ExternalResourceCacheSlot(
                HYDROGEN, 8_000L, 3_000L,
                ResourceFaceMask.none().with(ResourceFace.NORTH, true));

        assertEquals(HYDROGEN, slot.key());
        assertEquals(8_000L, slot.targetAmount());
        assertEquals(3_000L, slot.cachedAmount());
        assertTrue(slot.canPush(ResourceFace.NORTH, ResourceFaceMode.PUSH));
        assertFalse(slot.canPush(ResourceFace.SOUTH, ResourceFaceMode.PUSH));
        assertFalse(slot.canPush(ResourceFace.NORTH, ResourceFaceMode.PULL));
        assertFalse(slot.canPush(ResourceFace.NORTH, ResourceFaceMode.DISABLED));
    }

    @Test
    void amountsMustRemainNonNegativeAndCacheCannotExceedTarget() {
        assertThrows(IllegalArgumentException.class, () -> new ExternalResourceCacheSlot(
                HYDROGEN, -1L, 0L, ResourceFaceMask.none()));
        assertThrows(IllegalArgumentException.class, () -> new ExternalResourceCacheSlot(
                HYDROGEN, 1L, 2L, ResourceFaceMask.none()));
    }
}
