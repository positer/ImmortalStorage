package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression contract for the placed miniature ruin's living-entity effect boundary. */
final class MiniatureImmortalRuinEffectPolicyTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void effectAreaIsExactlyThirteenByOneByThirteenAroundTheRuinLayer() {
        AABB area = MiniatureImmortalRuinEffectPolicy.effectArea(new BlockPos(10, 64, -3));

        assertEquals(4.0D, area.minX);
        assertEquals(64.0D, area.minY);
        assertEquals(-9.0D, area.minZ);
        assertEquals(17.0D, area.maxX);
        assertEquals(65.0D, area.maxY);
        assertEquals(4.0D, area.maxZ);
        assertEquals(13.0D, area.maxX - area.minX);
        assertEquals(1.0D, area.maxY - area.minY);
        assertEquals(13.0D, area.maxZ - area.minZ);
    }

    @Test
    void holdingMiniatureRuinExcludesPlayersEvenWhenPlayerEffectsAreEnabled() {
        assertFalse(MiniatureImmortalRuinEffectPolicy.shouldAffectLivingEntity(true, true, true));
        assertFalse(MiniatureImmortalRuinEffectPolicy.shouldAffectLivingEntity(true, false, true));
    }

    @Test
    void ordinaryPlayerAndNonPlayerFilteringRemainsConfigurable() {
        assertTrue(MiniatureImmortalRuinEffectPolicy.shouldAffectLivingEntity(true, true, false));
        assertFalse(MiniatureImmortalRuinEffectPolicy.shouldAffectLivingEntity(true, false, false));
        assertTrue(MiniatureImmortalRuinEffectPolicy.shouldAffectLivingEntity(false, false, false));
        assertTrue(MiniatureImmortalRuinEffectPolicy.shouldAffectLivingEntity(false, false, true));
    }
}
