package com.immortalstorage.immortalstorage.entity;

import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

final class PrimordialQiConversionRegistryTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test void resolvesIronGolemEggFromTheGlobalItemRegistry() {
        assertSame(Items.IRON_GOLEM_SPAWN_EGG,
                PrimordialQiConversion.findRegisteredSpawnEgg(EntityType.IRON_GOLEM));
    }

    @Test void resolvesEnderDragonEggFromTheGlobalItemRegistry() {
        assertSame(Items.ENDER_DRAGON_SPAWN_EGG,
                PrimordialQiConversion.findRegisteredSpawnEgg(EntityType.ENDER_DRAGON));
    }
}
