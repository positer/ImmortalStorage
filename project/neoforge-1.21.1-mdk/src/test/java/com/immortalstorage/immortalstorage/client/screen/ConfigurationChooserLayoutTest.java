package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationChooserLayoutTest {
    @Test void heightFollowsVisibleRowsWithoutChangingTheFiveRowScrollWindow() {
        assertEquals(52, ConfigurationStyle.chooserHeight(1));
        assertEquals(72, ConfigurationStyle.chooserHeight(2));
        assertEquals(132, ConfigurationStyle.chooserHeight(5));
        assertEquals(132, ConfigurationStyle.chooserHeight(30));
        for (int rows = 1; rows <= 5; rows++) {
            int bottom = ConfigurationStyle.CHOOSER_TOP + (rows - 1) * ConfigurationStyle.CHOOSER_ROW + 18;
            assertTrue(ConfigurationStyle.chooserHeight(rows) - bottom >= 6);
        }
    }
}
