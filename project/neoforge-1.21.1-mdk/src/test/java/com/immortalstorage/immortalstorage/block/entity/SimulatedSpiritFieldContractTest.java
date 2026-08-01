package com.immortalstorage.immortalstorage.block.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.menu.custom.ImmortalFurnaceEngine;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SimulatedSpiritFieldContractTest {
    @BeforeAll static void bootstrap() { Bootstrap.bootStrap(); }

    @Test void matchesReincarnationFurnaceInventoryShapeAndTiming() {
        assertEquals(SimulatedReincarnationFurnaceBlockEntity.SOURCE_SLOT,
                SimulatedSpiritFieldBlockEntity.SEED_SLOT);
        assertEquals(SimulatedReincarnationFurnaceBlockEntity.FUEL_SLOT,
                SimulatedSpiritFieldBlockEntity.FUEL_SLOT);
        assertEquals(SimulatedReincarnationFurnaceBlockEntity.WEAPON_SLOT,
                SimulatedSpiritFieldBlockEntity.TOOL_SLOT);
        assertEquals(SimulatedReincarnationFurnaceBlockEntity.OUTPUT_START,
                SimulatedSpiritFieldBlockEntity.OUTPUT_START);
        assertEquals(SimulatedReincarnationFurnaceBlockEntity.SLOT_COUNT,
                SimulatedSpiritFieldBlockEntity.SLOT_COUNT);
        assertEquals(50, SimulatedSpiritFieldBlockEntity.PROCESS_TICKS);
        assertEquals(150, ImmortalFurnaceEngine.TRUE_YUAN.burnTicks());
        assertEquals(500, ImmortalFurnaceEngine.IMMORTAL_YUAN.burnTicks());
    }

    @Test void sidedAutomationKeepsInputsAndAddsConfiguredSixFaceOutputs() {
        SimulatedSpiritFieldBlockEntity field = new SimulatedSpiritFieldBlockEntity(
                net.minecraft.core.BlockPos.ZERO,
                com.immortalstorage.immortalstorage.block.ModBlocks.SIMULATED_SPIRIT_FIELD.get().defaultBlockState());
        assertArrayEquals(new int[]{0}, field.getSlotsForFace(Direction.UP));
        assertArrayEquals(new int[]{1}, field.getSlotsForFace(Direction.NORTH));
        assertArrayEquals(new int[]{1}, field.getSlotsForFace(Direction.EAST));
        assertArrayEquals(new int[]{1}, field.getSlotsForFace(Direction.SOUTH));
        assertArrayEquals(new int[]{1}, field.getSlotsForFace(Direction.WEST));
        assertEquals(0, field.getSlotsForFace(Direction.DOWN).length);
        field.toggleOutputFace(Direction.UP);
        field.toggleOutputFace(Direction.DOWN);
        assertEquals(13, field.getSlotsForFace(Direction.UP).length);
        assertEquals(12, field.getSlotsForFace(Direction.DOWN).length);
        assertTrue(field.canTakeItemThroughFace(SimulatedSpiritFieldBlockEntity.OUTPUT_START,
                ItemStack.EMPTY, Direction.UP));
    }

    @Test void recipeProducesFourAndRarityIsUncommon() throws Exception {
        JsonObject recipe = resource("/data/immortalstorage/recipe/simulated_spirit_field.json");
        assertEquals("CQC", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("NGN", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("CUC", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals(4, recipe.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(Rarity.UNCOMMON, ModItems.rarityFor("simulated_spirit_field"));
    }

    @Test void frameUsesOnlySmoothStoneAndCropListsAreDataPackExtensible() throws Exception {
        JsonObject model = resource("/assets/immortalstorage/models/block/simulated_spirit_field.json");
        assertEquals("immortalstorage:block/arcane_machine_frame", model.get("parent").getAsString());
        assertEquals("minecraft:block/smooth_stone",
                model.getAsJsonObject("textures").get("frame").getAsString());
        assertNotNull(SimulatedSpiritFieldContractTest.class.getResource(
                "/data/immortalstorage/tags/item/simulated_spirit_field_seeds.json"));
        assertNotNull(SimulatedSpiritFieldContractTest.class.getResource(
                "/data/immortalstorage/tags/block/simulated_spirit_field_substrates.json"));
        assertNotNull(SimulatedSpiritFieldContractTest.class.getResource(
                "/data/immortalstorage/simulated_spirit_field_crops/chorus_flower.json"));
        assertNotNull(SimulatedSpiritFieldContractTest.class.getResource(
                "/data/immortalstorage/simulated_spirit_field_crops/chorus_fruit.json"));
        assertNotNull(SimulatedSpiritFieldContractTest.class.getResource(
                "/data/immortalstorage/simulated_spirit_field_crops/nether_wart.json"));
    }

    @Test void chorusFruitIsPermanentSeedWithDedicatedFlowerAndFruitYield() throws Exception {
        SimulatedSpiritFieldBlockEntity field = new SimulatedSpiritFieldBlockEntity(
                net.minecraft.core.BlockPos.ZERO,
                com.immortalstorage.immortalstorage.block.ModBlocks.SIMULATED_SPIRIT_FIELD.get().defaultBlockState());
        assertTrue(field.isValidSeed(new ItemStack(Items.CHORUS_FRUIT)));
        Path source = locateProject().resolve(
                "src/main/java/com/immortalstorage/immortalstorage/block/entity/SimulatedSpiritFieldBlockEntity.java");
        String code = Files.readString(source);
        assertTrue(code.contains("new ItemStack(Items.CHORUS_FLOWER)"));
        assertTrue(code.contains("new ItemStack(Items.CHORUS_FRUIT, 2)"));
        assertTrue(code.contains("displaysGrowingChorusFlower"));
    }

    @Test void seedSlotIsAReusableSpecimenAndIsNeverConsumedByAWorkCycle() throws Exception {
        Path source = locateProject().resolve(
                "src/main/java/com/immortalstorage/immortalstorage/block/entity/SimulatedSpiritFieldBlockEntity.java");
        String code = Files.readString(source);
        assertTrue(code.contains("seed is a permanent specimen"));
        assertTrue(!code.contains("items.get(SEED_SLOT).shrink"));
        assertTrue(!code.contains("removeItem(SEED_SLOT"));
    }

    private static JsonObject resource(String path) throws Exception {
        var stream = SimulatedSpiritFieldContractTest.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src/main"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate NeoForge project");
    }
}
