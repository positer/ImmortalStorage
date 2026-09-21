package com.immortalstorage.immortalstorage.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;

/**
 * Server-authoritative Soul Escape session for stage-8+ players.
 *
 * <p>The toggle is deliberately independent of operator permissions, the
 * world's cheat flag and the hardcore flag: the client only reports the key
 * press, and this state machine alone decides whether the session starts and
 * which game mode it restores. A session always returns the exact mode the
 * player had before it started, so a creative or adventure player is never
 * dropped into survival by leaving Soul Escape.
 */
public final class SoulEscapeState {
    /** Stage that unlocks Soul Escape. */
    public static final int REQUIRED_STAGE = 8;

    private static final String ACTIVE = "ImmortalStorageSoulEscapeActive";
    private static final String PREVIOUS_GAME_TYPE = "ImmortalStorageSoulEscapePreviousGameType";

    public static boolean isActive(CompoundTag persistentData) {
        return persistentData.getBooleanOr(ACTIVE, false);
    }

    /**
     * A session starts once the stage gate is met. An already running session
     * is left to the exit path, and a spectator state produced by another
     * command or game rule is never re-interpreted as a Soul Escape session.
     */
    public static boolean canStart(int stage, boolean active, boolean alreadySpectator) {
        return !active && !alreadySpectator && stage >= REQUIRED_STAGE;
    }

    public static void start(CompoundTag persistentData, int previousGameTypeId) {
        persistentData.putInt(PREVIOUS_GAME_TYPE, previousGameTypeId);
        persistentData.putBoolean(ACTIVE, true);
    }

    /** Ends the session and reports the game mode that must be restored. */
    public static GameType finish(CompoundTag persistentData) {
        GameType restored = restoreTarget(persistentData.getIntOr(PREVIOUS_GAME_TYPE, 0));
        persistentData.remove(ACTIVE);
        persistentData.remove(PREVIOUS_GAME_TYPE);
        return restored;
    }

    /**
     * Maps a stored game-mode id back to a restorable mode. Spectator is never
     * a restore target, so a damaged session can never trap a player in
     * spectator mode.
     */
    public static GameType restoreTarget(int gameTypeId) {
        GameType stored = GameType.byNullableId(gameTypeId);
        return stored == null || stored == GameType.SPECTATOR ? GameType.SURVIVAL : stored;
    }

    private SoulEscapeState() {}
}
