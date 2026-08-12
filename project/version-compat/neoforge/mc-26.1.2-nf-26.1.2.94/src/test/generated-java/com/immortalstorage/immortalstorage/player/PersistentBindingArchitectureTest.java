package com.immortalstorage.immortalstorage.player;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class PersistentBindingArchitectureTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void realmAndOwnerBoundItemsShareOnePersistentIdentityAuthority() throws Exception {
        Path root = locate("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source/com/immortalstorage/immortalstorage");
        String realm = Files.readString(root.resolve("dimension/RealmHelper.java"));
        String drive = Files.readString(root.resolve("item/custom/SpiritDriveItem.java"));
        String puppet = Files.readString(root.resolve("item/custom/SubstitutePuppetItem.java"));
        String ae2 = Files.readString(root.resolve("item/custom/XianqiaoExchangeCellItem.java"));
        String rs = Files.readString(root.resolve("item/custom/XianqiaoRsExchangeDiskItem.java"));
        String storage = Files.readString(root.resolve("network/storage/PersonalStorageNetwork.java"));

        assertTrue(realm.contains("bindPersonalRealmOnce"));
        assertTrue(realm.contains("legacyBoundItemOwner"));
        assertTrue(drive.contains("PersistentPlayerIdentity.id(player)"));
        assertTrue(puppet.contains("migrateOwner"));
        assertTrue(ae2.contains("migrateLegacyOwner"));
        assertTrue(rs.contains("migrateLegacyOwner"));
        assertTrue(storage.contains("PersistentPlayerIdentity.onlinePlayer"));
    }

    private static Path locate(String relative) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relative);
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException(relative + " not found");
    }
}
