package com.immortalstorage.immortalstorage.item.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ModWeaponAttackProjectionTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void combinesPaidDamageAndGrowthBeforeExternalAttributeMultipliers() {
        assertEquals(517.0F, ModWeaponAttackProjection.calculate(5.0F, 512.0F, true, 0L, 0.01D));
        assertEquals(1034.0F, ModWeaponAttackProjection.calculate(5.0F, 512.0F, true, 100L, 0.01D));
        assertEquals(10.0F, ModWeaponAttackProjection.calculate(5.0F, 512.0F, false, 100L, 0.01D));
    }
}
