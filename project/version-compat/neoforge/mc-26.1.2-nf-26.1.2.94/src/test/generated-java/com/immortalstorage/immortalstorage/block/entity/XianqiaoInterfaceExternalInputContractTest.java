package com.immortalstorage.immortalstorage.block.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceExternalInputContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void pullFacesUseTheOwnerLedgerWhilePushFacesUseTheConfiguredCache() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))) {
            root = root.getParent();
        }
        if (root == null) throw new IllegalStateException("cannot locate project root");
        Path source = root.resolve("src/main/java/com/immortalstorage/immortalstorage/block/entity/"
                + "XianqiaoInterfaceBlockEntity.java");
        String text = Files.readString(source);
        assertTrue(text.contains("case PULL -> resolveExternalResourceStore(channel)"));
        assertTrue(text.contains("case PUSH -> inventory.hasExternalTarget(channel, side)"));
        assertTrue(text.contains("resolveExternalResourceFaceStore(ExternalResourceChannels.FE, side)"));
    }
}
