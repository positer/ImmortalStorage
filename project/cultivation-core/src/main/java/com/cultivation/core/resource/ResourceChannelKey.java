package com.cultivation.core.resource;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable loader-neutral identity for one typed long-valued resource. */
public record ResourceChannelKey(String channel, String resourceId) {
    private static final Pattern CHANNEL = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern RESOURCE_LOCATION =
            Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    public ResourceChannelKey {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(resourceId, "resourceId");
        if (!CHANNEL.matcher(channel).matches()) {
            throw new IllegalArgumentException("invalid resource channel: " + channel);
        }
        if (!RESOURCE_LOCATION.matcher(resourceId).matches()) {
            throw new IllegalArgumentException("invalid resource id: " + resourceId);
        }
    }

    @Override
    public String toString() {
        return channel + "/" + resourceId;
    }
}
