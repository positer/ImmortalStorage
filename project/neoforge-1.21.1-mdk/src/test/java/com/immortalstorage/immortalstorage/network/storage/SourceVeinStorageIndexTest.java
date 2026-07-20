package com.immortalstorage.immortalstorage.network.storage;

import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceVeinStorageIndexTest {
    private static final UUID OWNER = UUID.fromString("d9502512-b1a6-4b9e-92c1-48158a60b2ed");

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void loadedItemSourcesAreExplicitVirtualEntriesAndShareTheRealPaidCache() {
        SourceVeinStorageIndex.OwnerDirectory directory =
                new SourceVeinStorageIndex.OwnerDirectory(OWNER);
        FakeSource paid = FakeSource.item("realm/stone", OWNER, new ItemStack(Items.STONE), false, 320L);

        assertTrue(directory.register(paid));
        long placementRevision = directory.itemRevision();
        List<StorageItemSummary> snapshot = directory.itemSnapshot();

        assertEquals(1, snapshot.size());
        assertTrue(ItemStack.isSameItemSameComponents(new ItemStack(Items.STONE), snapshot.getFirst().prototype()));
        assertEquals(320L, snapshot.getFirst().amount());
        TerminalEntryKey stone = TerminalEntryKey.of(new ItemStack(Items.STONE));
        assertEquals(64L, directory.extractItem(stone, 64L, TerminalStorageAction.SIMULATE));
        assertEquals(320L, paid.available, "simulation must read but not consume the source cache");
        assertEquals(64L, directory.extractItem(stone, 64L, TerminalStorageAction.EXECUTE));
        assertEquals(256L, paid.available, "terminal/API extraction must consume the same persistent cache");
        assertTrue(directory.itemRevision() > placementRevision);
    }

    @Test
    void freeSourcesReportIntMaxWithoutMaterializingStacksOrChangingOnExtraction() {
        SourceVeinStorageIndex.OwnerDirectory directory =
                new SourceVeinStorageIndex.OwnerDirectory(OWNER);
        FakeSource free = FakeSource.item("realm/cobble", OWNER, new ItemStack(Items.COBBLESTONE), true,
                Long.MAX_VALUE);
        directory.register(free);

        StorageItemSummary entry = directory.itemSnapshot().getFirst();
        assertEquals(Long.MAX_VALUE, entry.amount());
        assertEquals(1, entry.prototype().getCount(), "the directory must retain only a one-count prototype");
        long revision = directory.itemRevision();
        TerminalEntryKey cobble = TerminalEntryKey.of(new ItemStack(Items.COBBLESTONE));
        assertEquals(1_000_000L,
                directory.extractItem(cobble, 1_000_000L, TerminalStorageAction.EXECUTE));
        assertEquals(Long.MAX_VALUE, free.available);
        assertEquals(revision, directory.itemRevision(), "unchanged creative amounts need no false invalidation");

        directory.register(FakeSource.item("realm/cobble-second", OWNER,
                new ItemStack(Items.COBBLESTONE), true, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, directory.itemSnapshot().getFirst().amount(),
                "duplicate creative sources must not overflow or double the advertised entry");
        assertEquals(Long.MAX_VALUE,
                directory.extractItem(cobble, Long.MAX_VALUE, TerminalStorageAction.SIMULATE),
                "native extraction must remain bounded by the explicit directory amount");
    }

    @Test
    void identicalSourcesAggregateOnceAndPlacementRemovalInvalidateTheDirectory() {
        SourceVeinStorageIndex.OwnerDirectory directory =
                new SourceVeinStorageIndex.OwnerDirectory(OWNER);
        FakeSource first = FakeSource.item("realm/coal-a", OWNER, new ItemStack(Items.COAL), false, 32L);
        FakeSource second = FakeSource.item("realm/coal-b", OWNER, new ItemStack(Items.COAL), false, 64L);

        directory.register(first);
        long firstRevision = directory.itemRevision();
        directory.register(second);
        assertTrue(directory.itemRevision() > firstRevision);
        assertEquals(96L, directory.itemSnapshot().getFirst().amount());

        long beforeRemoval = directory.itemRevision();
        assertTrue(directory.unregister(second.sourceId()));
        assertTrue(directory.itemRevision() > beforeRemoval);
        assertEquals(32L, directory.itemSnapshot().getFirst().amount());
        assertFalse(directory.unregister("missing"));
    }

    @Test
    void fluidSourcesUseTheSameDirectoryAndInactiveOrForeignSourcesAreHidden() {
        SourceVeinStorageIndex.OwnerDirectory directory =
                new SourceVeinStorageIndex.OwnerDirectory(OWNER);
        FakeSource water = FakeSource.fluid("realm/water", OWNER,
                new FluidStack(Fluids.WATER, 1), true, Long.MAX_VALUE);
        FakeSource inactive = FakeSource.item("realm/inactive", OWNER,
                new ItemStack(Items.DIAMOND), false, 4L);
        inactive.active = false;
        FakeSource foreign = FakeSource.item("realm/foreign", UUID.randomUUID(),
                new ItemStack(Items.EMERALD), false, 4L);

        directory.register(water);
        directory.register(inactive);
        directory.register(foreign);

        assertTrue(directory.itemSnapshot().isEmpty());
        Map<TerminalFluidKey, Long> fluids = directory.fluidSnapshot();
        assertEquals(1, fluids.size());
        assertEquals(Long.MAX_VALUE, fluids.values().iterator().next());
        TerminalFluidKey waterKey = TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1));
        long revision = directory.fluidRevision();
        assertEquals(25_000L,
                directory.extractFluid(waterKey, 25_000L, TerminalStorageAction.EXECUTE));
        assertEquals(revision, directory.fluidRevision());
    }

    private static final class FakeSource implements SourceVeinStorageIndex.SourceHandle {
        private final String id;
        private final UUID owner;
        private final ItemStack item;
        private final FluidStack fluid;
        private final boolean free;
        private boolean active = true;
        private long available;

        private FakeSource(String id, UUID owner, ItemStack item, FluidStack fluid,
                           boolean free, long available) {
            this.id = id;
            this.owner = owner;
            this.item = item;
            this.fluid = fluid;
            this.free = free;
            this.available = available;
        }

        static FakeSource item(String id, UUID owner, ItemStack item, boolean free, long available) {
            return new FakeSource(id, owner, item.copyWithCount(1), FluidStack.EMPTY, free, available);
        }

        static FakeSource fluid(String id, UUID owner, FluidStack fluid, boolean free, long available) {
            return new FakeSource(id, owner, ItemStack.EMPTY, fluid.copyWithAmount(1), free, available);
        }

        @Override public String sourceId() { return id; }
        @Override public UUID owner() { return owner; }
        @Override public boolean activeFor(UUID requestedOwner) { return active && owner.equals(requestedOwner); }
        @Override public boolean fluid() { return !fluid.isEmpty(); }
        @Override public boolean free() { return free; }
        @Override public ItemStack itemPrototype() { return item.copy(); }
        @Override public FluidStack fluidPrototype() { return fluid.copyWithAmount(1); }
        @Override public long availableUnits() { return available; }

        @Override
        public long extract(long requested, boolean simulate) {
            if (requested <= 0L) return 0L;
            if (free) return requested;
            long extracted = Math.min(requested, available);
            if (!simulate) available -= extracted;
            return extracted;
        }
    }
}
