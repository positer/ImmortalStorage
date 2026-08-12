package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.nbt.CompoundTag;

/**
 * Lightweight client-visible occupancy projection.  Only one 0..7 integer
 * crosses the render-sync boundary; source identities, cache amounts, owner
 * data and the private 72-slot inventory remain server-side.
 */
public final class SourceVeinManagerDisplayState {
    public static final String TAG = "DisplayState";
    public static final int CAPACITY = 72;
    public static final int MAX_STATE = 7;
    private int state;

    public int state() { return state; }

    /** Returns true only when the eight-state occupancy indicator changed. */
    public boolean refreshFrom(SourceVeinManagerBlockEntity manager) {
        int filled = 0;
        for (int slot = 0; slot < manager.memberSlots(); slot++) {
            if (!manager.members().getStackInSlot(slot).isEmpty()) filled++;
        }
        int next = stateForFilled(filled);
        if (next == state) return false;
        state = next;
        return true;
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putInt(TAG, state);
        return tag;
    }

    public void load(CompoundTag tag) {
        state = Math.max(0, Math.min(MAX_STATE, tag.getIntOr(TAG, 0)));
    }

    /** state=ceil(filled*7/72), with zero kept as its own all-black state. */
    public static int stateForFilled(int filled) {
        if (filled <= 0) return 0;
        int clamped = Math.min(CAPACITY, filled);
        return Math.min(MAX_STATE, (clamped * MAX_STATE + CAPACITY - 1) / CAPACITY);
    }
}
