package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class RsExternalGridResourceMapperTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void registeredMapperAllowsRsGridToOpenWithFallbackExternalResource() {
        RsCompat.initialize();
        var key = new RsExternalResource(ExternalResourceChannels.FE);

        var mapped = assertDoesNotThrow(() ->
                RefinedStorageApi.INSTANCE.getGridResourceRepositoryMapper().apply(key));

        assertInstanceOf(RsExternalGridResource.class, mapped);
    }
}
