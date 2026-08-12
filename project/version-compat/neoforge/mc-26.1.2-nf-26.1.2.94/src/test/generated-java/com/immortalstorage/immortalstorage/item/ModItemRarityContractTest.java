package com.immortalstorage.immortalstorage.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.item.Rarity;
import org.junit.jupiter.api.Test;

class ModItemRarityContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void rarityTracksRecipeComplexityAndIngredientScarcity() {
        assertEquals(Rarity.COMMON, ModItems.rarityFor("spirit_iron"));
        assertEquals(Rarity.UNCOMMON, ModItems.rarityFor("spirit_crystal"));
        assertEquals(Rarity.UNCOMMON, ModItems.rarityFor("spirit_core"));
        assertEquals(Rarity.UNCOMMON, ModItems.rarityFor("simulated_spirit_field"));
        assertEquals(Rarity.RARE, ModItems.rarityFor("substitute_puppet"));
        assertEquals(Rarity.RARE, ModItems.rarityFor("xianqiao_manager"));
        assertEquals(Rarity.RARE, ModItems.rarityFor("ancient_debris_vein"));
        assertEquals(Rarity.EPIC, ModItems.rarityFor("world_shard_miner"));
        assertEquals(Rarity.EPIC, ModItems.rarityFor("dragon_egg_vein"));
        assertEquals(Rarity.EPIC, ModItems.rarityFor("immortal_ruin_forged_spirit_sword"));
    }
}
