package com.immortalstorage.immortalstorage.villager;

import com.google.common.collect.ImmutableSet;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Supplier;

/** NeoForge 26.1 profession registration using the official trade-set map. */
public final class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, ImmortalStorageMod.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, ImmortalStorageMod.MODID);

    public static final ResourceKey<PoiType> IMMORTAL_FURNACE_POI_KEY = ResourceKey.create(
            Registries.POINT_OF_INTEREST_TYPE,
            Identifier.fromNamespaceAndPath(ImmortalStorageMod.MODID, "immortal_furnace_poi"));

    public static final Supplier<PoiType> IMMORTAL_FURNACE_POI =
            POI_TYPES.register("immortal_furnace_poi", () -> {
                Set<BlockState> states = Set.copyOf(ModBlocks.IMMORTAL_FURNACE.get()
                        .getStateDefinition().getPossibleStates());
                return new PoiType(states, 1, 1);
            });

    public static final Supplier<VillagerProfession> IMMORTAL_SAGE =
            PROFESSIONS.register("immortal_sage", () -> new VillagerProfession(
                    Component.translatable("entity.minecraft.villager.immortalstorage.immortal_sage"),
                    holder -> holder.is(IMMORTAL_FURNACE_POI_KEY),
                    holder -> holder.is(IMMORTAL_FURNACE_POI_KEY),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_FLETCHER,
                    new Int2ObjectOpenHashMap<>()));

    private ModVillagers() {
    }
}
