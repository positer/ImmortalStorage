package com.immortalstorage.immortalstorage.compat.beyonddimensions;

import com.immortalstorage.immortalstorage.network.storage.backend.PersonalStorageBackendContext;
import com.immortalstorage.immortalstorage.network.storage.backend.PersonalStorageBackendResolution;
import com.immortalstorage.immortalstorage.network.storage.backend.PersonalStorageBackendRouter;
import com.immortalstorage.immortalstorage.network.storage.backend.PersonalStorageBackendStatus;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;

import java.util.IdentityHashMap;
import java.util.Map;

/** Optional bootstrap for the audited Beyond Dimensions 0.7.24 API. */
public final class BeyondDimensionsCompat {
    private static final Map<UnifiedStorage, BeyondDimensionsRevisionTracker> REVISIONS =
            new IdentityHashMap<>();
    private static boolean initialized;

    public static synchronized void initialize() {
        if (initialized) return;
        PersonalStorageBackendRouter.install(BeyondDimensionsCompat::resolve);
        initialized = true;
    }

    private static PersonalStorageBackendResolution resolve(PersonalStorageBackendContext context) {
        if (context == null || context.player() == null) {
            return PersonalStorageBackendResolution.rejected(
                    "beyonddimensions", PersonalStorageBackendStatus.OWNER_UNAVAILABLE,
                    "The owner must be online before resolving Beyond Dimensions storage");
        }

        // Official 0.7.24 primary-network API. Never select an arbitrary member network.
        // https://github.com/Frostbite-time/BeyondDimensions/tree/012d9ba1b45075edf128378a61a2c2536e045d47
        DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(context.player());
        if (net == null) {
            return PersonalStorageBackendResolution.rejected(
                    "beyonddimensions", PersonalStorageBackendStatus.PRIMARY_NETWORK_MISSING,
                    "Player has no Beyond Dimensions primary network");
        }

        UnifiedStorage storage = net.getUnifiedStorage();
        BeyondDimensionsRevisionTracker revisions = trackerFor(storage);
        BeyondDimensionsItemStorage items = new BeyondDimensionsItemStorage(
                storage, revisions, context.onChanged());
        BeyondDimensionsFluidStorage fluids = context.includeFluids()
                ? new BeyondDimensionsFluidStorage(storage, revisions, context.onChanged())
                : null;
        return PersonalStorageBackendResolution.active(
                "beyonddimensions", items, fluids, "primary network " + net.getId());
    }

    private static synchronized BeyondDimensionsRevisionTracker trackerFor(UnifiedStorage storage) {
        return REVISIONS.computeIfAbsent(storage, BeyondDimensionsRevisionTracker::new);
    }

    private BeyondDimensionsCompat() {}
}
