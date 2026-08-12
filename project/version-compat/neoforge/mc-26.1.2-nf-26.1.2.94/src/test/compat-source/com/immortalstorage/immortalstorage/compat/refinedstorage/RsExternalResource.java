package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.core.resource.ResourceChannelKey;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceTag;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceType;

import java.util.List;
import java.util.Objects;

/** RS-native identity backed directly by ImmortalStorage's loader-neutral key. */
public final class RsExternalResource implements PlatformResourceKey {
    private final ResourceChannelKey resource;

    public RsExternalResource(ResourceChannelKey resource) {
        this.resource = Objects.requireNonNull(resource, "resource");
    }

    public ResourceChannelKey resource() {
        return resource;
    }

    @Override public long getInterfaceExportLimit() { return Long.MAX_VALUE; }
    @Override public long getProcessingPatternLimit() { return Long.MAX_VALUE; }
    @Override public List<ResourceTag> getTags() { return List.of(); }
    @Override public ResourceType getResourceType() { return RsExternalResourceType.INSTANCE; }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof RsExternalResource that
                && resource.equals(that.resource);
    }

    @Override public int hashCode() { return resource.hashCode(); }
    @Override public String toString() { return "RsExternalResource[" + resource + "]"; }
}
