package com.immortalstorage.immortalstorage.network.storage.backend;

import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PersonalStorageBackendRouterTest {
    @Test
    void noExternalProviderSelectsTheImmortalStorageBackend() {
        PersonalStorageBackendRouter.Registry registry = new PersonalStorageBackendRouter.Registry();

        PersonalStorageBackendResolution resolution = registry.resolve(null);

        assertEquals(PersonalStorageBackendStatus.LOCAL_ACTIVE, resolution.status());
        assertEquals("immortalstorage", resolution.backendId());
        assertTrue(resolution.usesLocalStorage());
    }

    @Test
    void supportedExternalBackendIsTheOnlyAuthority() {
        PersonalStorageBackendRouter.Registry registry = new PersonalStorageBackendRouter.Registry();
        TerminalItemStorage external = new EmptyItemStorage();
        registry.install(context -> PersonalStorageBackendResolution.active(
                "beyonddimensions", external, null, "primary network 17"));

        PersonalStorageBackendResolution resolution = registry.resolve(null);

        assertEquals(PersonalStorageBackendStatus.EXTERNAL_ACTIVE, resolution.status());
        assertFalse(resolution.usesLocalStorage());
        assertSame(external, resolution.itemStorage());
    }

    @Test
    void missingPrimaryNetworkRejectsInsteadOfFallingBackToLocalStorage() {
        PersonalStorageBackendRouter.Registry registry = new PersonalStorageBackendRouter.Registry();
        registry.install(context -> PersonalStorageBackendResolution.rejected(
                "beyonddimensions",
                PersonalStorageBackendStatus.PRIMARY_NETWORK_MISSING,
                "player has no Beyond Dimensions primary network"));

        PersonalStorageBackendResolution resolution = registry.resolve(null);

        assertEquals(PersonalStorageBackendStatus.PRIMARY_NETWORK_MISSING, resolution.status());
        assertFalse(resolution.usesLocalStorage());
        assertFalse(resolution.acceptsOperations());
        assertTrue(resolution.diagnostic().contains("primary network"));
    }

    @Test
    void aSecondDifferentAuthorityCannotReplaceTheInstalledProvider() {
        PersonalStorageBackendRouter.Registry registry = new PersonalStorageBackendRouter.Registry();
        registry.install(context -> PersonalStorageBackendResolution.rejected(
                "beyonddimensions", PersonalStorageBackendStatus.PRIMARY_NETWORK_MISSING, "missing"));

        IllegalStateException failure = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> registry.install(context -> PersonalStorageBackendResolution.local()));

        assertTrue(failure.getMessage().contains("already installed"));
    }

    private static final class EmptyItemStorage implements TerminalItemStorage {
        @Override public long revision() { return 0L; }
        @Override public List<StorageItemSummary> snapshot() { return List.of(); }
        @Override public long insert(TerminalEntryKey key, long amount, TerminalStorageAction action) { return 0L; }
        @Override public long extract(TerminalEntryKey key, long amount, TerminalStorageAction action) { return 0L; }
    }
}
