package com.immortalstorage.immortalstorage.api.storage;

import com.immortalstorage.immortalstorage.network.storage.PersonalStorageNetwork;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceTransferAction;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ExternalResourceStorageEndpointTest {
    @Test
    void endpointUsesTheAuthoritativeStageGatedLedger() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(8);
        PersonalStorageNetwork.Endpoint endpoint = new PersonalStorageNetwork.Endpoint(
                UUID.fromString("00000000-0000-0000-0000-000000000811"),
                data,
                null,
                null);

        ExternalResourceStorage storage = endpoint.externalResourceStorage();
        assertNotNull(storage);
        assertEquals(4_096L, storage.insert(
                ExternalResourceChannels.FE, 4_096L, ResourceTransferAction.EXECUTE));
        assertEquals(4_096L, data.getExternalResourceAmount(ExternalResourceChannels.FE));
        assertEquals(4_096L, storage.snapshot().getFirst().amount());

        long revision = storage.revision();
        assertEquals(128L, storage.extract(
                ExternalResourceChannels.FE, 128L, ResourceTransferAction.SIMULATE));
        assertEquals(revision, storage.revision(), "simulation must not dirty the shared ledger");
        assertEquals(128L, storage.extract(
                ExternalResourceChannels.FE, 128L, ResourceTransferAction.EXECUTE));
        assertEquals(3_968L, data.getExternalResourceAmount(ExternalResourceChannels.FE));
    }
}
