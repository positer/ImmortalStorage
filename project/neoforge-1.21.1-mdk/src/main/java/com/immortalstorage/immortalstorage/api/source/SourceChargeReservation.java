package com.immortalstorage.immortalstorage.api.source;

/** Exact charge held until source delivery commits or rolls back. */
public interface SourceChargeReservation {
    long reservedOutputs();

    /** Commit the batch cost for the actual delivered output. */
    boolean commit(long deliveredOutputs);

    /** Release the complete reservation without charging. */
    boolean cancel();
}
