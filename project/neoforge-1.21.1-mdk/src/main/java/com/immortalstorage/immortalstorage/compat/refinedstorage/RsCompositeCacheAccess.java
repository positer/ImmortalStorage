package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;

import java.util.Set;

/**
 * Exact-RS-2.0.9 bridge implemented on {@code CompositeStorageImpl} by the
 * optional mixin. It exposes only lifecycle data needed to rebuild caches
 * after a same-owner exchange disk becomes primary or passive.
 */
public interface RsCompositeCacheAccess {
    Set<ParentComposite> immortalstorage$getParentComposites();

    void immortalstorage$rebuildCache();
}
