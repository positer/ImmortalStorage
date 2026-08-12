package com.immortalstorage.immortalstorage.compat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateSchematicannonStorageContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void xianqiaoManagerExposesTheStandardUnsidedBlockItemCapability() throws Exception {
        Path root = locateMainSourceRoot().getParent().getParent().resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage", "block", "entity")).normalize();
        String registration = Files.readString(root.resolve("ModBlockEntities.java"));
        String manager = Files.readString(root.resolve("XianqiaoManagerBlockEntity.java"));

        assertTrue(registration.contains(
                "registerBlockEntity(Capabilities.Item.BLOCK, XIANQIAO_MANAGER.get()"));
        assertTrue(registration.contains("CompatTransfer.item(be.getItemHandler())"));
        assertTrue(manager.contains("PersonalStorageNetwork.resolveInOwnerRealm"));
        assertTrue(manager.contains("public IItemHandler getItemHandler()"));
    }

    private static Path locateMainSourceRoot() {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (cursor != null) {
            Path direct = cursor.resolve("src/main");
            if (Files.isRegularFile(direct.resolve("java/com/immortalstorage/immortalstorage/ImmortalStorageMod.java"))) return direct;
            Path nested = cursor.resolve("project/neoforge-1.21.1-mdk/src/main");
            if (Files.isRegularFile(nested.resolve("java/com/immortalstorage/immortalstorage/ImmortalStorageMod.java"))) return nested;
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Unable to locate ImmortalStorage src/main");
    }
}
