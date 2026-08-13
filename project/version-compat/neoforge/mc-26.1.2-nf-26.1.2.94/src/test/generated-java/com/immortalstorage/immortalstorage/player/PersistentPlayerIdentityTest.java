package com.immortalstorage.immortalstorage.player;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class PersistentPlayerIdentityTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    @Test
    void stableOwnerIdSurvivesSaveReloadAndCannotBeRebound() {
        UUID legacyOwner = UUID.randomUUID();
        ImmortalStoragePlayerData original = new ImmortalStoragePlayerData();
        assertEquals(legacyOwner, original.bindPersonalRealmOnce(legacyOwner));

        CompoundTag saved = original.serializeNBT(REGISTRIES);
        assertEquals(legacyOwner, com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.getUuid(saved, "personalRealmId"));

        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, saved);
        assertEquals(legacyOwner, restored.getPersonalRealmId());
        assertEquals(legacyOwner, restored.bindPersonalRealmOnce(UUID.randomUUID()));
    }

    @Test
    void pre009SaveRemainsUnboundUntilTheOneShotMigrationChoosesAnOwner() {
        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, new CompoundTag());
        assertNull(restored.getPersonalRealmId());

        UUID migrated = UUID.randomUUID();
        assertEquals(migrated, restored.bindPersonalRealmOnce(migrated));
        assertEquals(migrated, restored.getPersonalRealmId());
    }
}
