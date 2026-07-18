package com.cultivation.cultivation.block.entity;

import com.cultivation.cultivation.api.storage.terminal.StorageItemSummary;
import com.cultivation.cultivation.api.storage.terminal.TerminalEntryKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalItemStorage;
import com.cultivation.cultivation.api.storage.terminal.TerminalStorageAction;
import com.cultivation.cultivation.block.custom.VeinKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceVeinActiveOutputRateTest {
    private static RegistryAccess.Frozen registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void freeFluidPushHonorsAnIndependentConfiguredPerTickRateForEveryFace() {
        SourceVeinBlockEntity source = source(VeinKind.WATER, 7L, 250_000L);

        assertEquals(250_000L, source.extractForActiveOutput(Direction.EAST, 1_000_000L, true));
        assertEquals(250_000L, source.extractForActiveOutput(Direction.EAST, 1_000_000L, false));
        assertEquals(0L, source.extractForActiveOutput(Direction.EAST, 1_000_000L, false));
        assertEquals(250_000L, source.extractForActiveOutput(Direction.WEST, 1_000_000L, false),
                "one saturated PUSH face must not reduce any other face's allowance");
        assertEquals(Long.MAX_VALUE, persistedBacking(source),
                "creative water remains non-consuming and materializes its long cache");
    }

    @Test
    void freeSourcesDefaultToTheMaximumIntCompatiblePushRate() {
        SourceVeinBlockEntity source = new SourceVeinBlockEntity(
                BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState(), VeinKind.WATER);

        assertEquals(Integer.MAX_VALUE, source.getFluxLimit());
    }

    @Test
    void itemTransferPassDoesNotLetOneSaturatedFaceSuppressAnotherFace() {
        SourceVeinBlockEntity source = source(VeinKind.COBBLE, 7L, 32L);
        ItemStackHandler east = new ItemStackHandler(2);
        ItemStackHandler west = new ItemStackHandler(2);
        source.setSideMode(Direction.EAST, SourceVeinBlockEntity.SourceSideMode.byId(2));
        source.setSideMode(Direction.WEST, SourceVeinBlockEntity.SourceSideMode.byId(2));

        source.pushItemsToHandler(Direction.EAST, east, new ItemStack(Items.COBBLESTONE));
        source.pushItemsToHandler(Direction.WEST, west, new ItemStack(Items.COBBLESTONE));

        assertEquals(32, east.getStackInSlot(0).getCount());
        assertEquals(32, west.getStackInSlot(0).getCount(),
                "finishing EAST's multi-slot pass must continue with WEST's independent budget");
    }

    @Test
    void fluidTransferPassAppliesTheConfiguredRateIndependentlyToEachFace() {
        SourceVeinBlockEntity source = source(VeinKind.WATER, 7L, 250_000L);
        FluidTank east = new FluidTank(1_000_000);
        FluidTank west = new FluidTank(1_000_000);
        source.setSideMode(Direction.EAST, SourceVeinBlockEntity.SourceSideMode.byId(2));
        source.setSideMode(Direction.WEST, SourceVeinBlockEntity.SourceSideMode.byId(2));

        source.pushFluidToHandler(Direction.EAST, east, Fluids.WATER);
        source.pushFluidToHandler(Direction.WEST, west, Fluids.WATER);

        assertEquals(250_000, east.getFluidAmount());
        assertEquals(250_000, west.getFluidAmount());
    }

    @Test
    void oneDispatchPassCanDeliverSixTimesTheConfiguredRateToAggregateStorage() {
        SourceVeinBlockEntity source = source(VeinKind.COBBLE, 7L, 250_000L);
        for (Direction direction : Direction.values()) {
            source.setSideMode(direction, SourceVeinBlockEntity.SourceSideMode.byId(3));
        }
        CountingTerminalStorage storage = new CountingTerminalStorage();
        XianqiaoInterfaceInventory xianqiaoInterface =
                new XianqiaoInterfaceInventory(storage, () -> true);

        source.pushItemsToTargets(direction -> xianqiaoInterface);

        assertEquals(1_500_000L, storage.amount,
                "six PUSH faces must each receive the full 250000 items/t allowance");
        source.pushItemsToTargets(direction -> xianqiaoInterface);
        assertEquals(1_500_000L, storage.amount,
                "a second pass in the same game tick must not replenish any face budget");
    }

    @Test
    void bypassPushOffersOneOverstackWhileNormalPushKeepsAProtocolLegalStack() {
        SourceVeinBlockEntity normalSource = source(VeinKind.COBBLE, 7L, 250_000L);
        normalSource.setSideMode(Direction.EAST, SourceVeinBlockEntity.SourceSideMode.byId(2));
        OverstackAcceptingHandler normalTarget = new OverstackAcceptingHandler();

        normalSource.pushItemsToHandler(Direction.EAST, normalTarget, new ItemStack(Items.COBBLESTONE));

        assertEquals(64, normalTarget.simulatedCount,
                "ordinary PUSH must never manufacture an ItemStack above the item's legal stack size");
        assertEquals(64, normalTarget.executedCount);
        assertEquals(1, normalTarget.executeCalls);

        SourceVeinBlockEntity bypassSource = source(VeinKind.COBBLE, 7L, 250_000L);
        bypassSource.setSideMode(Direction.EAST, SourceVeinBlockEntity.SourceSideMode.byId(3));
        OverstackAcceptingHandler bypassTarget = new OverstackAcceptingHandler();

        bypassSource.pushItemsToHandler(Direction.EAST, bypassTarget, new ItemStack(Items.COBBLESTONE));

        assertEquals(250_000, bypassTarget.simulatedCount,
                "BYPASS_PUSH must offer the face's complete allowance in one simulation call");
        assertEquals(250_000, bypassTarget.executedCount,
                "an overstack-capable one-slot target must receive the full allowance in one execute call");
        assertEquals(1, bypassTarget.executeCalls);
    }

    @Test
    void oneBrokenSimulationFaceDoesNotCrashOrSuppressLaterFaces() {
        SourceVeinBlockEntity source = source(VeinKind.COBBLE, 7L, 32L);
        source.setActiveOutput(true);
        ItemStackHandler broken = new ItemStackHandler(1) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (simulate) throw new IllegalStateException("broken simulation");
                return super.insertItem(slot, stack, false);
            }
        };
        ItemStackHandler east = new ItemStackHandler(1);

        source.pushItemsToTargets(direction -> switch (direction) {
            case DOWN -> broken;
            case EAST -> east;
            default -> null;
        });

        assertEquals(32, east.getStackInSlot(0).getCount());
    }

    @Test
    void bypassSimulationFailureSkipsOnlyThatFaceWithoutPersistingAFault() throws Exception {
        SourceVeinBlockEntity source = source(VeinKind.COBBLE, 7L, 32L);
        source.setSideMode(Direction.DOWN, SourceVeinBlockEntity.SourceSideMode.BYPASS_PUSH);
        source.setSideMode(Direction.EAST, SourceVeinBlockEntity.SourceSideMode.BYPASS_PUSH);
        ItemStackHandler broken = new ItemStackHandler(1) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (simulate) throw new IllegalStateException("simulation is unavailable");
                return stack;
            }
        };
        ItemStackHandler east = new ItemStackHandler(1);

        source.pushItemsToTargets(direction -> switch (direction) {
            case DOWN -> broken;
            case EAST -> east;
            default -> null;
        });

        assertEquals(32, east.getStackInSlot(0).getCount(),
                "a failed BYPASS simulation must not freeze unrelated physical faces");
        assertFalse(faceFaulted(source, Direction.DOWN),
                "simulation has no unknown committed state and must not become a persistent fault");
        assertEquals(0L, uncertainInFlight(source, Direction.DOWN));
    }

    @Test
    void unknownExecuteFaultsOnlyItsFacePersistsTheStagedAmountAndClearsOnModeChange() throws Exception {
        SourceVeinBlockEntity source = source(VeinKind.STONE, 128L, 32L);
        source.setSideMode(Direction.DOWN, SourceVeinBlockEntity.SourceSideMode.BYPASS_PUSH);
        source.setSideMode(Direction.EAST, SourceVeinBlockEntity.SourceSideMode.BYPASS_PUSH);
        IItemHandler broken = new ItemStackHandler(1) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (simulate) return ItemStack.EMPTY;
                throw new IllegalStateException("commit result is unknown");
            }
        };
        ItemStackHandler east = new ItemStackHandler(1);

        source.pushItemsToTargets(direction -> switch (direction) {
            case DOWN -> broken;
            case EAST -> east;
            default -> null;
        });

        assertTrue(faceFaulted(source, Direction.DOWN));
        assertEquals(32L, uncertainInFlight(source, Direction.DOWN));
        assertFalse(faceFaulted(source, Direction.EAST));
        assertEquals(32, east.getStackInSlot(0).getCount(),
                "an indeterminate commit on DOWN must not suppress EAST");

        SourceVeinBlockEntity reloaded = source(VeinKind.STONE, 0L, 1L);
        reloaded.loadAdditional(save(source), registries);
        assertTrue(faceFaulted(reloaded, Direction.DOWN));
        assertEquals(32L, uncertainInFlight(reloaded, Direction.DOWN));
        assertFalse(faceFaulted(reloaded, Direction.EAST));

        reloaded.setSideMode(Direction.DOWN, SourceVeinBlockEntity.SourceSideMode.PUSH);
        assertFalse(faceFaulted(reloaded, Direction.DOWN));
        assertEquals(0L, uncertainInFlight(reloaded, Direction.DOWN));
    }

    @Test
    void activeAndPassiveItemOutputShareTheSamePhysicalFaceBudget() throws Exception {
        SourceVeinBlockEntity activeFirst = source(VeinKind.COBBLE, 7L, 64L);
        activeFirst.setSideMode(Direction.EAST, SourceVeinBlockEntity.SourceSideMode.PUSH);
        ItemStackHandler target = new ItemStackHandler(1);
        activeFirst.pushItemsToHandler(Direction.EAST, target, new ItemStack(Items.COBBLESTONE));
        IItemHandler eastCapability = sidedItemHandler(activeFirst, Direction.EAST);

        assertEquals(64, target.getStackInSlot(0).getCount());
        assertTrue(eastCapability.extractItem(0, 64, false).isEmpty(),
                "a passive pull must not receive a second copy of an already spent face allowance");

        SourceVeinBlockEntity passiveFirst = source(VeinKind.COBBLE, 7L, 64L);
        passiveFirst.setSideMode(Direction.EAST, SourceVeinBlockEntity.SourceSideMode.PUSH);
        IItemHandler passiveFirstCapability = sidedItemHandler(passiveFirst, Direction.EAST);
        assertEquals(64, passiveFirstCapability.extractItem(0, 64, false).getCount());
        ItemStackHandler secondTarget = new ItemStackHandler(1);
        passiveFirst.pushItemsToHandler(Direction.EAST, secondTarget, new ItemStack(Items.COBBLESTONE));
        assertTrue(secondTarget.getStackInSlot(0).isEmpty(),
                "active PUSH must observe the allowance already spent through the same side capability");
    }

    @Test
    void activeAndPassiveFluidOutputShareTheSamePhysicalFaceBudget() throws Exception {
        SourceVeinBlockEntity source = source(VeinKind.WATER, 7L, 1_000L);
        source.setSideMode(Direction.NORTH, SourceVeinBlockEntity.SourceSideMode.PUSH);
        FluidTank target = new FluidTank(2_000);
        source.pushFluidToHandler(Direction.NORTH, target, Fluids.WATER);

        net.neoforged.neoforge.fluids.capability.IFluidHandler northCapability =
                sidedFluidHandler(source, Direction.NORTH);
        assertEquals(1_000, target.getFluidAmount());
        assertTrue(northCapability.drain(1_000,
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).isEmpty());
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

    private static CompoundTag save(SourceVeinBlockEntity source) {
        CompoundTag tag = new CompoundTag();
        source.saveAdditional(tag, registries);
        return tag;
    }

    private static boolean faceFaulted(SourceVeinBlockEntity source, Direction side) throws Exception {
        Method method = SourceVeinBlockEntity.class.getMethod("isFaceFaulted", Direction.class);
        return (boolean) method.invoke(source, side);
    }

    private static long uncertainInFlight(SourceVeinBlockEntity source, Direction side) throws Exception {
        Method method = SourceVeinBlockEntity.class.getMethod("uncertainInFlight", Direction.class);
        return ((Number) method.invoke(source, side)).longValue();
    }

    private static IItemHandler sidedItemHandler(SourceVeinBlockEntity source, Direction side) throws Exception {
        Method method = SourceVeinBlockEntity.class.getMethod("getItemHandler", Direction.class);
        return (IItemHandler) method.invoke(source, side);
    }

    private static net.neoforged.neoforge.fluids.capability.IFluidHandler sidedFluidHandler(
            SourceVeinBlockEntity source, Direction side) throws Exception {
        Method method = SourceVeinBlockEntity.class.getMethod("getFluidHandler", Direction.class);
        return (net.neoforged.neoforge.fluids.capability.IFluidHandler) method.invoke(source, side);
    }

    private static final class CountingTerminalStorage implements TerminalItemStorage {
        private long amount;

        @Override public long revision() { return amount; }
        @Override public List<StorageItemSummary> snapshot() { return List.of(); }

        @Override
        public long insert(TerminalEntryKey key, long offered, TerminalStorageAction action) {
            if (action.executes()) amount += offered;
            return offered;
        }

        @Override
        public long extract(TerminalEntryKey key, long requested, TerminalStorageAction action) {
            return 0L;
        }
    }

    /** Models AE2's condenser-style slot: it deliberately accepts the full int-sized count. */
    private static final class OverstackAcceptingHandler implements IItemHandler {
        private int simulatedCount;
        private int executedCount;
        private int executeCalls;

        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (simulate) {
                simulatedCount += stack.getCount();
            } else {
                executedCount += stack.getCount();
                executeCalls++;
            }
            return ItemStack.EMPTY;
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return Integer.MAX_VALUE; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
    }
}
