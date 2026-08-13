package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.block.custom.VeinKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceVeinPassiveCapabilityTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static RegistryAccess.Frozen registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void disabledItemFacesRemainEnumerableSimulatableAndExtractableWithoutActivelyPushing() {
        SourceVeinBlockEntity source = source(VeinKind.COBBLE, 7L, 32L);
        Map<Direction, ItemStackHandler> activeTargets = new EnumMap<>(Direction.class);

        for (Direction side : Direction.values()) {
            assertEquals(SourceVeinBlockEntity.SourceSideMode.DISABLED, source.getSideMode(side));
            IItemHandler handler = source.getItemHandler(side);
            assertNotNull(handler);
            assertEquals(1, handler.getSlots());
            assertEquals(Integer.MAX_VALUE, handler.getStackInSlot(0).getCount(),
                    side + " must enumerate the full int-compatible source cache while active output is disabled");
            assertEquals(32, handler.extractItem(0, 32, true).getCount(),
                    side + " must support non-consuming storage-bus simulation");
            assertEquals(32, handler.extractItem(0, 32, false).getCount(),
                    side + " must support passive standard-capability extraction");
            activeTargets.put(side, new ItemStackHandler(1));
        }

        source.pushItemsToTargets(activeTargets::get);

        assertTrue(activeTargets.values().stream()
                        .allMatch(target -> target.getStackInSlot(0).isEmpty()),
                "DISABLED must still prevent the source from actively pushing");
        assertEquals(Long.MAX_VALUE, persistedBacking(source),
                "free-source passive extraction must retain its creative backing cache");
    }

    @Test
    void freeItemSourcePublishesItsFullCompatibilityCacheToStorageBusScans() {
        SourceVeinBlockEntity source = source(
                VeinKind.COBBLE, Integer.MAX_VALUE, Integer.MAX_VALUE);

        for (Direction side : Direction.values()) {
            IItemHandler handler = source.getItemHandler(side);
            assertNotNull(handler);
            assertEquals(Integer.MAX_VALUE, handler.getStackInSlot(0).getCount(),
                    side + " must expose the real creative cache instead of one vanilla stack");
            assertEquals(64, handler.extractItem(0, Integer.MAX_VALUE, true).getCount(),
                    "NeoForge extraction results must still respect the item's maximum stack size");
        }
    }

    @Test
    void disabledPaidItemFacesShareTheRealCacheButKeepIndependentFaceBudgets() {
        SourceVeinBlockEntity source = source(VeinKind.STONE, 96L, 32L);
        IItemHandler north = source.getItemHandler(Direction.NORTH);
        IItemHandler south = source.getItemHandler(Direction.SOUTH);

        assertEquals(64, north.getStackInSlot(0).getCount(),
                "a finite standard capability view must remain a protocol-legal ItemStack");
        assertEquals(32, north.extractItem(0, 32, true).getCount());
        assertEquals(96L, persistedBacking(source), "simulation must not consume prepaid cache");
        assertEquals(32, north.extractItem(0, 32, false).getCount());
        assertTrue(north.extractItem(0, 1, false).isEmpty(),
                "one face cannot exceed its configured per-tick budget");
        assertEquals(32, south.extractItem(0, 32, false).getCount(),
                "a saturated face must not consume another face's budget");
        assertEquals(32L, persistedBacking(source),
                "paid passive extraction must consume the shared persistent cache exactly once");
    }

    @Test
    void disabledFluidFacesRemainEnumerableSimulatableAndExtractableWithoutActivelyPushing() {
        SourceVeinBlockEntity source = source(VeinKind.WATER, 7L, 250L);
        Map<Direction, FluidTank> activeTargets = new EnumMap<>(Direction.class);

        for (Direction side : Direction.values()) {
            assertEquals(SourceVeinBlockEntity.SourceSideMode.DISABLED, source.getSideMode(side));
            IFluidHandler handler = source.getFluidHandler(side);
            assertNotNull(handler);
            assertEquals(1, handler.getTanks());
            assertEquals(Integer.MAX_VALUE, handler.getFluidInTank(0).getAmount(),
                    side + " must enumerate the full int-compatible fluid cache while active output is disabled");
            assertEquals(250, handler.drain(250, IFluidHandler.FluidAction.SIMULATE).getAmount());
            assertEquals(250, handler.drain(250, IFluidHandler.FluidAction.EXECUTE).getAmount());
            activeTargets.put(side, new FluidTank(1_000));
        }

        source.pushFluidToTargets(activeTargets::get);

        assertTrue(activeTargets.values().stream().allMatch(target -> target.getFluidAmount() == 0),
                "DISABLED must still prevent the source from actively filling neighbors");
        assertEquals(Long.MAX_VALUE, persistedBacking(source),
                "free-fluid passive extraction must retain its creative backing cache");
    }

    private static SourceVeinBlockEntity source(VeinKind kind, long cachedUnits, long fluxLimit) {
        SourceVeinBlockEntity source = new SourceVeinBlockEntity(
                BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState(), kind);
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind.name());
        tag.putLong("CachedUnits", cachedUnits);
        tag.putLong("FluxLimit", fluxLimit);
        source.loadAdditionalLegacy(tag, registries);
        return source;
    }

    private static long persistedBacking(SourceVeinBlockEntity source) {
        CompoundTag tag = new CompoundTag();
        source.saveAdditionalLegacy(tag, registries);
        return tag.getLongOr("CachedUnits", 0L);
    }
}
