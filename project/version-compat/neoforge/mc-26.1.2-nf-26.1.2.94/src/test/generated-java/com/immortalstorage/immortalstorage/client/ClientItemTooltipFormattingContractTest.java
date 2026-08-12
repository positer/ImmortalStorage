package com.immortalstorage.immortalstorage.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientItemTooltipFormattingContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void temperingPercentIsLimitedToTwoDecimalPlaces() {
        assertEquals("0", ClientItemTooltips.formatTemperingPercent(0.0D));
        assertEquals("1", ClientItemTooltips.formatTemperingPercent(0.01D));
        assertEquals("1.5", ClientItemTooltips.formatTemperingPercent(0.015F));
        assertEquals("1.23", ClientItemTooltips.formatTemperingPercent(0.012345D));
    }
}
