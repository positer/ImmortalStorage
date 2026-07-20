package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WorldShardMinerAppearanceTest {
    @BeforeAll
    static void bootstrap() {
        Bootstrap.bootStrap();
    }

    @Test
    void coverAlwaysUsesUntintedVanillaClearGlass() {
        assertEquals(Blocks.GLASS, WorldShardMinerAppearance.glassCover().getBlock());
    }

    @Test
    void inactiveCoreIsWhiteAndBuiltinModesUseTheirBeamColors() {
        assertEquals(0xF7FBFF, WorldShardMinerAppearance.coreColor(false, 0xFF123456));

        Map<net.minecraft.resources.ResourceLocation, Integer> colors = WorldShardMinerModes.builtinModes()
                .stream().collect(Collectors.toMap(WorldShardMinerMode::id, WorldShardMinerMode::beamColor));
        assertEquals(0x55FF55, WorldShardMinerAppearance.coreColor(true,
                colors.get(WorldShardMinerModes.OVERWORLD)));
        assertEquals(0xFF5555, WorldShardMinerAppearance.coreColor(true,
                colors.get(WorldShardMinerModes.NETHER)));
        assertEquals(0xAA55FF, WorldShardMinerAppearance.coreColor(true,
                colors.get(WorldShardMinerModes.END)));
    }

    @Test
    void dataPackModeColorsRemainSupportedWithoutChangingTheGlass() {
        assertEquals(0x12AB34, WorldShardMinerAppearance.coreColor(true, 0xFF12AB34));
        assertEquals(Blocks.GLASS, WorldShardMinerAppearance.glassCover().getBlock());
    }
}
