package com.cultivation.cultivation.network.storage.backend;

import com.cultivation.cultivation.api.storage.terminal.TerminalFluidStorage;
import com.cultivation.cultivation.api.storage.terminal.TerminalItemStorage;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/** Immutable result of resolving the one authoritative personal-storage backend. */
public record PersonalStorageBackendResolution(
        String backendId,
        PersonalStorageBackendStatus status,
        @Nullable TerminalItemStorage itemStorage,
        @Nullable TerminalFluidStorage fluidStorage,
        String diagnostic) {

    public PersonalStorageBackendResolution {
        backendId = requireBackendId(backendId);
        Objects.requireNonNull(status, "status");
        diagnostic = diagnostic == null ? "" : diagnostic;
        if (!status.acceptsOperations() && (itemStorage != null || fluidStorage != null)) {
            throw new IllegalArgumentException("Rejected backends cannot expose writable storage");
        }
        if (status == PersonalStorageBackendStatus.EXTERNAL_ACTIVE
                && itemStorage == null && fluidStorage == null) {
            throw new IllegalArgumentException("An active external backend requires a storage surface");
        }
    }

    public static PersonalStorageBackendResolution local() {
        return new PersonalStorageBackendResolution(
                "cultivation", PersonalStorageBackendStatus.LOCAL_ACTIVE,
                null, null, "Cultivation personal storage is authoritative");
    }

    public static PersonalStorageBackendResolution active(
            String backendId,
            @Nullable TerminalItemStorage itemStorage,
            @Nullable TerminalFluidStorage fluidStorage,
            String diagnostic) {
        return new PersonalStorageBackendResolution(
                backendId, PersonalStorageBackendStatus.EXTERNAL_ACTIVE,
                itemStorage, fluidStorage, diagnostic);
    }

    public static PersonalStorageBackendResolution rejected(
            String backendId,
            PersonalStorageBackendStatus status,
            String diagnostic) {
        if (status == null || status.acceptsOperations()) {
            throw new IllegalArgumentException("A rejected backend requires an unavailable status");
        }
        return new PersonalStorageBackendResolution(backendId, status, null, null, diagnostic);
    }

    public boolean usesLocalStorage() {
        return status == PersonalStorageBackendStatus.LOCAL_ACTIVE;
    }

    public boolean acceptsOperations() {
        return status.acceptsOperations();
    }

    private static String requireBackendId(String backendId) {
        if (backendId == null || backendId.isBlank()) {
            throw new IllegalArgumentException("backendId must not be blank");
        }
        return backendId;
    }
}
