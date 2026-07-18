package com.cultivation.cultivation.item;

import com.cultivation.cultivation.CultivationMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Persistent stack identity used by the frozen generic source block/item. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CultivationMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>>
            SOURCE_DEFINITION_ID = DATA_COMPONENTS.registerComponentType(
                    "source_definition_id",
                    builder -> builder.persistent(ResourceLocation.CODEC));

    private ModDataComponents() {}
}
