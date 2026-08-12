package com.immortalstorage.immortalstorage.compat;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Target-only JUnit bootstrap for migrated tests that construct vanilla
 * ItemStacks without having a mod lifecycle event available.
 */
public final class CompatTestBootstrapExtension implements BeforeAllCallback {
    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (BOOTSTRAPPED.compareAndSet(false, true)) {
            CompatTestBootstrap.bootstrap();
        }
    }
}
