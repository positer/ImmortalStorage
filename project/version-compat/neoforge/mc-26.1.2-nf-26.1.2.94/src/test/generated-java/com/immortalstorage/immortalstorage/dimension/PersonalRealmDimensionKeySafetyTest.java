package com.immortalstorage.immortalstorage.dimension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Regression coverage for nullable respawn dimensions during logout. */
final class PersonalRealmDimensionKeySafetyTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void nullableDimensionKeysAreSafeForRealmGuards() {
        assertFalse(ImmortalStorageDimensions.isXianqiaoRealm(null));
        assertFalse(ImmortalStorageDimensions.isPersonalRealmFor(null, UUID.randomUUID()));
        assertFalse(ImmortalStorageDimensions.isPersonalRealmFor(null, null));
        assertTrue(ImmortalStorageDimensions.personalRealmOwner(null).isEmpty());
    }
}
