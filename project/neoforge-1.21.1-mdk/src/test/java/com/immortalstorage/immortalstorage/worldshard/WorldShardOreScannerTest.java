package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldShardOreScannerTest {
    @BeforeAll
    static void bootstrap() {
        Bootstrap.bootStrap();
    }

    @Test
    void countAndRarityPlacementModifiersContributeExpectedAttempts() {
        PlacedFeature placed = ore(Feature.ORE, 8,
                List.of(CountPlacement.of(10), RarityFilter.onAverageOnceEvery(2)));

        Map<?, Long> weights = WorldShardOreScanner.scanWeights(List.of(placed), 1);

        assertEquals(40L * WorldShardOreScanner.WEIGHT_SCALE, weights.get(Items.DIAMOND_ORE));
    }

    @Test
    void repeatedFeatureOccurrencesAreWeightedByUniformCandidateBiomeProbability() {
        PlacedFeature placed = ore(Feature.ORE, 8, List.of(CountPlacement.of(4)));

        long oneOfOne = WorldShardOreScanner.scanWeights(List.of(placed), 1).get(Items.DIAMOND_ORE);
        long oneOfTwo = WorldShardOreScanner.scanWeights(List.of(placed), 2).get(Items.DIAMOND_ORE);
        long twoOfTwo = WorldShardOreScanner.scanWeights(List.of(placed, placed), 2).get(Items.DIAMOND_ORE);

        assertEquals(oneOfOne / 2L, oneOfTwo);
        assertEquals(oneOfOne, twoOfTwo,
                "the same placed feature in two biomes is two real occurrences, not an identity duplicate");
    }

    @Test
    void scatteredOreIsIncludedAndOpaqueNonOreFeaturesAreSkipped() {
        PlacedFeature scattered = ore(Feature.SCATTERED_ORE, 5, List.of(CountPlacement.of(2)));
        ConfiguredFeature<?, ?> nonOre = new ConfiguredFeature<>(Feature.NO_OP,
                net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE);
        PlacedFeature opaque = new PlacedFeature(Holder.direct(nonOre), List.of());

        Map<?, Long> weights = WorldShardOreScanner.scanWeights(List.of(scattered, opaque), 1);

        assertEquals(10L * WorldShardOreScanner.WEIGHT_SCALE, weights.get(Items.DIAMOND_ORE));
        assertTrue(weights.size() == 1);
    }

    @Test
    void multipleTargetStatesShareOneFeaturesExpectedYield() {
        OreConfiguration configuration = new OreConfiguration(List.of(
                OreConfiguration.target(new BlockMatchTest(Blocks.STONE),
                        Blocks.DIAMOND_ORE.defaultBlockState()),
                OreConfiguration.target(new BlockMatchTest(Blocks.DEEPSLATE),
                        Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState())), 8);
        ConfiguredFeature<OreConfiguration, Feature<OreConfiguration>> configured =
                new ConfiguredFeature<>(Feature.ORE, configuration);
        PlacedFeature placed = new PlacedFeature(Holder.direct(configured), List.of(CountPlacement.of(4)));

        Map<?, Long> weights = WorldShardOreScanner.scanWeights(List.of(placed), 1);

        assertEquals(16L * WorldShardOreScanner.WEIGHT_SCALE, weights.get(Items.DIAMOND_ORE));
        assertEquals(16L * WorldShardOreScanner.WEIGHT_SCALE, weights.get(Items.DEEPSLATE_DIAMOND_ORE));
        assertEquals(32L * WorldShardOreScanner.WEIGHT_SCALE,
                weights.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    void structurallyCompatibleDynamicOreConfigUsesItsLiveVeinSize() {
        DynamicOreConfiguration configuration = new DynamicOreConfiguration(List.of(
                OreConfiguration.target(new BlockMatchTest(Blocks.STONE),
                        Blocks.DIAMOND_ORE.defaultBlockState()),
                OreConfiguration.target(new BlockMatchTest(Blocks.DEEPSLATE),
                        Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState())), () -> 12);

        WorldShardOreScanner.OreDescriptor descriptor =
                WorldShardOreScanner.describeOreConfiguration(configuration);

        assertEquals(12, descriptor.size());
        assertEquals(Set.of(Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE), descriptor.outputs());
    }


    @Test
    void configuredWeightsModifyTheDetectedWorldPoolWithoutDiscardingUnmentionedOres() {
        Map<net.minecraft.world.item.Item, Long> detected = Map.of(
                Items.DIAMOND, 10L,
                Items.COAL, 20L);
        Map<ResourceLocation, Long> configured = Map.of(
                ResourceLocation.withDefaultNamespace("diamond"), 40L);

        Map<net.minecraft.world.item.Item, Long> merged =
                WorldShardOreScanner.applyConfiguredWeights(detected, configured, false);

        assertEquals(40L, merged.get(Items.DIAMOND));
        assertEquals(20L, merged.get(Items.COAL));
    }

    @Test
    void configuredWeightsCanRemoveEntriesOrExplicitlyReplaceTheDetectedWorldPool() {
        Map<net.minecraft.world.item.Item, Long> detected = Map.of(
                Items.DIAMOND, 10L,
                Items.COAL, 20L);
        Map<ResourceLocation, Long> configured = Map.of(
                ResourceLocation.withDefaultNamespace("diamond"), 0L,
                ResourceLocation.withDefaultNamespace("gold_ingot"), 5L);

        Map<net.minecraft.world.item.Item, Long> overlay =
                WorldShardOreScanner.applyConfiguredWeights(detected, configured, false);
        Map<net.minecraft.world.item.Item, Long> replacement =
                WorldShardOreScanner.applyConfiguredWeights(detected, configured, true);

        assertTrue(!overlay.containsKey(Items.DIAMOND));
        assertEquals(20L, overlay.get(Items.COAL));
        assertEquals(5L, overlay.get(Items.GOLD_INGOT));
        assertEquals(Map.of(Items.GOLD_INGOT, 5L), replacement);
    }

    private static PlacedFeature ore(Feature<OreConfiguration> feature, int veinSize,
                                     List<net.minecraft.world.level.levelgen.placement.PlacementModifier> placement) {
        OreConfiguration configuration = new OreConfiguration(
                new BlockMatchTest(Blocks.STONE), Blocks.DIAMOND_ORE.defaultBlockState(), veinSize);
        ConfiguredFeature<OreConfiguration, Feature<OreConfiguration>> configured =
                new ConfiguredFeature<>(feature, configuration);
        return new PlacedFeature(Holder.direct(configured), placement);
    }

    public record DynamicOreConfiguration(List<OreConfiguration.TargetBlockState> targetStates,
                                          IntSupplier size)
            implements net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration {
    }
}
