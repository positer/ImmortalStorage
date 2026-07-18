package com.cultivation.cultivation.entity;

import com.cultivation.cultivation.CultivationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CultivationMod.MODID);

    private ModEntities() {}
}
