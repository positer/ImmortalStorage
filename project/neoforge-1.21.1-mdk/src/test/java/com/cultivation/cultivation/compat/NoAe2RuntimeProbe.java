package com.cultivation.cultivation.compat;

import com.cultivation.core.resource.ResourceChannelKey;

/** Standalone class-loading probe run with the AE2 JAR removed. */
public final class NoAe2RuntimeProbe {
    public static void main(String[] args) throws Exception {
        try {
            Class.forName("appeng.api.stacks.AEKey");
            throw new AssertionError("AE2 must not be present on the runtime probe classpath");
        } catch (ClassNotFoundException expected) {
            // Required proof condition.
        }

        ResourceChannelKey key = new ResourceChannelKey("energy", "neoforge:fe");
        if (!"energy/neoforge:fe".equals(key.toString())) {
            throw new AssertionError("shared resource key failed without AE2");
        }
        Class.forName("com.cultivation.cultivation.api.storage.ExternalResourceStorage");
        Class.forName("com.cultivation.cultivation.api.storage.PersonalStorageEndpoint");
        System.out.println("Verified Cultivation shared resource API without AE2 on the runtime classpath.");
    }

    private NoAe2RuntimeProbe() {}
}
