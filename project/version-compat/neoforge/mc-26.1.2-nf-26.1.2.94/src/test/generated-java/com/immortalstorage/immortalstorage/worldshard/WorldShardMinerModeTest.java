package com.immortalstorage.immortalstorage.worldshard;

import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldShardMinerModeTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll
    static void bootstrap() {
        Bootstrap.bootStrap();
    }

    @Test
    void parserSupportsBlockTargetColorMultiplierAndExplicitWeights() {
        WorldShardMinerMode mode = WorldShardMinerModeParser.parse(
                Identifier.parse("example:test_file"),
                JsonParser.parseString("""
                        {
                          "id": "example:amber",
                          "activation": {"block": "minecraft:gold_block"},
                          "target_dimension": "minecraft:overworld",
                          "beam_color": "#12AB34",
                          "sampling_multiplier": 1.5,
                          "ore_weights": {"minecraft:diamond": 7}
                        }
                        """).getAsJsonObject());

        assertEquals(Identifier.parse("example:amber"), mode.id());
        assertTrue(mode.activation().matches(Blocks.GOLD_BLOCK.defaultBlockState()));
        assertEquals(Identifier.withDefaultNamespace("overworld"), mode.targetDimension().orElseThrow());
        assertEquals(0xFF12AB34, mode.beamColor());
        assertEquals(1.5D, mode.samplingMultiplier());
        assertEquals(Map.of(Identifier.withDefaultNamespace("diamond"), 7L), mode.explicitOreWeights());
        assertEquals(false, mode.replaceOreWeights(),
                "ore_weights modify the current-world pool unless replacement is explicit");
    }


    @Test
    void parserSupportsExplicitReplacementAndZeroWeightRemoval() {
        WorldShardMinerMode mode = WorldShardMinerModeParser.parse(
                Identifier.parse("example:replacement"),
                JsonParser.parseString("""
                        {
                          "id": "example:replacement",
                          "activation": {"block": "minecraft:gold_block"},
                          "target_dimension": "minecraft:overworld",
                          "beam_color": "#12AB34",
                          "generation_source": "fixed_table",
                          "ore_weights": {
                            "minecraft:diamond": 7,
                            "minecraft:coal": 0
                          }
                        }
                        """).getAsJsonObject());

        assertTrue(mode.replaceOreWeights());
        assertEquals(7L, mode.explicitOreWeights().get(Identifier.withDefaultNamespace("diamond")));
        assertEquals(0L, mode.explicitOreWeights().get(Identifier.withDefaultNamespace("coal")));
    }

    @Test
    void parserRejectsUnknownFieldsAndBadResourcesWithoutAmbiguousErrors() {
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> WorldShardMinerModeParser.parse(Identifier.parse("example:bad"),
                        JsonParser.parseString("""
                                {
                                  "id":"example:bad",
                                  "activation":{"block":"minecraft:diamond_block"},
                                  "target_dimension":"minecraft:overworld",
                                  "beam_color":"#00FF00",
                                  "mystery":true
                                }
                                """).getAsJsonObject()));
        assertTrue(unknown.getMessage().contains("mystery"));

        IllegalArgumentException badBlock = assertThrows(IllegalArgumentException.class,
                () -> WorldShardMinerModeParser.parse(Identifier.parse("example:bad_block"),
                        JsonParser.parseString("""
                                {
                                  "id":"example:bad_block",
                                  "activation":{"block":"missing:not_a_block"},
                                  "target_dimension":"minecraft:overworld",
                                  "beam_color":"#00FF00"
                                }
                                """).getAsJsonObject()));
        assertTrue(badBlock.getMessage().contains("missing:not_a_block"));
    }

    @Test
    void dataPackModeWithSameIdOverridesBuiltinDeterministically() {
        WorldShardMinerMode builtin = WorldShardMinerModes.builtinModes().getFirst();
        WorldShardMinerMode override = new WorldShardMinerMode(
                builtin.id(), WorldShardMinerActivation.forBlock(Blocks.GOLD_BLOCK),
                builtin.targetDimension(), builtin.targetBiomeTag(), 0xFFFFCC00,
                2.0D, Map.of(Identifier.withDefaultNamespace("gold_ingot"), 5L));

        Map<Identifier, WorldShardMinerMode> merged =
                WorldShardMinerModes.mergeDefinitions(List.of(builtin), List.of(override));

        assertEquals(1, merged.size());
        assertEquals(override, merged.get(builtin.id()));
    }
}
