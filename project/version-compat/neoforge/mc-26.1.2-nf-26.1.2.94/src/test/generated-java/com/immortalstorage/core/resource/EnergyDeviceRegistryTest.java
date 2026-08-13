package com.immortalstorage.core.resource;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EnergyDeviceRegistryTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final UUID OWNER = UUID.fromString("6f725bb1-49b3-4522-9c30-f0ecb6ad63c7");
    private static final UUID OTHER = UUID.fromString("9b552b6e-d39c-425f-8be1-ebad72b69e49");
    private static final String REALM = "immortalstorage:xianqiao_realm/6f725bb1-49b3-4522-9c30-f0ecb6ad63c7";

    @Test
    void staffConfigurationTracksOneDeviceSideAndExactPerTickInputLimit() {
        EnergyDeviceRegistry registry = new EnergyDeviceRegistry();
        EnergyDeviceKey key = new EnergyDeviceKey(OWNER, REALM, 10, 64, -3);

        assertTrue(registry.configure(key, BlockSide.WEST, 4_096L));
        EnergyDeviceBinding binding = registry.get(key).orElseThrow();
        assertEquals(BlockSide.WEST, binding.inputSide());
        assertEquals(4_096L, binding.inputLimitPerTick());
        assertEquals(4_096L, binding.limitRequest(Long.MAX_VALUE));
        assertEquals(17L, binding.limitRequest(17L));
    }

    @Test
    void unchangedConfigurationDoesNotAdvanceRevisionButDirectionOrLimitDoes() {
        EnergyDeviceRegistry registry = new EnergyDeviceRegistry();
        EnergyDeviceKey key = new EnergyDeviceKey(OWNER, REALM, 0, 70, 0);
        registry.configure(key, BlockSide.UP, 100L);
        long firstRevision = registry.revision();

        assertFalse(registry.configure(key, BlockSide.UP, 100L));
        assertEquals(firstRevision, registry.revision());
        assertTrue(registry.configure(key, BlockSide.NORTH, 250L));
        assertEquals(firstRevision + 1L, registry.revision());
    }

    @Test
    void tickViewReturnsOnlyConfiguredDevicesForTheExactOwnerAndDimension() {
        EnergyDeviceRegistry registry = new EnergyDeviceRegistry();
        registry.configure(new EnergyDeviceKey(OWNER, REALM, 1, 2, 3), BlockSide.DOWN, 20L);
        registry.configure(new EnergyDeviceKey(OWNER, "minecraft:overworld", 1, 2, 3), BlockSide.DOWN, 20L);
        registry.configure(new EnergyDeviceKey(OTHER, REALM, 1, 2, 3), BlockSide.DOWN, 20L);

        List<EnergyDeviceBinding> realmBindings = registry.bindingsFor(OWNER, REALM);
        assertEquals(1, realmBindings.size());
        assertEquals(REALM, realmBindings.getFirst().key().dimensionId());
        assertThrows(UnsupportedOperationException.class,
                () -> realmBindings.add(realmBindings.getFirst()));
    }

    @Test
    void removalAndRestoreAreStableAndRejectInvalidLimitsOrDimensionIds() {
        EnergyDeviceRegistry registry = new EnergyDeviceRegistry();
        EnergyDeviceKey key = new EnergyDeviceKey(OWNER, REALM, 2, 80, 2);
        registry.configure(key, BlockSide.SOUTH, Long.MAX_VALUE);
        List<EnergyDeviceBinding> snapshot = registry.snapshot();

        assertTrue(registry.remove(key));
        assertFalse(registry.remove(key));
        registry.restore(snapshot, 77L);
        assertEquals(77L, registry.revision());
        assertEquals(Long.MAX_VALUE, registry.get(key).orElseThrow().inputLimitPerTick());

        assertThrows(IllegalArgumentException.class,
                () -> registry.configure(key, BlockSide.EAST, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new EnergyDeviceKey(OWNER, "bad dimension", 0, 0, 0));
    }
}
