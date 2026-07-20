package com.cultivation.cultivation.api.storage;

import com.cultivation.core.resource.ResourceChannelEntry;
import com.cultivation.core.resource.ResourceChannelKey;
import com.cultivation.core.resource.ResourceTransferAction;

import java.util.List;

/**
 * Loader-neutral long-valued view of optional resource channels in Xianqiao storage.
 *
 * <p>Optional integrations translate their native identity into a stable
 * {@link ResourceChannelKey}. The backing ledger remains shared, so AE2, RS,
 * capability adapters and the terminal never maintain competing copies. This
 * interface and its key type do not depend on AE2; addons can use them when
 * AE2 is not installed. If AE2 is present, Cultivation's optional adapter only
 * wraps the same key as an AEKey for AE2-facing calls.</p>
 */
public interface ExternalResourceStorage {
    long revision();

    List<ResourceChannelEntry> snapshot();

    long insert(ResourceChannelKey key, long amount, ResourceTransferAction action);

    long extract(ResourceChannelKey key, long amount, ResourceTransferAction action);
}
