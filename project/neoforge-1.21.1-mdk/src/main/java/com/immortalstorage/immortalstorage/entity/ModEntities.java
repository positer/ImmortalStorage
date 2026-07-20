package com.immortalstorage.immortalstorage.entity;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ImmortalStorageMod.MODID);

    private ModEntities() {}
}
