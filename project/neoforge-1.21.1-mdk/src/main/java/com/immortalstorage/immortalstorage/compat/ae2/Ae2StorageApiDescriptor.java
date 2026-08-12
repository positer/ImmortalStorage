package com.immortalstorage.immortalstorage.compat.ae2;

import com.immortalstorage.core.amount.StorageAmountApiProbe;

import java.util.Objects;

/** Runtime contract for the AE2 ME storage amount surface. */
public final class Ae2StorageApiDescriptor {
    public static final String ME_STORAGE_CLASS = "appeng.api.storage.MEStorage";

    private Ae2StorageApiDescriptor() {
    }

    public static Probe probe(ClassLoader loader) {
        Objects.requireNonNull(loader, "loader");
        return new Probe(
                StorageAmountApiProbe.probe(loader, ME_STORAGE_CLASS, "insert", 4, 1),
                StorageAmountApiProbe.probe(loader, ME_STORAGE_CLASS, "extract", 4, 1));
    }

    public record Probe(
            StorageAmountApiProbe.Result insert,
            StorageAmountApiProbe.Result extract) {
        public boolean supportsLongAmounts() {
            return insert.supported() && extract.supported();
        }

        public boolean compatible() {
            return supportsLongAmounts();
        }

        public String summary() {
            return "insert=" + insert.detail() + "; extract=" + extract.detail();
        }
    }
}
