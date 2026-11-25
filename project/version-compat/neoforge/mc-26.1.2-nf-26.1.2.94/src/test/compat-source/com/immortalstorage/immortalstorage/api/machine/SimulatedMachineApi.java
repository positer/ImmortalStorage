package com.immortalstorage.immortalstorage.api.machine;

import com.immortalstorage.immortalstorage.spiritfield.SimulatedSpiritFieldCropCatalog;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import java.util.List;
import java.util.Optional;

/** Seed-to-crop defaults for non-BlockItem seeds. Register during common setup, before IS freezes registration. */
public final class SimulatedMachineApi {
    private SimulatedMachineApi() {}
    /** Substrates are block ids or #block tags; empty means the mapping accepts every substrate. */
    public static void registerCrop(Item seed, BlockState crop, List<String> substrates) {
        var rules = substrates.stream().map(selector -> {
            if (selector.startsWith("#")) return new SimulatedSpiritFieldCropCatalog.SubstrateRule(
                    null, BlockTags.create(Identifier.parse(selector.substring(1))));
            var id = Identifier.parse(selector);
            return new SimulatedSpiritFieldCropCatalog.SubstrateRule(BuiltInRegistries.BLOCK.getOptional(id)
                    .orElseThrow(() -> new IllegalArgumentException("unknown substrate " + id)), null);
        }).toList();
        SimulatedSpiritFieldCropCatalog.register(seed, new SimulatedSpiritFieldCropCatalog.Entry(crop, rules));
    }
    /** Explicit mappings only; absent means IS uses its existing automatic crop inference. */
    public static Optional<BlockState> crop(Item seed) {
        return SimulatedSpiritFieldCropCatalog.find(seed).map(SimulatedSpiritFieldCropCatalog.Entry::crop);
    }
    public static long cropGeneration() { return SimulatedSpiritFieldCropCatalog.generation(); }
}
