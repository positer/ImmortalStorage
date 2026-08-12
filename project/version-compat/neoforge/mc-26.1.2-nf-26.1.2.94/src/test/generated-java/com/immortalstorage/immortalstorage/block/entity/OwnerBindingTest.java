package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OwnerBindingTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void firstClaimWinsAndCannotBeSilentlyRebound() {
        UUID owner = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        OwnerBinding binding = new OwnerBinding();

        assertTrue(binding.claim(owner));
        assertTrue(binding.claim(owner));
        assertFalse(binding.claim(intruder));
        assertEquals(owner, binding.owner());
    }

    @Test
    void ownerBindingRoundTripsThroughManagerNbtShape() {
        UUID owner = UUID.randomUUID();
        OwnerBinding original = new OwnerBinding();
        assertTrue(original.claim(owner));
        CompoundTag tag = new CompoundTag();
        original.save(tag, "Owner");

        OwnerBinding restored = new OwnerBinding();
        restored.load(tag, "Owner");
        assertEquals(owner, restored.owner());
        assertFalse(restored.claim(UUID.randomUUID()));
        assertEquals(owner, restored.owner());
    }
}
