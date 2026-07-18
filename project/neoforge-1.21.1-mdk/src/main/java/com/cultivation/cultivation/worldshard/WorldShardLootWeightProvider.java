package com.cultivation.cultivation.worldshard;

/**
 * Replaceable weighting boundary. The default uses the explicit datapack
 * weight because vanilla's reloadable loot-table registry does not expose a
 * structure-placement frequency for chest tables. Integrations can supply a
 * provider derived from their own world-generation metadata without changing
 * selection or generation code.
 */
@FunctionalInterface
public interface WorldShardLootWeightProvider {
    long weight(WorldShardLootDefinition definition);

    static WorldShardLootWeightProvider configured() {
        return WorldShardLootDefinition::weight;
    }
}
