package com.cultivation.cultivation.worldshard;

/** Basin-only operating state; no value is stored on or shared with the miner. */
public enum TreasureBasinStatus {
    INACTIVE,
    ACTIVE,
    CALIBRATING,
    CACHE_FULL,
    STORAGE_UNAVAILABLE;

    public static TreasureBasinStatus resolve(boolean attachedActive,
                                              boolean selectableLoot,
                                              boolean cacheFull,
                                              boolean storageUnavailable) {
        if (!attachedActive) return INACTIVE;
        if (!selectableLoot) return CALIBRATING;
        if (storageUnavailable) return STORAGE_UNAVAILABLE;
        if (cacheFull) return CACHE_FULL;
        return ACTIVE;
    }

    public boolean blocksGeneration() {
        return this != ACTIVE;
    }

    public static TreasureBasinStatus fromNetwork(int ordinal) {
        TreasureBasinStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : INACTIVE;
    }
}
