package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;
import com.refinedmods.refinedstorage.api.network.storage.StorageProvider;
import com.refinedmods.refinedstorage.api.storage.Storage;

import java.util.Objects;

/**
 * One RS 2.0.9 storage-bearing network node owned by the Xianqiao manager.
 *
 * <p>VERIFIED against refinedstorage-neoforge-2.0.9.jar with javap:</p>
 * <ul>
 *   <li>{@code com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode(long energyUsage)}
 *       — public class, public constructor.</li>
 *   <li>{@code com.refinedmods.refinedstorage.api.network.storage.StorageProvider#getStorage()}
 *       — the only member.</li>
 *   <li>{@code StorageNetworkComponentImpl.onContainerAdded(NetworkNodeContainer)} adds
 *       {@code ((StorageProvider) container.getNode()).getStorage()} to the network root storage
 *       the moment the container joins; activeness is irrelevant for that registration.
 *       Source: javap -p -c of StorageNetworkComponentImpl.</li>
 * </ul>
 *
 * <p>ASSUMPTION (unverified at runtime): because RS keeps the exact object returned by
 * {@code getStorage()} for the lifetime of the container, this node hands over one stable
 * instance and {@link #setStorage(Storage)} is intentionally not provided.</p>
 */
final class RsManagerStorageNode extends SimpleNetworkNode implements StorageProvider {
    private final Storage storage;
    private final String name;

    RsManagerStorageNode(String name, Storage storage) {
        super(0L);
        this.name = Objects.requireNonNull(name, "name");
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override
    public Storage getStorage() {
        return storage;
    }

    /** Debug/status label only; RS derives its own names from the container. */
    String nodeName() {
        return name;
    }

    /**
     * Always affordable: this node advertises no network energy usage, so {@code doWork()}
     * never has to extract anything. RS's own block entities drive activeness themselves,
     * which this bridge does from its server tick.
     */
    @Override
    public long getEnergyUsage() {
        return 0L;
    }
}
