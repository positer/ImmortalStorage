package com.immortalstorage.immortalstorage.compat.ae2;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Ae2StorageApiDescriptorTest {
    @Test
    void resolvedAe2ContractIsLongValued() {
        Ae2StorageApiDescriptor.Probe probe =
                Ae2StorageApiDescriptor.probe(Ae2StorageApiDescriptor.class.getClassLoader());
        assertTrue(probe.compatible(), probe::summary);
        assertTrue(probe.supportsLongAmounts());
    }

    @Test
    void absentAe2ContractDoesNotAccidentallyClaimLongSupport() throws Exception {
        try (URLClassLoader empty = new URLClassLoader(new URL[0], null)) {
            Ae2StorageApiDescriptor.Probe probe = Ae2StorageApiDescriptor.probe(empty);
            assertFalse(probe.compatible());
            assertFalse(probe.supportsLongAmounts());
        }
    }
}
