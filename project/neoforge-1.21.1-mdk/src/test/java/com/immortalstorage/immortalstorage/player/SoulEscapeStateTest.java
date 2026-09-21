package com.immortalstorage.immortalstorage.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SoulEscapeStateTest {
    @Test
    void stageEightUnlocksTheSessionWithoutAnyPermissionInput() {
        assertFalse(SoulEscapeState.canStart(0, false, false));
        assertFalse(SoulEscapeState.canStart(7, false, false));
        assertTrue(SoulEscapeState.canStart(SoulEscapeState.REQUIRED_STAGE, false, false));
        assertTrue(SoulEscapeState.canStart(10, false, false));
    }

    @Test
    void runningAndForeignSpectatorStatesAreNeverReinterpreted() {
        assertFalse(SoulEscapeState.canStart(10, true, true),
                "an active session belongs to the exit path");
        assertFalse(SoulEscapeState.canStart(10, false, true),
                "spectator mode chosen elsewhere is not a Soul Escape session");
        assertFalse(SoulEscapeState.canStart(10, true, false));
    }

    @Test
    void leavingRestoresTheExactPreviousModeAndClearsTheSession() {
        CompoundTag persistent = new CompoundTag();
        SoulEscapeState.start(persistent, GameType.ADVENTURE.getId());

        assertTrue(SoulEscapeState.isActive(persistent));
        assertEquals(GameType.ADVENTURE, SoulEscapeState.finish(persistent));
        assertFalse(SoulEscapeState.isActive(persistent));
    }

    @Test
    void everyNonSpectatorModeRoundTripsAndSpectatorNeverDoes() {
        for (GameType type : GameType.values()) {
            if (type == GameType.SPECTATOR) continue;
            assertEquals(type, SoulEscapeState.restoreTarget(type.getId()));
        }
        assertEquals(GameType.SURVIVAL, SoulEscapeState.restoreTarget(GameType.SPECTATOR.getId()));
        assertEquals(GameType.SURVIVAL, SoulEscapeState.restoreTarget(99));
        assertEquals(GameType.SURVIVAL, SoulEscapeState.restoreTarget(-1));
    }

    @Test
    void aReconnectedSessionStillLeavesCleanly() {
        CompoundTag persistent = new CompoundTag();
        SoulEscapeState.start(persistent, GameType.CREATIVE.getId());

        CompoundTag reloaded = persistent.copy();
        assertTrue(SoulEscapeState.isActive(reloaded));
        assertEquals(GameType.CREATIVE, SoulEscapeState.finish(reloaded));
        assertFalse(SoulEscapeState.isActive(reloaded));
    }
}
