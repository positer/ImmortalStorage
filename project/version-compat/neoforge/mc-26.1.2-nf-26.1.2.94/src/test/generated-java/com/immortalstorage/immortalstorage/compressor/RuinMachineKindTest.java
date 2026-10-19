package com.immortalstorage.immortalstorage.compressor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RuinMachineKindTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test void onlyTheTopCentreCellSeparatesTheTwoMachines() {
        assertEquals("immortalstorage:treasure_basin", SpiritCompressorStructure.blockId(0, 1, 0, RuinMachineKind.SPIRIT));
        assertEquals("minecraft:enchanting_table", SpiritCompressorStructure.blockId(0, 1, 0, RuinMachineKind.ENCHANTMENT));
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
            if (x == 0 && y == 1 && z == 0) continue;
            assertEquals(SpiritCompressorStructure.blockId(x, y, z), SpiritCompressorStructure.blockId(x, y, z, RuinMachineKind.SPIRIT));
            assertEquals(SpiritCompressorStructure.blockId(x, y, z), SpiritCompressorStructure.blockId(x, y, z, RuinMachineKind.ENCHANTMENT));
        }
    }

    @Test void theSpiritShellDefinitionIsStillTheTreasureBasinFixture() {
        // The legacy three-argument lookup is what the 0.1.6 fixture and its chain particles are built on.
        assertEquals("immortalstorage:treasure_basin", SpiritCompressorStructure.blockId(0, 1, 0));
        assertEquals("immortalstorage:world_shard_miner", SpiritCompressorStructure.blockId(0, -1, 0));
        assertEquals("minecraft:chain", SpiritCompressorStructure.blockId(1, 0, 1));
        assertEquals("minecraft:air", SpiritCompressorStructure.blockId(1, 0, 0));
        assertEquals("immortalstorage:world_barrier", SpiritCompressorStructure.blockId(1, 1, 1));
        assertEquals("immortalstorage:miniature_immortal_ruin_block", SpiritCompressorStructure.blockId(0, 0, 0));
    }

    @Test void theKindIsReadFromTheBlockAboveTheCore() {
        assertEquals(RuinMachineKind.SPIRIT, RuinMachineKind.of("immortalstorage:treasure_basin"));
        assertEquals(RuinMachineKind.ENCHANTMENT, RuinMachineKind.of("minecraft:enchanting_table"));
        assertNull(RuinMachineKind.of("minecraft:air"));
        assertNull(RuinMachineKind.of("immersiveengineering:rs_engineering"));
        assertNull(RuinMachineKind.of(null));
    }

    @Test void titlesFollowKindAndPolarity() {
        assertEquals("container.immortalstorage.spirit_compressor", RuinMachineKind.SPIRIT.titleKey(false));
        assertEquals("container.immortalstorage.spirit_decompressor", RuinMachineKind.SPIRIT.titleKey(true));
        assertEquals("container.immortalstorage.enchantment_compressor", RuinMachineKind.ENCHANTMENT.titleKey(false));
        assertEquals("container.immortalstorage.enchantment_decompressor", RuinMachineKind.ENCHANTMENT.titleKey(true));
    }

    @Test void particleColoursKeepTheSpiritFloatsAndUseTheExperienceRuneGreenForEnchantment() {
        var spirit = RuinMachineKind.SPIRIT.particleColour();
        assertEquals(.76F, spirit.red());
        assertEquals(.97F, spirit.green());
        assertEquals(1F, spirit.blue());
        assertEquals(0xC2F7FF, RuinMachineKind.SPIRIT.particleFadeColour());
        var enchantment = RuinMachineKind.ENCHANTMENT.particleColour();
        assertEquals(0x80 / 255F, enchantment.red());
        assertEquals(1F, enchantment.green());
        assertEquals(0x20 / 255F, enchantment.blue());
        assertEquals(0xE8FFB0, RuinMachineKind.ENCHANTMENT.particleFadeColour());
    }
}
