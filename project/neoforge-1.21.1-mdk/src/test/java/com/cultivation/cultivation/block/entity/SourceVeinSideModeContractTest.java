package com.cultivation.cultivation.block.entity;

import com.cultivation.cultivation.block.custom.VeinKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceVeinSideModeContractTest {
    private static RegistryAccess.Frozen registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void sideModesExposeOnlyTheThreeOutputStatesAndDecodeStablePersistenceIds() {
        Set<String> names = Arrays.stream(SourceVeinBlockEntity.SourceSideMode.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(Set.of("DISABLED", "PUSH", "BYPASS_PUSH"), names);
        assertEquals("DISABLED", SourceVeinBlockEntity.SourceSideMode.byId(0).name());
        assertEquals("DISABLED", SourceVeinBlockEntity.SourceSideMode.byId(1).name(),
                "legacy EXTRACT id 1 must fail closed instead of becoming output");
        assertEquals("PUSH", SourceVeinBlockEntity.SourceSideMode.byId(2).name());
        assertEquals("BYPASS_PUSH", SourceVeinBlockEntity.SourceSideMode.byId(3).name());
    }

    @Test
    void legacyNbtNeverEscalatesExtractToOutputOrPushToBypass() {
        SourceVeinBlockEntity source = source(BlockPos.ZERO);
        CompoundTag legacy = new CompoundTag();
        legacy.putString("Kind", VeinKind.COBBLE.name());
        legacy.putLong("FluxLimit", 73L);
        legacy.putIntArray("SideModes", new int[] {1, 2, 0, 1, 2, 0});

        source.loadAdditional(legacy, registries);

        assertEquals("DISABLED", source.getSideMode(Direction.DOWN).name());
        assertEquals("PUSH", source.getSideMode(Direction.UP).name(),
                "legacy PUSH id 2 must remain ordinary PUSH");
        assertEquals("DISABLED", source.getSideMode(Direction.SOUTH).name());
        assertEquals("PUSH", source.getSideMode(Direction.WEST).name());

        CompoundTag migrated = save(source);
        assertTrue(migrated.contains("SideModesVersion"),
                "rewritten NBT must identify the explicit side-mode id schema");
        int[] migratedModes = migrated.getIntArray("SideModes");
        assertEquals(0, migratedModes[Direction.DOWN.ordinal()]);
        assertEquals(2, migratedModes[Direction.UP.ordinal()]);
    }

    @Test
    void separateBlocksKeepIndependentRatesAndModesAcrossPersistence() {
        SourceVeinBlockEntity first = source(new BlockPos(1, 2, 3));
        SourceVeinBlockEntity second = source(new BlockPos(4, 5, 6));
        first.setFluxLimit(7L);
        second.setFluxLimit(19L);
        first.setSideMode(Direction.NORTH, SourceVeinBlockEntity.SourceSideMode.byId(2));
        second.setSideMode(Direction.NORTH, SourceVeinBlockEntity.SourceSideMode.byId(3));

        first.setFluxLimit(11L);
        first.setSideMode(Direction.NORTH, SourceVeinBlockEntity.SourceSideMode.byId(0));

        assertEquals(19L, second.getFluxLimit(), "one block's editor must not mutate another block's rate");
        assertEquals("BYPASS_PUSH", second.getSideMode(Direction.NORTH).name(),
                "one block's face change must not mutate another block's mode");

        SourceVeinBlockEntity reloadedFirst = source(new BlockPos(1, 2, 3));
        SourceVeinBlockEntity reloadedSecond = source(new BlockPos(4, 5, 6));
        reloadedFirst.loadAdditional(save(first), registries);
        reloadedSecond.loadAdditional(save(second), registries);

        assertEquals(11L, reloadedFirst.getFluxLimit());
        assertEquals("DISABLED", reloadedFirst.getSideMode(Direction.NORTH).name());
        assertEquals(19L, reloadedSecond.getFluxLimit());
        assertEquals("BYPASS_PUSH", reloadedSecond.getSideMode(Direction.NORTH).name());
    }

    @Test
    void legacyGlobalFaultMigratesToEveryPreviouslyActivePhysicalFace() throws Exception {
        SourceVeinBlockEntity source = source(BlockPos.ZERO);
        CompoundTag legacy = new CompoundTag();
        legacy.putString("Kind", VeinKind.COBBLE.name());
        legacy.putBoolean("OutputFaulted", true);
        legacy.putIntArray("SideModes", new int[] {2, 0, 3, 0, 2, 0});

        source.loadAdditional(legacy, registries);

        assertEquals(true, faceFaulted(source, Direction.DOWN));
        assertEquals(false, faceFaulted(source, Direction.UP));
        assertEquals(true, faceFaulted(source, Direction.NORTH));
        assertEquals(true, faceFaulted(source, Direction.WEST));
        assertEquals(0L, uncertainInFlight(source, Direction.DOWN));

        CompoundTag migrated = save(source);
        assertTrue(migrated.contains("FaceFaultsVersion"));
        assertTrue(migrated.contains("FaceFaults"));
        assertTrue(migrated.contains("FaceInFlight"));
    }

    private static SourceVeinBlockEntity source(BlockPos pos) {
        return new SourceVeinBlockEntity(
                BlockEntityType.FURNACE, pos, Blocks.FURNACE.defaultBlockState(), VeinKind.COBBLE);
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
}
