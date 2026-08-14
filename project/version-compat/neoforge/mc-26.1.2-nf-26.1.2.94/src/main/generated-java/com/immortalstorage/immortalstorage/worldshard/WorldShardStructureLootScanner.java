package com.immortalstorage.immortalstorage.worldshard;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dynamically discovers every structure chest loot table from the current
 * world datapack (instead of a hard-coded whitelist).  A table is a "structure
 * chest" when its location lives under {@code chests/}, which is the vanilla
 * convention every structure treasure uses (ancient_city, stronghold, bastion,
 * modded structures, ...).  The scanner reads the live LOOT_TABLE registry so
 * datapack overrides and other mods' newly added structure chests are picked up
 * automatically on the next reload.
 *
 * Each discovered table is assigned to the dimension whose structure actually
 * generates it (see {@link #modeFor(Identifier)}), so an End-mode basin
 * rolls the End-city treasure table and a Nether-mode basin rolls Nether
 * structure chests instead of every dimension's chests diluting one another.
 * Ore pools remain dimension-scoped via {@link WorldShardOreScanner}.
 */
public final class WorldShardStructureLootScanner {
    private static final Logger LOG = LoggerFactory.getLogger(ImmortalStorageMod.MODID + ".worldshard.loot");

    private WorldShardStructureLootScanner() {}

    /**
     * Enumerate every {@code <ns>:chests/<path>} loot table in the world's
     * LOOT_TABLE registry and emit one definition in the mining mode whose
     * dimension actually generates that structure chest.
     */
    public static List<WorldShardLootDefinition> discover(RegistryAccess registryAccess) {
        Registry<LootTable> registry = registryAccess.lookupOrThrow(Registries.LOOT_TABLE);
        List<WorldShardLootDefinition> discovered = new ArrayList<>();
        for (Identifier id : registry.keySet()) {
            if (!id.getPath().startsWith("chests/")) continue;
            Identifier mode = modeFor(id);
            long weight = defaultWeight(id);
            discovered.add(new WorldShardLootDefinition(
                    Identifier.fromNamespaceAndPath(ImmortalStorageMod.MODID,
                            "structure_" + id.getNamespace() + "_" + id.getPath().replace('/', '_')),
                    mode, id, weight, 0L));
        }
        discovered.sort(Comparator.comparing(d -> d.id().toString()));
        LOG.info("Discovered {} structure chest loot tables",
                discovered.size());
        return List.copyOf(discovered);
    }

    /**
     * Assign a discovered chest table to the dimension whose structure
     * generates it.  The End and Nether use a fixed, Mojang-defined structure
     * chest set (end city treasure, nether bridge, bastion variants); every
     * other {@code chests/*} table — including modded structures — belongs to
     * the Overworld.  This keeps each basin mode's window focused on its own
     * dimension instead of diluting End across every chest table.
     */
    static Identifier modeFor(Identifier id) {
        String path = id.getPath();
        if (path.equals("chests/end_city_treasure")) {
            return WorldShardMinerModes.END;
        }
        if (path.equals("chests/nether_bridge") || path.startsWith("chests/bastion_")) {
            return WorldShardMinerModes.NETHER;
        }
        return WorldShardMinerModes.OVERWORLD;
    }

    /** Stable default weight; datapack {@code world_shard_loot} entries still override on top. */
    static long defaultWeight(Identifier id) {
        return 8L;
    }
}
