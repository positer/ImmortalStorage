package com.immortalstorage.immortalstorage.network.storage.backend;

import org.jetbrains.annotations.Nullable;

/** Optional-mod-neutral backend selector installed by exactly one authority. */
@FunctionalInterface
public interface PersonalStorageBackendProvider {
    PersonalStorageBackendResolution resolve(@Nullable PersonalStorageBackendContext context);
}
