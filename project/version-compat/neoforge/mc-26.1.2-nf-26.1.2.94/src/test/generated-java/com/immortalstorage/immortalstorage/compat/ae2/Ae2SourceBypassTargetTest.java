package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Ae2SourceBypassTargetTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void nativeMeStoragePathPreservesTheFullLongRequestAndAction() throws Exception {
        Class<?> adapter = Class.forName(
                "com.immortalstorage.immortalstorage.compat.ae2.Ae2SourceBypassTarget");
        Method insert = adapter.getDeclaredMethod(
                "insertItem", MEStorage.class, ItemStack.class, long.class, boolean.class);
        insert.setAccessible(true);
        RecordingStorage storage = new RecordingStorage();
        ItemStack prototype = new ItemStack(Items.COBBLESTONE);

        long simulated = ((Number) insert.invoke(null, storage, prototype, 250_000L, true)).longValue();
        assertEquals(250_000L, simulated);
        assertEquals(250_000L, storage.lastAmount);
        assertEquals(Actionable.SIMULATE, storage.lastAction);
        assertTrue(storage.lastKey instanceof AEItemKey itemKey && itemKey.matches(prototype));
        assertFalse(storage.lastSource.player().isPresent());

        long executed = ((Number) insert.invoke(null, storage, prototype, 250_000L, false)).longValue();
        assertEquals(250_000L, executed);
        assertEquals(Actionable.MODULATE, storage.lastAction);
    }

    @Test
    void nativeFluidPathBypassesTheOneBucketCapabilityBoundary() throws Exception {
        Class<?> adapter = Class.forName(
                "com.immortalstorage.immortalstorage.compat.ae2.Ae2SourceBypassTarget");
        Method insert = adapter.getDeclaredMethod(
                "insertFluid", MEStorage.class, FluidStack.class, long.class, boolean.class);
        insert.setAccessible(true);
        RecordingStorage storage = new RecordingStorage();
        FluidStack prototype = new FluidStack(Fluids.WATER, 1);

        long simulated = ((Number) insert.invoke(null, storage, prototype, 2_500_000L, true)).longValue();
        assertEquals(2_500_000L, simulated);
        assertEquals(2_500_000L, storage.lastAmount);
        assertEquals(Actionable.SIMULATE, storage.lastAction);
        assertTrue(storage.lastKey instanceof AEFluidKey fluidKey && fluidKey.matches(prototype));

        long executed = ((Number) insert.invoke(null, storage, prototype, 2_500_000L, false)).longValue();
        assertEquals(2_500_000L, executed);
        assertEquals(Actionable.MODULATE, storage.lastAction);
    }

    @Test
    void impossibleAe2AcceptanceFailsClosed() throws Exception {
        Class<?> adapter = Class.forName(
                "com.immortalstorage.immortalstorage.compat.ae2.Ae2SourceBypassTarget");
        Method insert = adapter.getDeclaredMethod(
                "insertItem", MEStorage.class, ItemStack.class, long.class, boolean.class);
        insert.setAccessible(true);
        RecordingStorage storage = new RecordingStorage() {
            @Override
            public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
                super.insert(what, amount, mode, source);
                return amount + 1L;
            }
        };

        assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> insert.invoke(null, storage, new ItemStack(Items.STONE), 64L, true));
    }

    @Test
    void condenserKeepsNativeItemsButFallsBackForExactFluidSemantics() throws Exception {
        Class<?> condenserStorageClass = Class.forName("appeng.blockentity.misc.CondenserMEStorage");
        MEStorage condenserStorage = (MEStorage) unsafe().allocateInstance(condenserStorageClass);
        Class<?> adapterClass = Class.forName(
                "com.immortalstorage.immortalstorage.compat.ae2.Ae2SourceBypassTarget");
        Constructor<?> constructor = adapterClass.getDeclaredConstructor(MEStorage.class);
        constructor.setAccessible(true);
        Object condenserAdapter = constructor.newInstance(condenserStorage);
        Object ordinaryAdapter = constructor.newInstance(new RecordingStorage());

        assertTrue(((com.immortalstorage.immortalstorage.api.source.SourceBypassTransferTarget) condenserAdapter)
                .supportsItems());
        assertFalse(((com.immortalstorage.immortalstorage.api.source.SourceBypassTransferTarget) condenserAdapter)
                        .supportsFluids(),
                "CondenserMEStorage truncates non-125mB conversion progress and must use its exact FluidHandler");
        assertTrue(((com.immortalstorage.immortalstorage.api.source.SourceBypassTransferTarget) ordinaryAdapter)
                .supportsFluids());
    }

    private static Unsafe unsafe() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static class RecordingStorage implements MEStorage {
        private AEKey lastKey;
        private long lastAmount;
        private Actionable lastAction;
        private IActionSource lastSource;

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            this.lastKey = what;
            this.lastAmount = amount;
            this.lastAction = mode;
            this.lastSource = source;
            return amount;
        }

        @Override
        public Component getDescription() {
            return Component.literal("recording ME storage");
        }
    }
}
