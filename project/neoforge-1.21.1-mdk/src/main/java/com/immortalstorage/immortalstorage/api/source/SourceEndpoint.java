package com.immortalstorage.immortalstorage.api.source;

import com.immortalstorage.immortalstorage.block.custom.VeinKind;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Stable view of a source block endpoint.
 *
 * Use the standard fluid handler when {@link #fluidHandler()} is non-null.
 * Item sources expose an item sample plus the configured output rate.
 */
public interface SourceEndpoint {
    ResourceLocation sourceDefinitionId();
    VeinKind kind();
    @Nullable UUID owner();
    long fluxLimit();
    long outputCostPerTick();
    SourceChargePlan chargePlan();
    boolean activeOutput();
    boolean fluidSource();
    Fluid fluid();
    ItemStack itemSample(int count);
    @Nullable IFluidHandler fluidHandler();
}
