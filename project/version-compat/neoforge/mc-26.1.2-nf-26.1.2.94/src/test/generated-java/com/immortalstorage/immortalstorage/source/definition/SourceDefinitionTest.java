package com.immortalstorage.immortalstorage.source.definition;

import com.immortalstorage.immortalstorage.block.custom.VeinKind;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceDefinitionTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void strictParserAcceptsLongRatesAndRejectsInvalidBoundaries() {
        SourceDefinition definition = SourceDefinitionParser.parse(id("pack:diamond"),
                JsonParser.parseString("""
                        {
                          "type": "item",
                          "item": "minecraft:diamond",
                          "yuan_cost_per_batch": 8,
                          "outputs_per_batch": 4,
                          "min_stage": 8,
                          "default_rate": 2147483648,
                          "max_rate": 9223372036854775807,
                          "core_color": "#70F4FF",
                          "aliases": ["pack:old_diamond"]
                        }
                        """).getAsJsonObject());

        assertEquals(id("pack:diamond"), definition.id());
        assertEquals(2_147_483_648L, definition.defaultRate());
        assertEquals(Long.MAX_VALUE, definition.maxRate());
        assertEquals(0x70F4FF, definition.coreColor());
        assertEquals(List.of(id("pack:old_diamond")), definition.aliases());

        assertThrows(IllegalArgumentException.class, () -> SourceDefinitionParser.parse(id("pack:bad"),
                JsonParser.parseString("""
                        {"type":"fluid","fluid":"minecraft:water","outputs_per_batch":0}
                        """).getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> SourceDefinitionParser.parse(id("pack:bad"),
                JsonParser.parseString("""
                        {"type":"item","item":"minecraft:stone","min_stage":11}
                        """).getAsJsonObject()));
    }

    @Test
    void higherPriorityLayerOverridesBuiltinByStableDefinitionId() {
        Map<Identifier, SourceDefinition> builtins = SourceDefinitions.builtinDefinitions();
        Identifier cobbleId = SourceDefinitions.legacyId(VeinKind.COBBLE);
        SourceDefinition override = new SourceDefinition(cobbleId,
                SourceDefinition.OutputType.ITEM, id("minecraft:deepslate"),
                3L, 7L, 6, 12L, Long.MAX_VALUE,
                "custom.cobble", 0x112233, "dark", List.of(), null);

        Map<Identifier, SourceDefinition> merged = SourceDefinitions.merge(builtins, List.of(override));

        assertEquals(id("minecraft:deepslate"), merged.get(cobbleId).outputId());
        assertEquals(3L, merged.get(cobbleId).yuanCostPerBatch());
        assertEquals(builtins.size(), merged.size());
    }

    @Test
    void oneLayerRejectsDuplicateStableIdsInsteadOfDependingOnFileOrder() {
        SourceDefinition first = definition("pack:same", "minecraft:stone");
        SourceDefinition second = definition("pack:same", "minecraft:dirt");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> SourceDefinitions.merge(Map.of(), List.of(first, second)));
        assertTrue(error.getMessage().contains("duplicate source definition id"));
    }

    @Test
    void differentDefinitionIdsCannotProduceTheSameResourceInOneEffectiveConfiguration() {
        SourceDefinition first = definition("pack:first_stone", "minecraft:stone");
        SourceDefinition second = definition("pack:second_stone", "minecraft:stone");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> SourceDefinitions.merge(Map.of(first.id(), first), List.of(second)));

        assertTrue(error.getMessage().contains("duplicate source output"));
        assertTrue(error.getMessage().contains("pack:first_stone"));
        assertTrue(error.getMessage().contains("pack:second_stone"));
    }

    @Test
    void finalInstalledSnapshotAlsoRejectsDuplicateResourceDefinitions() {
        SourceDefinition first = definition("pack:first_stone", "minecraft:stone");
        SourceDefinition second = definition("pack:second_stone", "minecraft:stone");
        try {
            SourceDefinitions.install(List.of(second, first));

            assertEquals(1, SourceDefinitions.snapshot().definitions().size());
            assertTrue(SourceDefinitions.snapshot().definitions().containsKey(first.id()),
                    "snapshot acceptance is deterministic by stable definition id");
        } finally {
            SourceDefinitions.install(SourceDefinitions.builtinDefinitions().values());
        }
    }

    private static SourceDefinition definition(String definitionId, String outputId) {
        return new SourceDefinition(id(definitionId), SourceDefinition.OutputType.ITEM, id(outputId),
                0L, 1L, 0, 64L, Long.MAX_VALUE,
                "", 0xFFFFFF, "", List.of(), null);
    }

    private static Identifier id(String value) {
        return Identifier.parse(value);
    }
}
