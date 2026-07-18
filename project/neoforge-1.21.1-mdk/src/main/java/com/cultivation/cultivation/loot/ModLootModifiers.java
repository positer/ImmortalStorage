package com.cultivation.cultivation.loot;

import java.util.function.Supplier;

import com.cultivation.cultivation.CultivationMod;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, CultivationMod.MODID);

    public static final Supplier<MapCodec<com.cultivation.cultivation.loot.custom.AddItemModifier>> ADD_ITEM =
            LOOT_MODIFIERS.register("add_item", () -> com.cultivation.cultivation.loot.custom.AddItemModifier.CODEC);

    private ModLootModifiers() {}
}
