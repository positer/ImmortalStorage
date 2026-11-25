package com.immortalstorage.immortalstorage.spiritfield;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.entity.SimulatedSpiritFieldBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SimulatedCropReloadTest {
    @Test @SuppressWarnings("unchecked") void loadedFieldRefreshesMappingWithoutReplacingItsSeed() throws Exception {
        Bootstrap.bootStrap();
        var entriesField=SimulatedSpiritFieldCropCatalog.class.getDeclaredField("entries");
        entriesField.setAccessible(true);
        var previous=(Map<Item,SimulatedSpiritFieldCropCatalog.Entry>)entriesField.get(null);
        var field=new SimulatedSpiritFieldBlockEntity(BlockPos.ZERO,ModBlocks.SIMULATED_SPIRIT_FIELD.get().defaultBlockState());
        field.setItem(0,new ItemStack(Items.CHORUS_FRUIT));
        try {
            SimulatedSpiritFieldCropCatalog.install(Map.of(Items.CHORUS_FRUIT,
                    new SimulatedSpiritFieldCropCatalog.Entry(Blocks.WHEAT.defaultBlockState(),List.of())));
            assertTrue(field.displayCrop().orElseThrow().is(Blocks.WHEAT));
            long generation=SimulatedSpiritFieldCropCatalog.generation();
            SimulatedSpiritFieldCropCatalog.install(Map.of(Items.CHORUS_FRUIT,
                    new SimulatedSpiritFieldCropCatalog.Entry(Blocks.CARROTS.defaultBlockState(),List.of())));
            assertTrue(SimulatedSpiritFieldCropCatalog.generation()>generation);
            assertTrue(field.displayCrop().orElseThrow().is(Blocks.CARROTS));
            assertTrue(field.getItem(0).is(Items.CHORUS_FRUIT));
            SimulatedSpiritFieldCropCatalog.install(Map.of());
            assertTrue(field.displayCrop().orElseThrow().is(Blocks.CHORUS_FLOWER),"removed mapping restores automatic inference");
        } finally { SimulatedSpiritFieldCropCatalog.install(previous); }
    }
}
