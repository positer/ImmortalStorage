package com.immortalstorage.immortalstorage.api.storage;

import com.immortalstorage.core.resource.ResourceChannelKey;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A standard FE capability with a stable logical identity for storage-network
 * integrations.  When {@link #canonicalOwner()} is non-null, the capability
 * is a view of that owner's canonical external FE ledger; adapters must not
 * register a second logical FE entry for the same owner and channel.
 */
public interface CanonicalEnergyStorage extends IEnergyStorage {
    ResourceChannelKey canonicalChannel();

    @Nullable UUID canonicalOwner();

    default boolean aliasesExternalLedger() {
        return canonicalOwner() != null;
    }
}
