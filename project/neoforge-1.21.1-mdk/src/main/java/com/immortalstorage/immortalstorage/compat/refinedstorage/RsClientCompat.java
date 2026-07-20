package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;

/** Client-only RS resource renderer registration. */
public final class RsClientCompat {
    private static boolean initialized;

    public static synchronized void initialize() {
        if (initialized) return;
        RefinedStorageClientApi.INSTANCE.registerResourceRendering(
                RsExternalResource.class, RsExternalResourceRendering.INSTANCE);
        initialized = true;
    }

    private RsClientCompat() {}
}
