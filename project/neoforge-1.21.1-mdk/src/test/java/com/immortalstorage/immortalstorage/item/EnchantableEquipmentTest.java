package com.immortalstorage.immortalstorage.item;

import com.immortalstorage.immortalstorage.compressor.EnchantmentStackRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The immortal equipment is meant to be an ordinary enchantable item, not a compressor-private whitelist: the
 * compressor, the anvil and other mods all judge it by isEnchantable / the enchantability value. These assertions
 * cover that registration only; which enchantments each item may receive is driven by the vanilla item tags and is
 * pinned by the game test against the real registry.
 */
final class EnchantableEquipmentTest {
    @BeforeAll static void bootstrapRegistries() { Bootstrap.bootStrap(); }

    private static List<Item> equipment() {
        return List.of(ModItems.SPIRIT_STAFF.get(), ModItems.IMMORTAL_ARTIFACT.get(),
                ModItems.SPIRIT_SWORD.get(), ModItems.IMMORTAL_RUIN_FORGED_SPIRIT_SWORD.get(),
                ModItems.ONE_QI_RETURNING_ORIGIN_SWORD.get());
    }

    /** The reported symptom: the enchantment compressor refused the immortal artifact. */
    @Test void theEnchantmentMachinesAcceptTheImmortalEquipment() {
        for (Item item : equipment()) {
            assertTrue(EnchantmentStackRules.acceptsCarrier(new ItemStack(item)),
                    item + " must be accepted as an enchantment carrier");
        }
        assertTrue(EnchantmentStackRules.acceptsCarrier(new ItemStack(Items.ENCHANTED_BOOK)), "books stay carriers");
        assertFalse(EnchantmentStackRules.acceptsCarrier(new ItemStack(Items.DIRT)),
                "a plain block is still not a carrier");
    }

    /**
     * Enchantable registration is what lets the anvil and other enchantment-modifying behaviour find the item. The
     * positive enchantability value itself is line-specific (a 1.21.1 item value, a 26.1.2 data component), so it is
     * pinned by the source contract instead of through a cross-version API call here.
     */
    @Test void theEquipmentIsRegisteredAsEnchantable() {
        for (Item item : equipment()) {
            ItemStack stack = new ItemStack(item);
            assertTrue(stack.isEnchantable(), item + " must register as an enchantable item");
            assertEquals(1, stack.getMaxStackSize(), item + " must be unstackable equipment");
        }
    }

    /** The artifact has no durability; 1.21.1's gate would reject it without the explicit override. */
    @Test void theDurabilityFreeArtifactCountsAsEnchantableToo() {
        ItemStack artifact = new ItemStack(ModItems.IMMORTAL_ARTIFACT.get());
        assertFalse(artifact.has(DataComponents.MAX_DAMAGE), "the artifact stays durability-free");
        assertTrue(artifact.isEnchantable(), "and it must still register as enchantable");
    }
}
