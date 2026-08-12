package com.immortalstorage.immortalstorage.block.entity;

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
import net.neoforged.neoforge.items.IItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RED contract for state that must belong to one placed interface, not to a player or client. */
final class XianqiaoInterfaceDirectionStateTest {
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
    void sixFacesPersistIndependentlyAndMissingLegacyStateDefaultsToDisabled() throws Exception {
        XianqiaoInterfaceBlockEntity original = entity();
        Class<? extends Enum<?>> modeType = sideModeType();

        for (Direction direction : Direction.values()) {
            assertEquals("DISABLED", modeName(getMode(original, modeType, direction)),
                    "new and legacy blocks without an explicit side-mode tag fail closed");
        }

        long before = configRevision(original);
        setMode(original, modeType, Direction.EAST, "PUSH");
        setMode(original, modeType, Direction.WEST, "PULL");
        setMode(original, modeType, Direction.UP, "DISABLED");
        long configuredRevision = configRevision(original);
        assertTrue(configuredRevision > before);

        CompoundTag saved = new CompoundTag();
        original.saveAdditionalLegacy(saved, registries);
        XianqiaoInterfaceBlockEntity restored = entity();
        restored.loadAdditionalLegacy(saved, registries);

        assertEquals("PUSH", modeName(getMode(restored, modeType, Direction.EAST)));
        assertEquals("PULL", modeName(getMode(restored, modeType, Direction.WEST)));
        assertEquals("DISABLED", modeName(getMode(restored, modeType, Direction.UP)));
        assertEquals("DISABLED", modeName(getMode(restored, modeType, Direction.DOWN)));
        assertEquals(configuredRevision, configRevision(restored));
    }

    @Test
    void twoPlacedInterfacesDoNotShareModesOrConfigurationRevision() throws Exception {
        XianqiaoInterfaceBlockEntity first = entity();
        XianqiaoInterfaceBlockEntity second = entity();
        Class<? extends Enum<?>> modeType = sideModeType();

        setMode(first, modeType, Direction.NORTH, "PUSH");

        assertEquals("PUSH", modeName(getMode(first, modeType, Direction.NORTH)));
        assertEquals("DISABLED", modeName(getMode(second, modeType, Direction.NORTH)));
        assertTrue(configRevision(first) > configRevision(second));
    }

    @Test
    void pullFaceKeepsTheBulkInsertFastPathAndUnsidedAccessCannotBypassModes() throws Exception {
        XianqiaoInterfaceBlockEntity blockEntity = entity();
        Class<? extends Enum<?>> modeType = sideModeType();
        Method handlerMethod = XianqiaoInterfaceBlockEntity.class
                .getDeclaredMethod("getItemHandler", Direction.class);
        handlerMethod.setAccessible(true);

        Object pull = handlerMethod.invoke(blockEntity, Direction.EAST);
        assertNotNull(pull);
        assertInstanceOf(BulkItemInsertTarget.class, pull,
                "source veins must not fall back to one normal stack per capability call");

        setMode(blockEntity, modeType, Direction.EAST, "DISABLED");
        BulkItemInsertTarget previouslyAcquired = (BulkItemInsertTarget) pull;
        assertEquals(0L, previouslyAcquired.insertBulk(
                new ItemStack(Items.COBBLESTONE), 250_000L, false),
                "no configured cobblestone cache exists; the active face mode is irrelevant");

        Object unsided = handlerMethod.invoke(blockEntity, new Object[] {null});
        if (unsided instanceof IItemHandler handler) {
            assertEquals(0, handler.getSlots(), "an unbound interface has no live cache");
            ItemStack offered = new ItemStack(Items.COBBLESTONE, 8);
            assertEquals(8, handler.insertItem(0, offered, false).getCount());
            assertTrue(handler.extractItem(0, 8, false).isEmpty());
        }
    }

    private static XianqiaoInterfaceBlockEntity entity() {
        return new XianqiaoInterfaceBlockEntity(
                BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum<?>> sideModeType() {
        return (Class<? extends Enum<?>>) Arrays.stream(XianqiaoInterfaceBlockEntity.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("setSideMode"))
                .filter(method -> method.getParameterCount() == 2
                        && method.getParameterTypes()[0] == Direction.class)
                .map(method -> method.getParameterTypes()[1])
                .filter(Class::isEnum)
                .filter(type -> hasEnumConstant(type, "PUSH")
                        && hasEnumConstant(type, "PULL")
                        && hasEnumConstant(type, "DISABLED"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Xianqiao Interface must declare PUSH/PULL/DISABLED side modes"));
    }

    private static boolean hasEnumConstant(Class<?> type, String expected) {
        return Arrays.stream(type.getEnumConstants())
                .map(constant -> ((Enum<?>) constant).name())
                .anyMatch(expected::equals);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumConstant(Class<? extends Enum<?>> type, String name) {
        return Enum.valueOf((Class) type, name);
    }

    private static void setMode(XianqiaoInterfaceBlockEntity entity,
                                Class<? extends Enum<?>> type,
                                Direction direction, String mode) throws Exception {
        Method setter = XianqiaoInterfaceBlockEntity.class
                .getDeclaredMethod("setSideMode", Direction.class, type);
        setter.setAccessible(true);
        setter.invoke(entity, direction, enumConstant(type, mode));
    }

    private static Object getMode(XianqiaoInterfaceBlockEntity entity,
                                  Class<? extends Enum<?>> type,
                                  Direction direction) throws Exception {
        Method getter = XianqiaoInterfaceBlockEntity.class
                .getDeclaredMethod("getSideMode", Direction.class);
        getter.setAccessible(true);
        Object result = getter.invoke(entity, direction);
        assertTrue(type.isInstance(result));
        return result;
    }

    private static long configRevision(XianqiaoInterfaceBlockEntity entity) throws Exception {
        Method getter = XianqiaoInterfaceBlockEntity.class.getDeclaredMethod("getConfigRevision");
        getter.setAccessible(true);
        return ((Number) getter.invoke(entity)).longValue();
    }

    private static String modeName(Object mode) {
        return ((Enum<?>) mode).name();
    }
}
