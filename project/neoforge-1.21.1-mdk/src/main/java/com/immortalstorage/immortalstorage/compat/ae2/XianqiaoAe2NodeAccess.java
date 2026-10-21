package com.immortalstorage.immortalstorage.compat.ae2;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Accessor the AE2-only mixin adds to {@code XianqiaoManagerBlockEntity}.
 *
 * <p>The interface is added by {@code mixin/ae2/XianqiaoManagerAe2NodeMixin} only
 * after {@code Ae2MixinConfigPlugin} has confirmed that AE2 is loaded, so the
 * unmixed block entity never touches this type. {@link XianqiaoAe2GridNodes#hostOf}
 * still checks {@code instanceof} before casting because the mixin can be absent
 * in a development runtime that runs without AE2.</p>
 */
@ApiStatus.Internal
public interface XianqiaoAe2NodeAccess {
    /** The per-block-entity AE2 node host, or {@code null} before the mixin constructor hook ran. */
    @Nullable
    XianqiaoAe2GridNodes.Host immortalstorage$ae2NodeHost();

    /** Replaces the host so it can be revived after a block-entity rebuild destroyed the node. */
    void immortalstorage$ae2SetNodeHost(@Nullable XianqiaoAe2GridNodes.Host host);
}
