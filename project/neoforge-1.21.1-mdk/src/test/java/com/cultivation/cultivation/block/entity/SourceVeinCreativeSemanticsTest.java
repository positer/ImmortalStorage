package com.cultivation.cultivation.block.entity;

import com.cultivation.cultivation.block.custom.VeinKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class SourceVeinCreativeSemanticsTest {
    private static RegistryAccess.Frozen registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void freeItemSourceReportsCreativeAmountsAndNeverConsumesItsBackingCache() {
        SourceVeinBlockEntity source = source(VeinKind.COBBLE, 7L, 1L);
        IItemHandler handler = source.getItemHandler();
        assertNotNull(handler);

        assertEquals(Long.MAX_VALUE, source.cachedUnits());
        assertEquals(Integer.MAX_VALUE, handler.getStackInSlot(0).getCount());
        assertEquals(Integer.MAX_VALUE, handler.getSlotLimit(0));
        assertEquals(64, handler.extractItem(0, Integer.MAX_VALUE, true).getCount(),
                "IItemHandler extraction must still obey the ItemStack maximum");
        assertEquals(Long.MAX_VALUE, persistedBacking(source),
                "free sources must materialize their authoritative long cache during load");

        assertEquals(64, handler.extractItem(0, Integer.MAX_VALUE, false).getCount());
        assertEquals(64, handler.extractItem(0, Integer.MAX_VALUE, false).getCount());
        assertEquals(64, handler.extractItem(0, Integer.MAX_VALUE, false).getCount(),
                "creative extraction must ignore the ordinary per-tick flux limit");
        assertEquals(Long.MAX_VALUE, persistedBacking(source),
                "executed creative extraction must not consume the real cache");
        assertEquals(Long.MAX_VALUE, source.cachedUnits());
    }

    @Test
    void freeFluidSourceReportsIntMaxAndSimulationExecutionAndRollbackNeverConsumeBacking() {
        SourceVeinBlockEntity source = source(VeinKind.WATER, 7L, 1L);
        IFluidHandler handler = source.getFluidHandler();
        assertNotNull(handler);

        assertEquals(Long.MAX_VALUE, source.cachedUnits());
        assertEquals(Integer.MAX_VALUE, handler.getFluidInTank(0).getAmount());
        assertEquals(Integer.MAX_VALUE, handler.getTankCapacity(0));
        assertEquals(Integer.MAX_VALUE,
                handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE).getAmount());
        assertEquals(Long.MAX_VALUE, persistedBacking(source));
        assertEquals(1_000, handler.fill(new FluidStack(Fluids.LAVA, 1_000),
                IFluidHandler.FluidAction.SIMULATE));
        assertEquals(1_000, handler.fill(new FluidStack(Fluids.LAVA, 1_000),
                IFluidHandler.FluidAction.EXECUTE));
        assertEquals(Long.MAX_VALUE, persistedBacking(source),
                "void input must destroy arbitrary fluids without mutating source cache");

        FluidStack executed = handler.drain(
                new FluidStack(Fluids.WATER, Integer.MAX_VALUE), IFluidHandler.FluidAction.EXECUTE);
        assertEquals(Integer.MAX_VALUE, executed.getAmount());
        assertEquals(Long.MAX_VALUE, persistedBacking(source));
        assertEquals(Integer.MAX_VALUE,
                handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE).getAmount(),
                "same-tick creative drains must ignore the ordinary per-tick flux limit");
        assertEquals(Long.MAX_VALUE, persistedBacking(source));
        source.rollbackFluidExtraction(executed.getAmount());
        assertEquals(Integer.MAX_VALUE,
                handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE).getAmount(),
                "creative rollback is a no-op and must not add finite backing");
        assertEquals(Long.MAX_VALUE, persistedBacking(source));
    }

    @Test
    void everyFluidSourceFaceAlwaysVoidsAnyIncomingFluid() {
        SourceVeinBlockEntity source = source(VeinKind.WATER, 64L, 1L);
        long before = persistedBacking(source);
        for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
            IFluidHandler handler = source.getFluidHandler(side);
            assertNotNull(handler);
            assertEquals(750, handler.fill(new FluidStack(Fluids.LAVA, 750),
                    IFluidHandler.FluidAction.EXECUTE));
        }
        assertEquals(before, persistedBacking(source));
    }

    @Test
    void paidItemSourceRetainsFiniteSimulationAndConsumptionSemantics() {
        SourceVeinBlockEntity source = source(VeinKind.STONE, 128L, Integer.MAX_VALUE);
        IItemHandler handler = source.getItemHandler();
        assertNotNull(handler);

        assertEquals(128L, source.cachedUnits());
        assertEquals(64, handler.getStackInSlot(0).getCount());
        assertEquals(Integer.MAX_VALUE, handler.getSlotLimit(0),
                "the int adapter advertises its saturated logical slot capacity");
        assertEquals(64, handler.extractItem(0, Integer.MAX_VALUE, true).getCount());
        assertEquals(128L, persistedBacking(source));

        ItemStack extracted = handler.extractItem(0, Integer.MAX_VALUE, false);
        assertEquals(64, extracted.getCount());
        assertEquals(64L, persistedBacking(source));
        assertEquals(64L, source.cachedUnits());
    }

    private static SourceVeinBlockEntity source(VeinKind kind, long cachedUnits, long fluxLimit) {
        SourceVeinBlockEntity source = new SourceVeinBlockEntity(
                BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState(), kind);
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind.name());
        tag.putLong("CachedUnits", cachedUnits);
        tag.putLong("FluxLimit", fluxLimit);
        source.loadAdditional(tag, registries);
        return source;
    }

    private static long persistedBacking(SourceVeinBlockEntity source) {
        CompoundTag tag = new CompoundTag();
        source.saveAdditional(tag, registries);
        return tag.getLong("CachedUnits");
    }
}
