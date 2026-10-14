package com.immortalstorage.immortalstorage.locator;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ToolLocatorLootIndexTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static JsonElement json(String value){return JsonParser.parseString(value);}
    private static JsonElement item(String id){return json("{\"type\":\"minecraft:item\",\"name\":\""+id+"\"}");}
    @Test void modDropMappingNeedsNeitherNamingConventionNorFamilyTagsAndNeverLoadsLootDuringSelection() {
        var tables=Map.of("mod:one",item("mod:charged_bit"),"mod:two",item("mod:charged_bit"),"mod:three",item("mod:different"));
        var calls=new AtomicInteger();
        var index=new ToolLocatorLootIndex(Map.of("mod:boulder_alpha","mod:one","mod:strange_beta","mod:two","mod:third","mod:three"),
                id->{calls.incrementAndGet();return tables.get(id);},id->Set.of());
        int baked=calls.get();
        for(int i=0;i<1000;i++) {
            assertEquals(Set.of("mod:boulder_alpha","mod:strange_beta"),index.select("mod:charged_bit",null));
            assertEquals(Set.of("mod:boulder_alpha","mod:strange_beta"),index.select("mod:boulder_alpha","mod:boulder_alpha"));
        }
        assertEquals(baked,calls.get());assertTrue(index.select("minecraft:stick",null).isEmpty());
    }
    @Test void silkTouchBlockItemsDoNotSeparateOrdinaryAndDeepVariantsAndToolPredicatesAreNotDrops() {
        var a=json("{\"pools\":[{\"entries\":[{\"type\":\"minecraft:alternatives\",\"children\":[{\"type\":\"minecraft:item\",\"name\":\"mod:token_a\",\"conditions\":[{\"items\":\"minecraft:netherite_pickaxe\"}]},{\"type\":\"minecraft:item\",\"name\":\"mod:raw\"}]}]}]}");
        var b=json(a.toString().replace("mod:token_a","mod:token_b"));
        var defs=Map.of("mod:a",a,"mod:b",b);
        var index=new ToolLocatorLootIndex(Map.of("mod:ore_a","mod:a","mod:ore_b","mod:b"),Set.of("mod:token_a","mod:token_b"),defs::get,id->Set.of());
        assertEquals(Set.of("mod:ore_a","mod:ore_b"),index.select("mod:token_a","mod:ore_a"));
        assertTrue(index.select("minecraft:netherite_pickaxe",null).isEmpty());
    }
    @Test void nestedTableReferencesTagsAndCyclesAreTracedWhileLiteralSharedTablesStillFormAVariantFamily() {
        var defs=Map.of("mod:a",json("{\"entries\":[{\"type\":\"minecraft:loot_table\",\"value\":\"mod:b\"}]}"),
                "mod:b",json("{\"entries\":[{\"type\":\"minecraft:loot_table\",\"value\":\"mod:a\"},{\"type\":\"minecraft:tag\",\"name\":\"mod:output\"}]}"));
        var index=new ToolLocatorLootIndex(Map.of("mod:first","mod:a","mod:second","mod:a"),defs::get,id->Set.of("mod:gem"));
        assertEquals(Set.of("mod:first","mod:second"),index.select("mod:gem",null));
        assertEquals(Set.of("mod:first","mod:second"),index.select("mod:first","mod:first"));
    }
    @Test void unconditionalItemReplacementDoesNotIndexTheReplacedItemAsARealDrop() {
        var definition=json("{\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"mod:old\",\"functions\":[{\"function\":\"minecraft:set_item\",\"item\":\"mod:actual\"}]}]}");
        var index=new ToolLocatorLootIndex(Map.of("mod:ore","mod:table"),id->definition,id->Set.of());
        assertEquals(Set.of("mod:ore"),index.select("mod:actual",null));assertTrue(index.select("mod:old",null).isEmpty());
    }
}
