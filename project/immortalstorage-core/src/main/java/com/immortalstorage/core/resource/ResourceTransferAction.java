package com.immortalstorage.core.resource;

/** Loader-neutral simulation/execution flag for cross-mod resource transactions. */
public enum ResourceTransferAction {
    SIMULATE,
    EXECUTE;

    public boolean executes() {
        return this == EXECUTE;
    }
}
