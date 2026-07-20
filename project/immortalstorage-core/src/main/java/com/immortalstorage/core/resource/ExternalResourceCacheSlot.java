package com.immortalstorage.core.resource;

import java.util.Objects;

/** Loader-neutral authoritative state for one of the nine mixed cache slots. */
public record ExternalResourceCacheSlot(
        ResourceChannelKey key,
        long targetAmount,
        long cachedAmount,
        ResourceFaceMask outputFaces) {
    public ExternalResourceCacheSlot {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(outputFaces, "outputFaces");
        if (targetAmount < 0L) throw new IllegalArgumentException("targetAmount must be non-negative");
        if (cachedAmount < 0L || cachedAmount > targetAmount) {
            throw new IllegalArgumentException("cachedAmount must be between zero and targetAmount");
        }
    }

    public boolean canPush(ResourceFace face, ResourceFaceMode mode) {
        return mode == ResourceFaceMode.PUSH && outputFaces.includes(face) && cachedAmount > 0L;
    }
}
