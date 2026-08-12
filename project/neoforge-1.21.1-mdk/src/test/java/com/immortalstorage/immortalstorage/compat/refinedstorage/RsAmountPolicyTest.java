package com.immortalstorage.immortalstorage.compat.refinedstorage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RsAmountPolicyTest {
    @Test
    void infiniteDirectoryAmountsKeepTheRsLongAggregationContract() {
        RsAmountPolicy.setLongAmountApiSupported(true);
        assertEquals(Long.MAX_VALUE, RsAmountPolicy.advertised(Long.MAX_VALUE));
        assertEquals(4_294_967_294L,
                RsAmountPolicy.saturatedSum(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, RsAmountPolicy.saturatedSum(Long.MAX_VALUE - 2L, 3L));
    }

    @Test
    void nativeLongTransfersKeepTheOriginalRequestAndNeverReturnMore() {
        assertEquals(Long.MAX_VALUE, RsAmountPolicy.boundedTransfer(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(64L, RsAmountPolicy.boundedTransfer(64L, 128L));
        assertEquals(0L, RsAmountPolicy.boundedTransfer(-1L, 64L));
    }
}
