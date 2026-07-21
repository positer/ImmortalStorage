package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SubstitutePuppetUseTest {
    @BeforeAll
    static void bootstrap() {
        Bootstrap.bootStrap();
    }

    @Test
    void debugUnbreakablePuppetCanTriggerWithoutLosingDurability() {
        ItemStack puppet = new ItemStack(Items.WOODEN_SWORD);
        puppet.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        puppet.setDamageValue(7);

        assertTrue(SubstitutePuppetItem.consumeUse(puppet));
        assertEquals(7, puppet.getDamageValue());
    }

    @Test
    void ordinaryPuppetStillConsumesOneDurability() {
        ItemStack puppet = new ItemStack(Items.WOODEN_SWORD);
        puppet.setDamageValue(7);

        assertTrue(SubstitutePuppetItem.consumeUse(puppet));
        assertEquals(8, puppet.getDamageValue());
    }
}
