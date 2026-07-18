package com.cultivation.cultivation.network.storage.backend;

/**
 * Server-authoritative outcome of selecting the personal-storage backend.
 *
 * <p>An installed external provider never falls back to the local store after
 * it reports an unavailable state. That invariant prevents two independent
 * authoritative balances from existing for the same player.</p>
 */
public enum PersonalStorageBackendStatus {
    LOCAL_ACTIVE(true),
    EXTERNAL_ACTIVE(true),
    PRIMARY_NETWORK_MISSING(false),
    OWNER_UNAVAILABLE(false),
    EXTERNAL_BACKEND_ERROR(false);

    private final boolean acceptsOperations;

    PersonalStorageBackendStatus(boolean acceptsOperations) {
        this.acceptsOperations = acceptsOperations;
    }

    public boolean acceptsOperations() {
        return acceptsOperations;
    }
}
