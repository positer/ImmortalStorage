package com.cultivation.cultivation.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JadeGuideOpeningPolicyTest {
    @Test
    void stageZeroCannotOpenGuideButCultivatorsCan() {
        assertFalse(SimpleJadeGuideItem.canOpenGuide(0));
        assertTrue(SimpleJadeGuideItem.canOpenGuide(1));
        assertTrue(SimpleJadeGuideItem.canOpenGuide(10));
    }
}
