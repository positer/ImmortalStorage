package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.world.entity.EquipmentSlot;
import com.immortalstorage.immortalstorage.item.ModItems;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpiritSwordWeaponContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void spiritSwordExposesStandardMainHandWeaponAttributes() {
        Bootstrap.bootStrap();
        var sword = ModItems.SPIRIT_SWORD.get().getDefaultInstance();

        double attackDamage = sword.getAttributeModifiers().compute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 1.0D, EquipmentSlot.MAINHAND);

        assertTrue(sword.getItem() instanceof SpiritSwordItem);
        assertTrue(attackDamage > 1.0D,
                "generic weapon integrations must see the sword's standard main-hand attack attributes");
    }

    @Test
    void bothRegisteredSwordsUseTheSharedProjectionLifecycle() {
        Bootstrap.bootStrap();

        assertTrue(ModItems.SPIRIT_SWORD.get() instanceof SpiritSwordItem);
        assertTrue(ModItems.IMMORTAL_RUIN_FORGED_SPIRIT_SWORD.get()
                instanceof ImmortalRuinForgedSpiritSwordItem);
        assertTrue(ModItems.IMMORTAL_RUIN_FORGED_SPIRIT_SWORD.get() instanceof SpiritSwordItem,
                "the upgraded sword must inherit inventory refresh and hit settlement from SpiritSwordItem");
    }
}
