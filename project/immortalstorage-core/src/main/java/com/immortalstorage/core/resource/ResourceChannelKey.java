package com.immortalstorage.core.resource;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable loader-neutral identity for one typed long-valued resource.
 *
 * <p>This is ImmortalStorage's authoritative shared key. It deliberately contains
 * no AE2, RS, or target-mod types; optional integrations translate to and from
 * this identity at their own class-loading boundary.</p>
 */
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
