package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.core.amount.StorageAmountApiProbe;

import java.util.Objects;

/** Runtime contract for Refined Storage's external long-valued storage API. */
public final class RefinedStorageApiDescriptor {
    public static final String INSERTABLE_STORAGE_CLASS =
            "com.refinedmods.refinedstorage.api.storage.InsertableStorage";
    public static final String EXTRACTABLE_STORAGE_CLASS =
            "com.refinedmods.refinedstorage.api.storage.ExtractableStorage";

    private RefinedStorageApiDescriptor() {
    }

    public static Probe probe(ClassLoader loader) {
        Objects.requireNonNull(loader, "loader");
        return new Probe(
                StorageAmountApiProbe.probe(loader, INSERTABLE_STORAGE_CLASS, "insert", 4, 1),
                StorageAmountApiProbe.probe(loader, EXTRACTABLE_STORAGE_CLASS, "extract", 4, 1));
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
