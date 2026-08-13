package com.immortalstorage.immortalstorage.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JadeGuideOpeningPolicyTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void stageZeroCannotOpenGuideButCultivatorsCan() {
        assertFalse(SimpleJadeGuideItem.canOpenGuide(0));
        assertTrue(SimpleJadeGuideItem.canOpenGuide(1));
        assertTrue(SimpleJadeGuideItem.canOpenGuide(10));
    }
}
