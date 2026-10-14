package com.immortalstorage.immortalstorage.locator;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ToolLocatorWatchIndexTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test void movingOnePlayerDoesNotChangeAnotherPlayersWindowOrAnotherDimension() {
        var index=new ToolLocatorWatchIndex<Object>();Object level=new Object(),other=new Object();
        UUID first=UUID.randomUUID(),second=UUID.randomUUID(),third=UUID.randomUUID();
        index.watch(first,level,List.of(1L,2L));index.watch(second,level,List.of(2L,3L));index.watch(third,other,List.of(2L));
        assertEquals(Set.of(first,second),index.nearby(level,2L));assertEquals(Set.of(third),index.nearby(other,2L));
        index.watch(first,level,List.of(4L));
        assertEquals(Set.of(second),index.nearby(level,2L));assertEquals(Set.of(first),index.nearby(level,4L));
        assertTrue(index.nearby(level,1L).isEmpty());assertEquals(Set.of(third),index.nearby(other,2L));
    }
    @Test void logoutRemovesOnlyThatPlayersSubscriptions() {
        var index=new ToolLocatorWatchIndex<Object>();Object level=new Object();UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        index.watch(first,level,List.of(1L));index.watch(second,level,List.of(1L));index.remove(first);
        assertEquals(Set.of(second),index.nearby(level,1L));index.remove(second);assertTrue(index.nearby(level,1L).isEmpty());
    }
    @Test void threeChunkWindowIsExactlySevenBySevenEvenAtNegativeCoordinates() {
        var window=new ToolLocatorWindow(-2,-3);int count=0;
        for(int x=-7;x<=3;x++)for(int z=-8;z<=2;z++)if(window.containsChunk(x,z))count++;
        assertEquals(49,count);assertTrue(window.containsChunk(-5,0));assertFalse(window.containsChunk(-6,0));
    }
}
