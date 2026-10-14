package com.immortalstorage.immortalstorage.locator;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolLocatorRulesTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test void renderingRespectsExploreMiningAndDisabledStates() {
        assertEquals(0,ToolLocatorRules.visibleCategory(15,false,false));
        assertEquals(ToolLocatorRules.STORAGE,ToolLocatorRules.visibleCategory(1,true,false));
        assertEquals(ToolLocatorRules.SPAWNER,ToolLocatorRules.visibleCategory(2,true,false));
        assertEquals(ToolLocatorRules.ARCHAEOLOGY,ToolLocatorRules.visibleCategory(4,true,false));
        assertEquals(ToolLocatorRules.ORE,ToolLocatorRules.visibleCategory(8,false,true));
        assertEquals(0,ToolLocatorRules.visibleCategory(8,true,false));
        assertEquals(0,ToolLocatorRules.visibleCategory(1,false,true));
    }
    @Test void overlappingCategoriesHaveDeterministicColoursWithoutHidingOreInMiningOnlyMode() {
        assertEquals(ToolLocatorRules.ARCHAEOLOGY,ToolLocatorRules.visibleCategory(15,true,true));
        assertEquals(ToolLocatorRules.SPAWNER,ToolLocatorRules.visibleCategory(11,true,true));
        assertEquals(ToolLocatorRules.ORE,ToolLocatorRules.visibleCategory(15,false,true));
    }
    @Test void optionalRangeUsesConfigurationAndExplicitDatapackRangeIsValidated() {
        assertEquals(32,ToolLocatorSettings.DEFAULT_RADIUS);
        assertEquals(16,ToolLocatorSettings.DEFAULT_MINING_RADIUS);
        assertEquals(0,ToolLocatorSettings.parseMiningRadius(JsonParser.parseString("{}").getAsJsonObject()));
        assertEquals(16,ToolLocatorSettings.parseMiningRadius(JsonParser.parseString("{\"mining_radius\":16,\"radius\":32}").getAsJsonObject()));
        assertEquals(0,ToolLocatorSettings.parseRadius(JsonParser.parseString("{}").getAsJsonObject()));
        assertEquals(48,ToolLocatorSettings.parseRadius(JsonParser.parseString("{\"radius\":48}").getAsJsonObject()));
        for(String bad:new String[]{"0","49","-1","1.5","\"32\"","null"})
            assertThrows(RuntimeException.class,()->ToolLocatorSettings.parseRadius(JsonParser.parseString("{\"radius\":"+bad+"}").getAsJsonObject()));
    }
}
