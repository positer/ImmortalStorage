package com.immortalstorage.immortalstorage.gametest;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.api.machine.MachinePolicyApi;
import com.immortalstorage.immortalstorage.api.machine.MachinePolicyApi.*;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.entity.*;
import com.immortalstorage.immortalstorage.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Synchronous server-thread fixture; restores the policy before other tests can tick. */
@GameTestHolder(ImmortalStorageMod.MODID)
@PrefixGameTestTemplate(false)
public final class MachinePolicyGameTest {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void policiesGateRealProductionAndRestoreDefaultFuelAndDrops(GameTestHelper helper) {
        var saved = MachinePolicyApi.rules();
        BlockPos fieldPos = new BlockPos(1,1,1), furnacePos = new BlockPos(2,1,1);
        try {
            helper.setBlock(fieldPos, ModBlocks.SIMULATED_SPIRIT_FIELD.get());
            helper.setBlock(furnacePos, ModBlocks.SIMULATED_REINCARNATION_FURNACE.get());
            SimulatedSpiritFieldBlockEntity field = helper.getBlockEntity(fieldPos);
            SimulatedReincarnationFurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos);
            var level=helper.getLevel();
            // Both machines default to direct Xianqiao output. This fixture has no bound owner,
            // so explicitly select local output before asserting the visible cache contents.
            field.toggleXianqiaoOutput();
            furnace.toggleXianqiaoOutput();
            helper.assertTrue(!field.xianqiaoOutput() && !furnace.xianqiaoOutput(),"fixture must use local output");
            field.setItem(0,new ItemStack(Items.WHEAT_SEEDS));
            field.setItem(1,new ItemStack(ModItems.TRUE_YUAN.get(),4));
            var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
            var entityTag = new net.minecraft.nbt.CompoundTag();
            helper.assertTrue(pig != null && pig.save(entityTag),"fixture must contain a complete pig specimen");
            var sourceTag = new net.minecraft.nbt.CompoundTag(); sourceTag.put("containedEntity",entityTag);
            var soul = new ItemStack(ModItems.SOUL_CATCHER.get());
            soul.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(sourceTag));
            furnace.setItem(0,soul);
            furnace.setItem(1,new ItemStack(ModItems.TRUE_YUAN.get(),4));
            MachinePolicyApi.install(Map.of(
                ResourceLocation.parse("test:seed"),new Rule(Machine.SIMULATED_SPIRIT_FIELD,1,1,ResourceLocation.parse("minecraft:wheat"),Target.INPUT_ITEM,List.of(),List.of("minecraft:wheat_seeds")),
                ResourceLocation.parse("test:pig"),new Rule(Machine.SIMULATED_REINCARNATION_FURNACE,1,1,ResourceLocation.parse("minecraft:pig"),Target.INPUT_ENTITY,List.of(),List.of("minecraft:pig"))));
            for(int i=0;i<60;i++) {
                SimulatedSpiritFieldBlockEntity.serverTick(level,field.getBlockPos(),field.getBlockState(),field);
                SimulatedReincarnationFurnaceBlockEntity.serverTick(level,furnace.getBlockPos(),furnace.getBlockState(),furnace);
            }
            helper.assertTrue(field.getItem(1).getCount()==4 && field.burnTicks()==0,"blocked field must not consume fuel");
            helper.assertTrue(furnace.getItem(1).getCount()==4 && furnace.burnTicks()==0,"blocked furnace must not consume fuel");
            MachinePolicyApi.install(Map.of(ResourceLocation.parse("test:wheat"),new Rule(Machine.SIMULATED_SPIRIT_FIELD,1,1,null,Target.OUTPUT_ITEM,List.of(),List.of("minecraft:wheat"))));
            for(int i=0;i<50;i++) SimulatedSpiritFieldBlockEntity.serverTick(level,field.getBlockPos(),field.getBlockState(),field);
            helper.assertTrue(count(field,Items.WHEAT)==0,"field output blacklist must reach harvest execution");
            MachinePolicyApi.install(Map.of());
            for(int i=0;i<50;i++) {
                SimulatedSpiritFieldBlockEntity.serverTick(level,field.getBlockPos(),field.getBlockState(),field);
                SimulatedReincarnationFurnaceBlockEntity.serverTick(level,furnace.getBlockPos(),furnace.getBlockState(),furnace);
            }
            helper.assertTrue(count(field,Items.WHEAT)>0,"removing rules must restore default harvest");
            helper.assertTrue(count(furnace,Items.PORKCHOP)>0,"removing rules must restore default furnace drops");
            helper.assertTrue(field.getItem(0).getCount()==1 && furnace.getItem(0).getCount()==1,"specimens remain reusable");
            helper.succeed();
        } finally { MachinePolicyApi.install(saved); }
    }
    private static int count(net.minecraft.world.Container container, Item item) {
        int count=0;
        for(int i=3;i<container.getContainerSize();i++) if(container.getItem(i).is(item)) count+=container.getItem(i).getCount();
        return count;
    }
}
