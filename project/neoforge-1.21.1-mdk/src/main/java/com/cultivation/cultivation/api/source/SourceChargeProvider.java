package com.cultivation.cultivation.api.source;

/** Provider-owned exact reservations for one registered source currency. */
public interface SourceChargeProvider {
    boolean canReserve(SourceChargeContext context, long units);

    /** Reserve exactly {@code units}, or return {@code null} without mutation. */
    Reservation reserve(SourceChargeContext context, long units);

    interface Reservation {
        long units();

        /** Atomically charge exactly {@code chargedUnits}; may be called once. */
        boolean settle(long chargedUnits);
    }
}
