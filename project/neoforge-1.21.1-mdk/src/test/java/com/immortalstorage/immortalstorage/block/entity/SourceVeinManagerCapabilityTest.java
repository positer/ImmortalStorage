package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceVeinManagerCapabilityTest {
    private static RegistryAccess.Frozen registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void paidItemProductsAreEnumeratedAndExtractedFromThePersistedMemberCacheOnEveryFace() {
        SourceVeinManagerBlockEntity manager = manager();
        assertTrue(manager.members().insertItem(0, source(ModBlocks.STONE_VEIN.get().asItem(), 100L), false)
                .isEmpty());

        for (Direction side : Direction.values()) {
            IItemHandler handler = manager.getItemHandler(side);
            assertNotNull(handler, side + " must expose passive item extraction");
            assertEquals(1, handler.getSlots());
            assertEquals(100, handler.getStackInSlot(0).getCount(),
                    "the int directory view must publish the full finite cache");
            assertEquals(64, handler.extractItem(0, Integer.MAX_VALUE, true).getCount());
            assertEquals(100L, manager.memberCachedUnits(0), "simulation must not mutate the member cache");
        }

        IItemHandler north = manager.getItemHandler(Direction.NORTH);
        ItemStack offered = new ItemStack(Items.STONE, 8);
        assertEquals(8, north.insertItem(0, offered, false).getCount(), "the product view is extract-only");
        assertFalse(north.isItemValid(0, offered));
        assertEquals(64, north.extractItem(0, Integer.MAX_VALUE, false).getCount());
        assertEquals(36L, manager.memberCachedUnits(0),
                "execution must debit the same persisted cache used by the manager member");
        assertEquals(36, north.extractItem(0, Integer.MAX_VALUE, false).getCount());
        assertEquals(0L, manager.memberCachedUnits(0));
    }

    @Test
    void paidItemProductsSaturateOnlyTheIntDirectoryViewWithoutClampingTheLongCache() {
        SourceVeinManagerBlockEntity manager = manager();
        assertTrue(manager.members().insertItem(0,
                source(ModBlocks.STONE_VEIN.get().asItem(), Long.MAX_VALUE), false).isEmpty());

        IItemHandler handler = manager.getItemHandler(Direction.UP);
        assertNotNull(handler);
        assertEquals(Integer.MAX_VALUE, handler.getStackInSlot(0).getCount());
        assertEquals(64, handler.extractItem(0, Integer.MAX_VALUE, true).getCount(),
                "one IItemHandler extraction still returns one legal item stack");
        assertEquals(Long.MAX_VALUE, manager.memberCachedUnits(0));
    }

    @Test
    void freeItemProductsPublishIntCompatibilityAmountButReturnOnlyOneLegalStackPerCall() {
        SourceVeinManagerBlockEntity manager = manager();
        assertTrue(manager.members().insertItem(0,
                source(ModBlocks.COBBLESTONE_VEIN.get().asItem(), 1L), false).isEmpty());

        IItemHandler handler = manager.getItemHandler(Direction.DOWN);
        assertNotNull(handler);
        assertEquals(Integer.MAX_VALUE, handler.getStackInSlot(0).getCount());
        assertEquals(Integer.MAX_VALUE, handler.getSlotLimit(0));
        assertEquals(64, handler.extractItem(0, Integer.MAX_VALUE, true).getCount());
        assertEquals(64, handler.extractItem(0, Integer.MAX_VALUE, false).getCount());
        assertEquals(Long.MAX_VALUE, manager.memberCachedUnits(0),
                "executing a free-source extraction must leave its authoritative MAX cache unchanged");
    }

    @Test
    void fluidProductsAreExtractOnlyAndUseTheSameCreativeCacheOnEveryFace() {
        SourceVeinManagerBlockEntity manager = manager();
        assertTrue(manager.members().insertItem(0,
                source(ModBlocks.WATER_VEIN.get().asItem(), 7L), false).isEmpty());

        for (Direction side : Direction.values()) {
            IFluidHandler handler = manager.getFluidHandler(side);
            assertNotNull(handler, side + " must expose passive fluid extraction");
            assertEquals(1, handler.getTanks());
            assertEquals(Integer.MAX_VALUE, handler.getFluidInTank(0).getAmount());
            assertEquals(Integer.MAX_VALUE, handler.getTankCapacity(0));
            assertEquals(0, handler.fill(new FluidStack(Fluids.WATER, 1_000),
                    IFluidHandler.FluidAction.EXECUTE));
            assertFalse(handler.isFluidValid(0, new FluidStack(Fluids.WATER, 1)));
            assertTrue(handler.drain(new FluidStack(Fluids.LAVA, 1_000),
                    IFluidHandler.FluidAction.SIMULATE).isEmpty());
            assertEquals(Integer.MAX_VALUE,
                    handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE).getAmount());
        }

        IFluidHandler north = manager.getFluidHandler(Direction.NORTH);
        assertEquals(Integer.MAX_VALUE,
                north.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE).getAmount());
        assertEquals(Long.MAX_VALUE, manager.memberCachedUnits(0));
    }

    @Test
    void absentResourceChannelsAreNotPublished() {
        SourceVeinManagerBlockEntity empty = manager();
        for (Direction side : Direction.values()) {
            assertNull(empty.getItemHandler(side));
            assertNull(empty.getFluidHandler(side));
        }

        SourceVeinManagerBlockEntity itemOnly = manager();
        itemOnly.members().insertItem(0, source(ModBlocks.COBBLESTONE_VEIN.get().asItem(), 0L), false);
        assertNotNull(itemOnly.getItemHandler(Direction.UP));
        assertNull(itemOnly.getFluidHandler(Direction.UP));

        SourceVeinManagerBlockEntity fluidOnly = manager();
        fluidOnly.members().insertItem(0, source(ModBlocks.WATER_VEIN.get().asItem(), 0L), false);
        assertNull(fluidOnly.getItemHandler(Direction.UP));
        assertNotNull(fluidOnly.getFluidHandler(Direction.UP));
    }

    @Test
    void managerRegistersBothStandardCapabilitiesWithoutFaceModeGates() throws IOException {
        Path project = locateProjectRoot();
        String registrations = Files.readString(project.resolve(Path.of(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "block", "entity", "ModBlockEntities.java")));

        assertTrue(registrations.contains(
                "Capabilities.ItemHandler.BLOCK, SOURCE_VEIN_MANAGER.get()"));
        assertTrue(registrations.contains(
                "Capabilities.FluidHandler.BLOCK, SOURCE_VEIN_MANAGER.get()"));
        assertTrue(registrations.contains("be.getItemHandler(side)"));
        assertTrue(registrations.contains("be.getFluidHandler(side)"));

        String manager = Files.readString(project.resolve(Path.of(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "block", "entity", "SourceVeinManagerBlockEntity.java")));
        assertTrue(manager.contains("serverLevel.invalidateCapabilities(worldPosition)"),
                "member changes must invalidate cached null/type-specific capabilities for adjacent storage buses");
        assertTrue(manager.contains("manager.definitionsChanged()"),
                "definition reloads must invalidate cached item/fluid/null capabilities");
        assertTrue(manager.contains("SourceVeinStorageIndex.changed(this, itemChanged, fluidChanged)"),
                "amount-only changes must publish revisions without rebuilding member capabilities");
        assertTrue(manager.contains("if (level instanceof ServerLevel) displayState.refreshFrom(this)"),
                "client world reload must retain the server-sent display state instead of refreshing from empty private members");
    }

    private static SourceVeinManagerBlockEntity manager() {
        return new SourceVeinManagerBlockEntity(
                BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
    }

    private static ItemStack source(net.minecraft.world.item.Item item, long cachedUnits) {
        ItemStack source = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putLong("CachedUnits", cachedUnits);
        source.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return source;
    }

    private static Path locateProjectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("gradlew.bat"))) {
            current = current.getParent();
        }
        if (current == null) throw new IllegalStateException("Cannot locate Gradle project root");
        return current;
    }
}
