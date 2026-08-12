package com.immortalstorage.immortalstorage.network.storage.backend;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Single-provider router for the current personal-storage authority.
 *
 * <p>With no provider installed the local ImmortalStorage backend is selected.
 * Once an optional provider is installed, every result comes from that
 * provider, including unavailable results; local fallback is intentionally
 * impossible until the next process start.</p>
 */
public final class PersonalStorageBackendRouter {
    private static final Registry GLOBAL = new Registry();

    public static void install(PersonalStorageBackendProvider provider) {
        GLOBAL.install(provider);
    }

    public static PersonalStorageBackendResolution resolve(
            @Nullable PersonalStorageBackendContext context) {
        return GLOBAL.resolve(context);
    }

    public static boolean hasExternalProvider() {
        return GLOBAL.hasProvider();
    }

    public static final class Registry {
        private @Nullable PersonalStorageBackendProvider provider;

        public synchronized void install(PersonalStorageBackendProvider provider) {
            Objects.requireNonNull(provider, "provider");
            if (this.provider != null) {
                throw new IllegalStateException("A personal-storage backend provider is already installed");
            }
            this.provider = provider;
        }

        public PersonalStorageBackendResolution resolve(
                @Nullable PersonalStorageBackendContext context) {
            PersonalStorageBackendProvider selected;
            synchronized (this) {
                selected = provider;
            }
            if (selected == null) return PersonalStorageBackendResolution.local();
            try {
                PersonalStorageBackendResolution resolution = selected.resolve(context);
                return resolution == null
                        ? PersonalStorageBackendResolution.rejected(
                                "external", PersonalStorageBackendStatus.EXTERNAL_BACKEND_ERROR,
                                "External backend provider returned no resolution")
                        : resolution;
            } catch (RuntimeException | LinkageError failure) {
                return PersonalStorageBackendResolution.rejected(
                        "external", PersonalStorageBackendStatus.EXTERNAL_BACKEND_ERROR,
                        failure.getClass().getSimpleName() + ": "
                                + Objects.toString(failure.getMessage(), "backend resolution failed"));
            }
        }

        public synchronized boolean hasProvider() {
            return provider != null;
        }
    }

    private PersonalStorageBackendRouter() {}
}
