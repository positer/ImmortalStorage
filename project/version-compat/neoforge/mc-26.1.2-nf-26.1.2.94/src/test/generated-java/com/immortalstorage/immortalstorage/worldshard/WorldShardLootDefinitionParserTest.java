package com.immortalstorage.immortalstorage.worldshard;

import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WorldShardLootDefinitionParserTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void parsesExternalModeLootTableWeightAndSourceSeed() {
        WorldShardLootDefinition definition = WorldShardLootDefinitionParser.parse(
                Identifier.parse("pack:custom_end_city"),
                JsonParser.parseString("""
                        {"mode":"immortalstorage:end","loot_table":"pack:chests/end_city",
                         "weight":37,"source_seed":112}
                        """).getAsJsonObject());

        assertEquals(Identifier.parse("pack:custom_end_city"), definition.id());
        assertEquals(WorldShardMinerModes.END, definition.mode());
        assertEquals(Identifier.parse("pack:chests/end_city"), definition.lootTable());
        assertEquals(37L, definition.weight());
        assertEquals(112L, definition.sourceSeed());
    }

    @Test
    void zeroWeightIsAnExplicitRemovalOverride() {
        WorldShardLootDefinition removal = WorldShardLootDefinitionParser.parse(
                Identifier.parse("pack:remove_builtin"), JsonParser.parseString("""
                        {"mode":"immortalstorage:overworld","loot_table":"pack:chests/test","weight":0}
                        """).getAsJsonObject());

        assertEquals(0L, removal.weight());
        assertEquals(0L, removal.sourceSeed());
    }
}
