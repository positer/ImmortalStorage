package com.cultivation.cultivation.block.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceExternalInputContractTest {
    @Test
    void pullFacesUseTheOwnerLedgerWhilePushFacesUseTheConfiguredCache() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("src/main/java"))) {
            root = root.getParent();
        }
        if (root == null) throw new IllegalStateException("cannot locate project root");
        Path source = root.resolve("src/main/java/com/cultivation/cultivation/block/entity/"
                + "XianqiaoInterfaceBlockEntity.java");
        String text = Files.readString(source);
        assertTrue(text.contains("case PULL -> resolveExternalResourceStore(channel)"));
        assertTrue(text.contains("case PUSH -> inventory.hasExternalTarget(channel, side)"));
        assertTrue(text.contains("resolveExternalResourceFaceStore(ExternalResourceChannels.FE, side)"));
    }
}
