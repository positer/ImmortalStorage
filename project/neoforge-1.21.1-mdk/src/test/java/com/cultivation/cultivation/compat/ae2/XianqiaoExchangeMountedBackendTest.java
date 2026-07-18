package com.cultivation.cultivation.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.implementations.blockentities.IChestOrDrive;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import com.cultivation.cultivation.api.storage.PersonalStorageEndpoint;
import com.cultivation.cultivation.api.storage.terminal.TerminalEntryKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalFluidKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalFluidStorage;
import com.cultivation.cultivation.api.storage.terminal.TerminalItemStorage;
import com.cultivation.cultivation.api.storage.terminal.TerminalStorageAction;
import com.cultivation.cultivation.network.storage.PersonalStorageFluidHandler;
import com.cultivation.cultivation.network.storage.PersonalStorageLongItemStorage;
import com.cultivation.cultivation.player.CultivationPlayerData;
import com.cultivation.cultivation.item.ModItems;
import com.cultivation.cultivation.item.custom.XianqiaoExchangeCellItem;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end adapter proof from one mounted AE2 cell to the real player-data storage model. */
final class XianqiaoExchangeMountedBackendTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void aBoundMountedCellEnumeratesAndMutatesAuthoritativeItemsAndFluids() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000401");
        UUID disk = UUID.fromString("00000000-0000-0000-0000-000000000402");
        CultivationPlayerData data = new CultivationPlayerData();
        data.setStage(7);
        TerminalEntryKey diamonds = TerminalEntryKey.of(new ItemStack(Items.DIAMOND));
        TerminalFluidKey water = TerminalFluidKey.of(
                new net.neoforged.neoforge.fluids.FluidStack(Fluids.WATER, 1));
        assertEquals(73L, data.insertXianqiaoItem(
                diamonds, 73L, TerminalStorageAction.EXECUTE));
        assertEquals(2_500L, data.insertXianqiaoFluid(
                water, 2_500L, TerminalStorageAction.EXECUTE));

        AtomicInteger endpointChanges = new AtomicInteger();
        TerminalItemStorage items = new PersonalStorageLongItemStorage(
                data, endpointChanges::incrementAndGet, () -> true);
        TerminalFluidStorage fluids = new PersonalStorageFluidHandler(
                data, endpointChanges::incrementAndGet, () -> true);
        PersonalStorageEndpoint endpoint = endpoint(owner, data, items, fluids);
        ItemStack link = new ItemStack(ModItems.XIANQIAO_EXCHANGE_CELL.get());
        assertTrue(XianqiaoExchangeCellItem.bindUnbound(link, owner, disk, "BackendOwner"));
        XianqiaoExchangeStorageCell cell = XianqiaoExchangeCellHandler.createCell(
                link, (server, requestedOwner) -> owner.equals(requestedOwner) ? endpoint : null);
        assertNotNull(cell);

        activateThroughConcreteClassIndexedGrid(cell);

        AEItemKey diamondKey = AEItemKey.of(Items.DIAMOND);
        AEFluidKey waterKey = AEFluidKey.of(Fluids.WATER);
        KeyCounter initial = new KeyCounter();
        cell.getAvailableStacks(initial);
        assertEquals(73L, initial.get(diamondKey));
        assertEquals(2_500L, initial.get(waterKey));

        assertEquals(11L, cell.extract(
                diamondKey, 11L, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(7L, cell.insert(
                diamondKey, 7L, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(500L, cell.extract(
                waterKey, 500L, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(250L, cell.insert(
                waterKey, 250L, Actionable.MODULATE, IActionSource.empty()));

        assertEquals(69L, data.getXianqiaoItemSummary().stream()
                .filter(summary -> diamonds.matches(summary.prototype()))
                .mapToLong(summary -> summary.amount()).sum(),
                "AE2 item operations must reach CultivationPlayerData, not a copied cell inventory");
        assertEquals(2_250L, data.getXianqiaoFluidAmount(water),
                "AE2 fluid operations must reach CultivationPlayerData, not a copied cell inventory");
        assertEquals(4, endpointChanges.get());

        KeyCounter afterMutations = new KeyCounter();
        cell.getAvailableStacks(afterMutations);
        assertEquals(69L, afterMutations.get(diamondKey));
        assertEquals(2_250L, afterMutations.get(waterKey));
    }

    private static PersonalStorageEndpoint endpoint(
            UUID owner,
            CultivationPlayerData data,
            TerminalItemStorage items,
            TerminalFluidStorage fluids) {
        IItemHandler unusedStackBridge = new ItemStackHandler();
        return new PersonalStorageEndpoint() {
            @Override public UUID owner() { return owner; }
            @Override public int stage() { return data.getStage(); }
            @Override public boolean online() { return true; }
            @Override public IItemHandler itemHandler() { return unusedStackBridge; }
            @Override public ItemStack insert(ItemStack stack, boolean simulate) { return stack; }
            @Override public ItemStack extract(ItemStack template, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }
            @Override public TerminalItemStorage itemStorage() { return items; }
            @Override public TerminalFluidStorage fluidStorage() { return fluids; }
        };
    }

    private static void activateThroughConcreteClassIndexedGrid(XianqiaoExchangeStorageCell cell) {
        IChestOrDrive drive = proxy(IChestOrDrive.class, (method, arguments) -> switch (method) {
            case "getCellCount" -> 1;
            case "getOriginalCellInventory" -> cell;
            default -> defaultValue(arguments.returnType());
        });
        IGridNode node = proxy(IGridNode.class, (method, arguments) -> switch (method) {
            case "getOwner" -> drive;
            case "isActive" -> true;
            default -> defaultValue(arguments.returnType());
        });
        IStorageService storage = proxy(IStorageService.class,
                (method, arguments) -> defaultValue(arguments.returnType()));
        IGrid grid = proxy(IGrid.class, (method, arguments) -> switch (method) {
            case "getNodes" -> List.of(node);
            // Mirrors AE2 19.2.17: interface keys are absent from the concrete-class index.
            case "getActiveMachines" -> Set.of();
            case "getStorageService" -> storage;
            default -> defaultValue(arguments.returnType());
        });
        new XianqiaoExchangeGridService(grid).onServerStartTick();
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

    private record InvocationArguments(Class<?> returnType, Object[] values) {}
}
