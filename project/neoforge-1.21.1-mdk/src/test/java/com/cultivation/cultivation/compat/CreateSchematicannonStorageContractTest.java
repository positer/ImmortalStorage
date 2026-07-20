package com.cultivation.cultivation.compat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateSchematicannonStorageContractTest {
    @Test
    void xianqiaoManagerExposesTheStandardUnsidedBlockItemCapability() throws Exception {
        Path root = locateMainSourceRoot().resolve("java/com/cultivation/cultivation/block/entity");
        String registration = Files.readString(root.resolve("ModBlockEntities.java"));
        String manager = Files.readString(root.resolve("XianqiaoManagerBlockEntity.java"));

        assertTrue(registration.contains(
                "registerBlockEntity(Capabilities.ItemHandler.BLOCK, XIANQIAO_MANAGER.get()"));
        assertTrue(registration.contains("(be, side) -> be.getItemHandler()"));
        assertTrue(manager.contains("PersonalStorageNetwork.resolveInOwnerRealm"));
        assertTrue(manager.contains("public IItemHandler getItemHandler()"));
    }

    private static Path locateMainSourceRoot() {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (cursor != null) {
            Path direct = cursor.resolve("src/main");
            if (Files.isRegularFile(direct.resolve("java/com/cultivation/cultivation/CultivationMod.java"))) return direct;
            Path nested = cursor.resolve("project/neoforge-1.21.1-mdk/src/main");
            if (Files.isRegularFile(nested.resolve("java/com/cultivation/cultivation/CultivationMod.java"))) return nested;
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Unable to locate Cultivation src/main");
    }
}
