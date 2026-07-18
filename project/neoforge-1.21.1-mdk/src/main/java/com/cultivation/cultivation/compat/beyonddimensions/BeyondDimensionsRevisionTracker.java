package com.cultivation.cultivation.compat.beyonddimensions;

import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;

import java.util.concurrent.atomic.AtomicLong;

/** Shared revision clock driven by the official UnifiedStorage change event. */
final class BeyondDimensionsRevisionTracker {
    private final AtomicLong revision = new AtomicLong();

    BeyondDimensionsRevisionTracker(UnifiedStorage storage) {
        // Official 0.7.24 source:
        // https://github.com/Frostbite-time/BeyondDimensions/tree/012d9ba1b45075edf128378a61a2c2536e045d47
        storage.subscribeAny(this, this::advance);
    }

    long revision() {
        return revision.get();
    }

    private void advance() {
        revision.updateAndGet(value -> value == Long.MAX_VALUE ? 0L : value + 1L);
    }
}
