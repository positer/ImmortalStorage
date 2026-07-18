package com.cultivation.cultivation.block.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.cultivation.cultivation.item.custom.SpiritDriveItem;
import com.cultivation.cultivation.dimension.CultivationDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImmortalFurnaceStandaloneContractTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void legacySlotsStayFixedWhileTwoChannelsAreAppended() {
        assertEquals(0, ImmortalFurnaceBlockEntity.INPUT_1);
        assertEquals(1, ImmortalFurnaceBlockEntity.FUEL);
        assertEquals(2, ImmortalFurnaceBlockEntity.RESULT_1);
        assertEquals(7, ImmortalFurnaceBlockEntity.SLOT_COUNT);
    }

    @Test
    void automationExposesInputsAboveFuelAtSidesAndResultsBelow() {
        assertArrayEquals(new int[] {0, 3, 5},
                ImmortalFurnaceBlockEntity.automationSlots(Direction.UP));
        assertArrayEquals(new int[] {1},
                ImmortalFurnaceBlockEntity.automationSlots(Direction.NORTH));
        assertArrayEquals(new int[] {2, 4, 6},
                ImmortalFurnaceBlockEntity.automationSlots(Direction.DOWN));
        assertTrue(ImmortalFurnaceBlockEntity.automationCanInsert(0, Direction.UP));
        assertTrue(ImmortalFurnaceBlockEntity.automationCanInsert(1, Direction.WEST));
        assertFalse(ImmortalFurnaceBlockEntity.automationCanInsert(2, Direction.DOWN));
        assertTrue(ImmortalFurnaceBlockEntity.automationCanExtract(4, Direction.DOWN));
        assertFalse(ImmortalFurnaceBlockEntity.automationCanExtract(0, Direction.DOWN));
    }

    @Test
    void furnaceOnlyRecipesUseTheDedicatedSerializerType() throws Exception {
        assertDedicatedRecipe("breakthrough_pill_smelting.json");
        assertDedicatedRecipe("stone_vein_smelting.json");
        assertDedicatedRecipe("smooth_stone_vein_smelting.json");
        assertDedicatedRecipe("crude_pill_immortal_furnace.json");
        assertDedicatedRecipe("refined_pill_immortal_furnace.json");
        assertDedicatedRecipe("crude_spirit_iron_immortal_furnace.json");
    }

    @Test
    void boundSpiritDriveIsAReusableCredentialRatherThanAStoredCharge() {
        SpiritDriveItem driveItem = (SpiritDriveItem) com.cultivation.cultivation.item.ModItems.SPIRIT_DRIVE.get();
        ItemStack drive = new ItemStack(driveItem);
        UUID owner = UUID.fromString("12345678-1234-5678-1234-567812345678");
        assertTrue(SpiritDriveItem.bind(drive, owner, "Cultivator"));
        assertFalse(SpiritDriveItem.bind(drive,
                UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"), "Intruder"));
        assertEquals(150, com.cultivation.cultivation.menu.custom.ImmortalFurnaceEngine.TRUE_YUAN.burnTicks());
        assertEquals(500, com.cultivation.cultivation.menu.custom.ImmortalFurnaceEngine.IMMORTAL_YUAN.burnTicks());
        assertFalse(ImmortalFurnaceBlockEntity.isImmortalFuel(new ItemStack(Items.COAL)));
        ImmortalFurnaceBlockEntity.consumeFuelCharge(drive, true);
        assertEquals(Optional.of(owner), SpiritDriveItem.owner(drive));
        assertEquals(Optional.of("Cultivator"), SpiritDriveItem.ownerName(drive));
        assertEquals(1, drive.getCount(), "the reusable drive container must remain in the fuel slot");
    }

    @Test
    void failedDrivePaymentUsesFiveTickRetryCadence() {
        assertTrue(ImmortalFurnaceBlockEntity.retryDue(100L, Long.MIN_VALUE));
        assertFalse(ImmortalFurnaceBlockEntity.retryDue(104L, 105L));
        assertTrue(ImmortalFurnaceBlockEntity.retryDue(105L, 105L));
        assertEquals(5, ImmortalFurnaceBlockEntity.SPIRIT_DRIVE_RETRY_TICKS);
    }

    @Test
    void automaticFuelIsBoundToThePersonalRealmOwnerInsteadOfTheNearestPlayer() {
        UUID owner = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        assertEquals(Optional.of(owner), ImmortalFurnaceBlockEntity.autoConsumeOwner(
                CultivationDimensions.personalRealmKey(owner)));
        assertEquals(Optional.empty(), ImmortalFurnaceBlockEntity.autoConsumeOwner(Level.OVERWORLD));
    }

    @Test
    void neoForgeSidedWrapperEnforcesTheSameDirectionContract() {
        DirectionalInventory inventory = new DirectionalInventory();
        SidedInvWrapper top = new SidedInvWrapper(inventory, Direction.UP);
        SidedInvWrapper side = new SidedInvWrapper(inventory, Direction.NORTH);
        SidedInvWrapper bottom = new SidedInvWrapper(inventory, Direction.DOWN);

        assertEquals(3, top.getSlots());
        assertTrue(top.insertItem(0, new ItemStack(Items.IRON_ORE, 3), false).isEmpty());
        assertEquals(3, inventory.getItem(0).getCount());
        assertEquals(1, side.getSlots());
        assertTrue(side.insertItem(0, new ItemStack(Items.BLAZE_ROD), false).isEmpty());
        assertEquals(Items.BLAZE_ROD, inventory.getItem(1).getItem());

        inventory.setItem(4, new ItemStack(Items.IRON_INGOT, 2));
        assertEquals(3, bottom.getSlots());
        assertEquals(2, bottom.extractItem(1, 2, false).getCount());
        assertFalse(bottom.insertItem(0, new ItemStack(Items.IRON_ORE), false).isEmpty());
    }

    private static void assertDedicatedRecipe(String file) throws Exception {
        String path = "/data/cultivation/recipe/" + file;
        var stream = ImmortalFurnaceStandaloneContractTest.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("cultivation:immortal_furnace", json.get("type").getAsString(), path);
        }
    }

    private static final class DirectionalInventory extends SimpleContainer implements WorldlyContainer {
        private DirectionalInventory() {
            super(ImmortalFurnaceBlockEntity.SLOT_COUNT);
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return ImmortalFurnaceBlockEntity.automationSlots(side);
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
            return ImmortalFurnaceBlockEntity.automationCanInsert(slot, side) && canPlaceItem(slot, stack);
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return ImmortalFurnaceBlockEntity.automationCanExtract(slot, side);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == ImmortalFurnaceBlockEntity.FUEL) return stack.is(Items.BLAZE_ROD);
            return slot == ImmortalFurnaceBlockEntity.INPUT_1
                    || slot == ImmortalFurnaceBlockEntity.INPUT_2
                    || slot == ImmortalFurnaceBlockEntity.INPUT_3;
        }
    }
}
