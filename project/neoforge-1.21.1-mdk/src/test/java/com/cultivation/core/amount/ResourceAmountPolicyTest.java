package com.cultivation.core.amount;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ResourceAmountPolicyTest {
    @Test
    void unchangedResourcesBehaveLikeCreativeSinksAcrossLongAndIntViews() {
        ResourceAmountPolicy policy = ResourceAmountPolicy.UNCHANGED;

        assertEquals(Long.MAX_VALUE, policy.reportedLong(0L));
        assertEquals(Integer.MAX_VALUE, policy.reportedInt(0L));
        assertEquals(Long.MAX_VALUE, policy.extractable(0L, Long.MAX_VALUE));
        assertEquals(0L, policy.afterExtract(0L, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, policy.insertable(0L, Long.MAX_VALUE));
        assertEquals(0L, policy.afterInsert(0L, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, policy.longPerTickCeiling(0L));
        assertEquals(Integer.MAX_VALUE, policy.intPerTickCeiling(0L));
    }

    @Test
    void consumedResourcesStillUseTheirRealFiniteBalance() {
        ResourceAmountPolicy policy = ResourceAmountPolicy.CONSUMED;

        assertEquals(10L, policy.reportedLong(10L));
        assertEquals(10, policy.reportedInt(10L));
        assertEquals(7L, policy.extractable(10L, 7L));
        assertEquals(3L, policy.afterExtract(10L, 7L));
        assertEquals(7L, policy.insertable(10L, 7L));
        assertEquals(17L, policy.afterInsert(10L, 7L));
        assertEquals(Long.MAX_VALUE, policy.afterInsert(Long.MAX_VALUE - 1L, 7L));
        assertEquals(10L, policy.longPerTickCeiling(10L));
        assertEquals(10, policy.intPerTickCeiling(10L));
    }
}
