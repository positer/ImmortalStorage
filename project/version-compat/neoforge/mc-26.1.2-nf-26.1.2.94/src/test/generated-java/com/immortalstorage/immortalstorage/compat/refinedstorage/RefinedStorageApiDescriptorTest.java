package com.immortalstorage.immortalstorage.compat.refinedstorage;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RefinedStorageApiDescriptorTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void resolvedRefinedStorageContractIsLongValued() {
        RefinedStorageApiDescriptor.Probe probe =
                RefinedStorageApiDescriptor.probe(RefinedStorageApiDescriptor.class.getClassLoader());
        assertTrue(probe.compatible(), probe::summary);
        assertTrue(probe.supportsLongAmounts());
    }

    @Test
    void absentRefinedStorageContractDoesNotAccidentallyClaimLongSupport() throws Exception {
        try (URLClassLoader empty = new URLClassLoader(new URL[0], null)) {
            RefinedStorageApiDescriptor.Probe probe = RefinedStorageApiDescriptor.probe(empty);
            assertFalse(probe.compatible());
            assertFalse(probe.supportsLongAmounts());
        }
    }
}
