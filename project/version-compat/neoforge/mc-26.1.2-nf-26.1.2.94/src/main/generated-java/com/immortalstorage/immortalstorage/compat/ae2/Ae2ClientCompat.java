package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.client.api.AEKeyRendering;

/**
 * Client-only AE2 registration entry point.
 *
 * <p>Mirrors the refined-storage client bootstrap: invoked reflectively only
 * when AE2 is loaded, and only from the physical client setup. AE2 terminal
 * screens require a registered render handler for every visible channel, so
 * this must run before any ME screen can open.</p>
 */
public final class Ae2ClientCompat {
    private static boolean initialized;

    public static synchronized void initialize() {
        if (initialized) return;
        AEKeyRendering.register(
                ImmortalStorageExternalResourceKeyType.TYPE,
                ImmortalStorageExternalResourceKey.class,
                ImmortalStorageExternalResourceKeyRenderHandler.INSTANCE);
        initialized = true;
    }

    private Ae2ClientCompat() {}
}
