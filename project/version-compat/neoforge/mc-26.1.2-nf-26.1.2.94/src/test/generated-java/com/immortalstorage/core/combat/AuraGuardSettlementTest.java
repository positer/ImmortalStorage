package com.immortalstorage.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuraGuardSettlementTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test void spendsTrueYuanOneForOneAndRetainsFractionalCredit() {
        AuraGuardSettlement.Result first = AuraGuardSettlement.settle(1.25D, 10L, 0L, 0L);
        assertEquals(1.25D, first.blockedDamage(), 1.0E-6D);
        assertEquals(0.0D, first.healthDamage(), 1.0E-6D);
        assertEquals(2L, first.trueYuanSpent());
        assertEquals(750_000L, first.endingCredit());

        AuraGuardSettlement.Result second = AuraGuardSettlement.settle(
                0.75D, 8L, 0L, first.endingCredit());
        assertEquals(0L, second.trueYuanSpent());
        assertEquals(0L, second.endingCredit());
        assertTrue(second.fullyBlocked(0.75D));
    }

    @Test void convertsTheMinimumWholeImmortalYuanAndReturnsRemainder() {
        AuraGuardSettlement.Result result = AuraGuardSettlement.settle(70.5D, 3L, 2L, 0L);
        assertEquals(3L, result.trueYuanSpent());
        assertEquals(2L, result.immortalYuanSpent());
        assertEquals(60L, result.convertedTrueYuanRemainder());
        assertEquals(500_000L, result.endingCredit());
        assertEquals(0.0D, result.healthDamage(), 1.0E-6D);
        assertTrue(result.fullyBlocked(70.5D));
    }

    @Test void partiallyBlocksWhenBothCurrenciesAreInsufficient() {
        AuraGuardSettlement.Result result = AuraGuardSettlement.settle(100.0D, 2L, 1L, 0L);
        assertEquals(66.0D, result.blockedDamage(), 1.0E-6D);
        assertEquals(34.0D, result.healthDamage(), 1.0E-6D);
        assertEquals(2L, result.trueYuanSpent());
        assertEquals(1L, result.immortalYuanSpent());
        assertFalse(result.fullyBlocked(100.0D));
    }

    @Test void appliesOnlyTheUnfundedRemainderToHealthAfterBothBalancesAreExhausted() {
        AuraGuardSettlement.Result result = AuraGuardSettlement.settle(70.5D, 3L, 1L, 0L);

        assertEquals(67.0D, result.blockedDamage(), 1.0E-6D);
        assertEquals(3.5D, result.healthDamage(), 1.0E-6D);
        assertEquals(3L, result.trueYuanSpent());
        assertEquals(1L, result.immortalYuanSpent());
        assertEquals(0L, result.convertedTrueYuanRemainder());
        assertEquals(0L, result.endingCredit());
    }

    @Test void invalidOrZeroDamageDoesNotSpendAnything() {
        for (double damage : new double[]{0.0D, -1.0D, Double.NaN, Double.POSITIVE_INFINITY}) {
            AuraGuardSettlement.Result result = AuraGuardSettlement.settle(damage, 5L, 5L, 123L);
            assertEquals(0L, result.trueYuanSpent());
            assertEquals(0L, result.immortalYuanSpent());
            assertEquals(0.0D, result.healthDamage(), 1.0E-6D);
            assertEquals(123L, result.endingCredit());
        }
    }
}
