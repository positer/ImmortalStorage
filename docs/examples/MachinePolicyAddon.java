package example;

import com.immortalstorage.immortalstorage.api.machine.MachinePolicyApi;
import com.immortalstorage.immortalstorage.api.machine.SimulatedMachineApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import java.util.List;

/** Call register() directly from the addon's FMLCommonSetupEvent listener. */
public final class MachinePolicyAddon {
    public static void register() {
        MachinePolicyApi.register(ResourceLocation.parse("example:lower_level_ores"),
                new MachinePolicyApi.Rule(MachinePolicyApi.Machine.WORLD_SHARD_MINER, 1, 2,
                        null, MachinePolicyApi.Target.OUTPUT_ITEM, List.of(), List.of("minecraft:diamond_ore")));
        SimulatedMachineApi.registerCrop(Items.WHEAT_SEEDS,
                Blocks.WHEAT.defaultBlockState(), List.of("minecraft:farmland"));
    }
}
