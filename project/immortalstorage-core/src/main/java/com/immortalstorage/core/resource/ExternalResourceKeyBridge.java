package com.immortalstorage.core.resource;

/**
 * Loader-neutral identity bridge for an optional resource system.
 *
 * <p>An integration implements this contract at its class-loading boundary:
 * the native key type stays in the integration module, while the shared
 * ledger, terminal and storage-network adapters use only
 * {@link ResourceChannelKey}. Returning {@code null} means that the bridge
 * does not own the supplied identity. Priorities are used when several
 * integrations expose the same logical resource and the highest-priority
 * native key should be emitted while lower-priority legacy keys remain
 * readable.</p>
 *
 * @param <N> native key type owned by the optional integration
 */
public interface ExternalResourceKeyBridge<N> {
    /** Higher values win when more than one bridge can emit the same key. */
    default int priority() {
        return 0;
    }

    /** Maps a native identity into the authoritative shared identity. */
    ResourceChannelKey toResourceKey(N nativeKey);

    /** Maps the shared identity into this bridge's native identity, if owned. */
    N toNativeKey(ResourceChannelKey key);
}
