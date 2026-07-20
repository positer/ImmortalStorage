package com.immortalstorage.immortalstorage.villager;

import java.util.function.Supplier;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public final class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, ImmortalStorageMod.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, ImmortalStorageMod.MODID);

    public static final ResourceKey<PoiType> IMMORTAL_FURNACE_POI_KEY = ResourceKey.create(
            Registries.POINT_OF_INTEREST_TYPE,
            ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "immortal_furnace_poi"));

    public static final Supplier<PoiType> IMMORTAL_FURNACE_POI =
            POI_TYPES.register("immortal_furnace_poi", () -> {
                Set<BlockState> states = Set.copyOf(ModBlocks.IMMORTAL_FURNACE.get().getStateDefinition().getPossibleStates());
                return new PoiType(states, 1, 1);
            });

    public static final Supplier<VillagerProfession> IMMORTAL_SAGE =
            PROFESSIONS.register("immortal_sage",
                    () -> new VillagerProfession("immortal_sage",
                            h -> h.is(IMMORTAL_FURNACE_POI_KEY),
                            h -> h.is(IMMORTAL_FURNACE_POI_KEY),
                            ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_FLETCHER));

    private ModVillagers() {}
}
