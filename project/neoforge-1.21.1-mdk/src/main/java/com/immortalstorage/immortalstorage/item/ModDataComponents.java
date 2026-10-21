package com.immortalstorage.immortalstorage.item;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Persistent stack identity used by the frozen generic source block/item. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ImmortalStorageMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>>
            SOURCE_DEFINITION_ID = DATA_COMPONENTS.registerComponentType(
                    "source_definition_id",
                    builder -> builder.persistent(ResourceLocation.CODEC));

    /** Time-Shift Staff tier (0..4 -> 2x..32x) that the next application grants. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            TIME_SHIFT_TIER = DATA_COMPONENTS.registerComponentType(
                    "time_shift_tier",
                    builder -> builder.persistent(Codec.INT));

    private ModDataComponents() {}
}
