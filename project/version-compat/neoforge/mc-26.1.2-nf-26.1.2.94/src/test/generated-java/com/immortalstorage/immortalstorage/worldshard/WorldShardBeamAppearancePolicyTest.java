package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WorldShardBeamAppearancePolicyTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void beamStaysVanillaWhiteWhileOnlyTheInternalCoreUsesTheModeColor() {
        int modeColor = 0xFF12AB34;

        assertEquals(0xFFFFFFFF, WorldShardMinerAppearance.beamColor());
        assertEquals(0x12AB34, WorldShardMinerAppearance.coreColor(true, modeColor));
        assertEquals(Blocks.GLASS, WorldShardMinerAppearance.glassCover().getBlock());
    }
}
