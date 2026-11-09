package com.immortalstorage.immortalstorage.client.screen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ConfigurationWindowBoundsTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test void normalWindowsStayReachableAtEdges() {
        assertEquals(0,ConfigurationWindowBounds.x(-100,800,356));
        assertEquals(444,ConfigurationWindowBounds.x(900,800,356));
        assertEquals(38,ConfigurationWindowBounds.y(-100,600,240));
        assertEquals(360,ConfigurationWindowBounds.y(900,600,240));
    }
    @Test void oversizedWindowCanMoveLeftToExposeItsSettings() {
        assertEquals(-156,ConfigurationWindowBounds.x(-999,200,356));
        assertEquals(0,ConfigurationWindowBounds.x(999,200,356));
        assertEquals(-12,ConfigurationWindowBounds.y(-999,100,240));
        assertEquals(-3,ConfigurationWindowBounds.y(-999,240,243));
    }
}
