package com.immortalstorage.immortalstorage.compressor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

/** The exact Buildings fixture, relative to its miniature-ruin controller. */
public final class SpiritCompressorStructure {
    public static String blockId(int x, int y, int z) {
        if (y == 0) {
            if (x == 0 && z == 0) return "immortalstorage:miniature_immortal_ruin_block";
            return x != 0 && z != 0 ? "minecraft:chain" : "minecraft:air";
        }
        if (x == 0 && z == 0) return y < 0 ? "immortalstorage:world_shard_miner" : "immortalstorage:treasure_basin";
        return x != 0 && z != 0 ? "immortalstorage:world_barrier" : "minecraft:chain";
    }

    public static boolean matches(Level level, BlockPos center) {
        if (level == null) return false;
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
            BlockPos pos = center.offset(x, y, z);
            if (!level.hasChunkAt(pos)) return false;
            String expected = blockId(x, y, z);
            String actual = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
            // Iron chain was renamed in 26.1; the structure has identical geometry.
            if (expected.equals("minecraft:chain")) {
                if (!actual.equals(expected) && !actual.equals("minecraft:iron_chain")) return false;
            } else if (!actual.equals(expected)) return false;
        }
        return true;
    }

    public static com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity controllerAtPart(Level level, BlockPos part) {
        if (level == null) return null;
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
            BlockPos pos = part.offset(x, y, z);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity ruin
                    && matches(level, pos)) return ruin;
        }
        return null;
    }

    private SpiritCompressorStructure() {}
}
