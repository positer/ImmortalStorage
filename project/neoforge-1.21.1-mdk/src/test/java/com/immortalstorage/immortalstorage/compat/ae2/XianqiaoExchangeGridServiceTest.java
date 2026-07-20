package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.implementations.blockentities.IChestOrDrive;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.StorageCell;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class XianqiaoExchangeGridServiceTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void oneWrapperPerOwnerWinsWithinAGridAndAnotherOwnerStillWorks() throws Exception {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        XianqiaoExchangeStorageCell first = cell(owner,
                "00000000-0000-0000-0000-000000000001");
        XianqiaoExchangeStorageCell duplicate = cell(owner,
                "00000000-0000-0000-0000-000000000002");
        XianqiaoExchangeStorageCell otherOwner = cell(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "00000000-0000-0000-0000-000000000003");
        AtomicReference<List<StorageCell>> mounted =
                new AtomicReference<>(List.of(duplicate, first, otherOwner));
        AtomicInteger invalidations = new AtomicInteger();

        IChestOrDrive drive = proxy(IChestOrDrive.class, (method, arguments) -> switch (method) {
            case "getCellCount" -> mounted.get().size();
            case "getOriginalCellInventory" -> mounted.get().get((int) arguments.get(0));
            default -> defaultValue(arguments.returnType());
        });
        IGridNode driveNode = activeNode(drive);
        IStorageService storage = proxy(IStorageService.class, (method, arguments) -> {
            if (method.equals("invalidateCache")) invalidations.incrementAndGet();
            return defaultValue(arguments.returnType());
        });
        IGrid grid = proxy(IGrid.class, (method, arguments) -> switch (method) {
            case "getNodes" -> List.of(driveNode);
            case "getStorageService" -> storage;
            default -> defaultValue(arguments.returnType());
        });

        XianqiaoExchangeGridService service = new XianqiaoExchangeGridService(grid);
        service.onServerStartTick();

        assertEquals(CellState.EMPTY, first.getStatus(), "lowest stable disk id wins");
        assertEquals(CellState.FULL, duplicate.getStatus(), "same-owner duplicate is inert");
        assertEquals(CellState.EMPTY, otherOwner.getStatus(), "another owner gets its own winner");
        assertEquals(true, physicalDriveAllowsExtraction(),
                "an inactive duplicate must remain removable from the physical Drive");
        assertEquals(1, invalidations.get());

        service.onServerStartTick();
        assertEquals(1, invalidations.get(), "unchanged grids do not churn AE2's cache");

        mounted.set(List.of(duplicate, otherOwner));
        service.removeNode(null);
        service.onServerStartTick();
        assertEquals(CellState.FULL, first.getStatus());
        assertEquals(CellState.EMPTY, duplicate.getStatus(), "remaining duplicate takes over");
        assertEquals(2, invalidations.get());
    }

    @Test
    void theSameOwnerCanMountOnceOnEachIndependentGrid() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000011");
        XianqiaoExchangeStorageCell firstGridCell = cell(owner,
                "00000000-0000-0000-0000-000000000012");
        XianqiaoExchangeStorageCell secondGridCell = cell(owner,
                "00000000-0000-0000-0000-000000000013");

        XianqiaoExchangeGridService firstGrid = serviceFor(List.of(firstGridCell));
        XianqiaoExchangeGridService secondGrid = serviceFor(List.of(secondGridCell));
        firstGrid.onServerStartTick();
        secondGrid.onServerStartTick();

        assertEquals(CellState.EMPTY, firstGridCell.getStatus());
        assertEquals(CellState.EMPTY, secondGridCell.getStatus());
    }

    @Test
    void discoversARealDriveThroughItsGridNodeOwnerInsteadOfAnInterfaceClassLookup() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000021");
        XianqiaoExchangeStorageCell mountedCell = cell(owner,
                "00000000-0000-0000-0000-000000000022");
        AtomicInteger invalidations = new AtomicInteger();

        IChestOrDrive drive = proxy(IChestOrDrive.class, (method, arguments) -> switch (method) {
            case "getCellCount" -> 1;
            case "getOriginalCellInventory" -> mountedCell;
            default -> defaultValue(arguments.returnType());
        });
        IGridNode driveNode = proxy(IGridNode.class, (method, arguments) -> switch (method) {
            case "getOwner" -> drive;
            case "isActive" -> true;
            default -> defaultValue(arguments.returnType());
        });
        IStorageService storage = proxy(IStorageService.class, (method, arguments) -> {
            if (method.equals("invalidateCache")) invalidations.incrementAndGet();
            return defaultValue(arguments.returnType());
        });
        IGrid grid = proxy(IGrid.class, (method, arguments) -> switch (method) {
            case "getNodes" -> List.of(driveNode);
            case "getActiveMachines" -> Set.of();
            case "getStorageService" -> storage;
            default -> defaultValue(arguments.returnType());
        });

        new XianqiaoExchangeGridService(grid).onServerStartTick();

        assertEquals(CellState.EMPTY, mountedCell.getStatus(),
                "AE2 indexes machines by the owner's concrete class, so an interface lookup must not be used");
        assertEquals(1, invalidations.get());
    }

    @Test
    void infiniteSentinelUsesTheIntCompatibleAe2DisplayCeiling() {
        AEItemKey diamond = AEItemKey.of(Items.DIAMOND);
        KeyCounter counter = new KeyCounter();

        XianqiaoExchangeStorageCell.addSaturated(counter, diamond, Long.MAX_VALUE);
        assertEquals((long) Integer.MAX_VALUE, counter.get(diamond));

        counter.set(diamond, Long.MAX_VALUE - 5L);
        XianqiaoExchangeStorageCell.addSaturated(counter, diamond, 10L);
        assertEquals(Long.MAX_VALUE, counter.get(diamond), "finite addition must saturate, never wrap");
    }

    private static XianqiaoExchangeStorageCell cell(UUID owner, String diskId) {
        return new XianqiaoExchangeStorageCell(
                owner, UUID.fromString(diskId), Component.literal("test"));
    }

    private static XianqiaoExchangeGridService serviceFor(List<StorageCell> mounted) {
        IChestOrDrive drive = proxy(IChestOrDrive.class, (method, arguments) -> switch (method) {
            case "getCellCount" -> mounted.size();
            case "getOriginalCellInventory" -> mounted.get((int) arguments.get(0));
            default -> defaultValue(arguments.returnType());
        });
        IStorageService storage = proxy(IStorageService.class,
                (method, arguments) -> defaultValue(arguments.returnType()));
        IGridNode driveNode = activeNode(drive);
        IGrid grid = proxy(IGrid.class, (method, arguments) -> switch (method) {
            case "getNodes" -> List.of(driveNode);
            case "getStorageService" -> storage;
            default -> defaultValue(arguments.returnType());
        });
        return new XianqiaoExchangeGridService(grid);
    }

    private static IGridNode activeNode(IChestOrDrive drive) {
        return proxy(IGridNode.class, (method, arguments) -> switch (method) {
            case "getOwner" -> drive;
            case "isActive" -> true;
            default -> defaultValue(arguments.returnType());
        });
    }

    private static boolean physicalDriveAllowsExtraction() throws Exception {
        Class<?> type = Class.forName(
                "appeng.blockentity.storage.DriveBlockEntity$CellValidInventoryFilter");
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object filter = constructor.newInstance();
        Method allowExtract = type.getDeclaredMethod(
                "allowExtract", appeng.api.inventories.InternalInventory.class, int.class, int.class);
        allowExtract.setAccessible(true);
        return (boolean) allowExtract.invoke(filter, null, 0, 1);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, ProxyHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == arguments[0];
                            case "toString" -> "test-" + type.getSimpleName();
                            default -> null;
                        };
                    }
                    return handler.invoke(method.getName(),
                            new InvocationArguments(method.getReturnType(),
                                    arguments == null ? new Object[0] : arguments));
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }

    @FunctionalInterface
    private interface ProxyHandler {
        Object invoke(String method, InvocationArguments arguments) throws Throwable;
    }

    private record InvocationArguments(Class<?> returnType, Object[] values) {
        Object get(int index) {
            return values[index];
        }
    }
}
