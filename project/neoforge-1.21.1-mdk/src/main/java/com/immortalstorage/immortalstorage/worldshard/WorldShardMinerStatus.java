package com.immortalstorage.immortalstorage.worldshard;

/** Common status vocabulary; each machine owns and synchronizes its own state. */
public enum WorldShardMinerStatus {
    INACTIVE,
    ACTIVE,
    CACHE_FULL,
    STORAGE_UNAVAILABLE;

    public static WorldShardMinerStatus resolve(boolean active, boolean cacheFull,
                                                boolean storageUnavailable) {
        if (!active) return INACTIVE;
        if (storageUnavailable) return STORAGE_UNAVAILABLE;
        if (cacheFull) return CACHE_FULL;
        return ACTIVE;
    }

    public boolean blocksGeneration() {
        return this == CACHE_FULL || this == STORAGE_UNAVAILABLE;
    }

    public boolean blocksOutput() {
        return blocksGeneration();
    }
}
