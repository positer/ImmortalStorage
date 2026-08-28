package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.block.entity.TreasureBasinBlockEntity;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import com.immortalstorage.immortalstorage.worldshard.TreasureBasinStatus;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Three-row cache menu backed exclusively by the Treasure Basin itself. */
public final class TreasureBasinMenu extends AbstractContainerMenu implements MachineRedstoneMenu {
    public static final int CACHE_SLOT_COUNT = WorldShardMinerCache.SLOT_COUNT;
    public static final int PLAYER_SLOT_COUNT = Inventory.INVENTORY_SIZE;
    public static final int CACHE_START = 0;
    public static final int PLUGIN_MENU_SLOT = CACHE_SLOT_COUNT;
    public static final int PLAYER_START = PLUGIN_MENU_SLOT + 1;
    public static final int PLAYER_INVENTORY_END = PLAYER_START + 27;
    public static final int PLAYER_END = PLAYER_START + PLAYER_SLOT_COUNT;

    public static final int CACHE_Y = 36;
    public static final int PLAYER_INVENTORY_Y = 103;
    public static final int HOTBAR_Y = 161;

    private static final int DATA_STATUS = 0;
    private static final int DATA_FILLED_SLOTS = 1;
    public static final int DATA_COUNT = 10;

    private final Container cacheContainer;
    private final ContainerData statusData;
    private final @Nullable TreasureBasinBlockEntity basin;
    private final net.minecraft.world.inventory.DataSlot redstoneMode;
    private boolean settingsVisible;

    public TreasureBasinMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, resolveContext(inventory, buffer));
    }

    public TreasureBasinMenu(int id, Inventory inventory, TreasureBasinBlockEntity basin) {
        this(id, inventory, basin, liveStatus(basin), basin);
    }

    /** Test-friendly constructor; production passes the basin itself here. */
    public TreasureBasinMenu(int id, Inventory inventory,
                             Container cacheContainer, ContainerData statusData) {
        this(id, inventory, cacheContainer, statusData,
                cacheContainer instanceof TreasureBasinBlockEntity blockEntity
                        ? blockEntity : null);
    }

    private TreasureBasinMenu(int id, Inventory inventory, ResolvedContext context) {
        this(id, inventory, context.cache(), context.status(), context.basin());
    }

    private TreasureBasinMenu(int id, Inventory inventory,
                              Container cacheContainer, ContainerData statusData,
                              @Nullable TreasureBasinBlockEntity basin) {
        super(ModMenus.TREASURE_BASIN.get(), id);
        checkContainerSize(cacheContainer, CACHE_SLOT_COUNT);
        checkContainerDataCount(statusData, DATA_COUNT);
        this.cacheContainer = cacheContainer;
        this.statusData = statusData;
        this.basin = basin;
        this.redstoneMode = MachineRedstoneMenu.dataSlot(basin);
        cacheContainer.startOpen(inventory.player);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(cacheContainer, column + row * 9,
                        8 + column * 18, CACHE_Y + row * 18));
            }
        }
        addSlot(new Slot(cacheContainer, CACHE_SLOT_COUNT, 190, 160) {
            @Override public boolean mayPlace(ItemStack stack) {
                return com.immortalstorage.immortalstorage.block.entity.ReinforcementPluginHost.isPlugin(stack);
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean isActive() { return settingsVisible; }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }
        addDataSlots(statusData);
        addDataSlot(redstoneMode);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack moving = slot.getItem();
        ItemStack original = moving.copy();

        if (index < PLAYER_START) {
            if (!moveItemStackTo(moving, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (com.immortalstorage.immortalstorage.block.entity.ReinforcementPluginHost.isPlugin(moving)) {
            if (!moveItemStackTo(moving, PLUGIN_MENU_SLOT, PLUGIN_MENU_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(moving, CACHE_START, PLUGIN_MENU_SLOT, false)) {
            if (index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(moving, PLAYER_INVENTORY_END, PLAYER_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(moving, PLAYER_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (moving.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, moving);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (basin == null) return cacheContainer.stillValid(player);
        return basin.getLevel() == player.level()
                && basin.getLevel().getBlockEntity(basin.getBlockPos()) == basin
                && player.distanceToSqr(basin.getBlockPos().getX() + 0.5D,
                basin.getBlockPos().getY() + 0.5D,
                basin.getBlockPos().getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        cacheContainer.stopOpen(player);
    }

    public Container getCacheContainer() {
        return cacheContainer;
    }

    public int getFilledSlots() {
        return statusData.get(DATA_FILLED_SLOTS);
    }

    public int getCacheCapacity() {
        return CACHE_SLOT_COUNT;
    }

    public @Nullable ResourceLocation getActiveMode() {
        return basin == null ? null : basin.getActiveMode();
    }

    public boolean isRunning() {
        return getOperatingStatus() == TreasureBasinStatus.ACTIVE;
    }

    public TreasureBasinStatus getOperatingStatus() {
        return TreasureBasinStatus.fromNetwork(statusData.get(DATA_STATUS));
    }

    public boolean xianqiaoOutput() { return statusData.get(2) != 0; }
    public boolean automaticOutput() { return statusData.get(3) != 0; }
    public boolean outputFace(int side) { return side >= 0 && side < 6 && statusData.get(4 + side) != 0; }
    public void setSettingsVisible(boolean visible) { settingsVisible = visible; }
    @Override public net.minecraft.world.inventory.DataSlot redstoneModeSlot() { return redstoneMode; }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id == MachineRedstoneMenu.CYCLE_BUTTON_ID) return MachineRedstoneMenu.cycle(basin);
        if (basin == null) return false;
        if (id == 2) { settingsVisible = !settingsVisible; return true; }
        if (id == 0) { basin.toggleXianqiaoOutput(); return true; }
        if (id == 1) { basin.toggleAutomaticOutput(); return true; }
        if (id >= 10 && id < 16) {
            basin.toggleOutputFace(net.minecraft.core.Direction.from3DDataValue(id - 10)); return true;
        }
        return false;
    }

    private static ContainerData liveStatus(TreasureBasinBlockEntity basin) {
        if (basin.getLevel() != null && basin.getLevel().isClientSide()) {
            return new SimpleContainerData(DATA_COUNT);
        }
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_STATUS -> basin.getOperatingStatus().ordinal();
                    case DATA_FILLED_SLOTS -> filledSlots(basin);
                    case 2 -> basin.xianqiaoOutput() ? 1 : 0;
                    case 3 -> basin.automaticOutput() ? 1 : 0;
                    case 4, 5, 6, 7, 8, 9 -> basin.outputFace(
                            net.minecraft.core.Direction.from3DDataValue(index - 4)) ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Read-only status surface.
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static int filledSlots(Container container) {
        int filled = 0;
        for (int slot = 0; slot < CACHE_SLOT_COUNT; slot++) {
            if (!container.getItem(slot).isEmpty()) filled++;
        }
        return filled;
    }

    private static ResolvedContext resolveContext(Inventory inventory, FriendlyByteBuf buffer) {
        if (inventory != null && inventory.player != null && buffer != null) {
            var pos = buffer.readBlockPos();
            if (inventory.player.level().getBlockEntity(pos) instanceof TreasureBasinBlockEntity basin) {
                return new ResolvedContext(basin, basin, new SimpleContainerData(DATA_COUNT));
            }
        }
        return new ResolvedContext(null,
                new SimpleContainer(CACHE_SLOT_COUNT), new SimpleContainerData(DATA_COUNT));
    }

    private record ResolvedContext(
            @Nullable TreasureBasinBlockEntity basin,
            Container cache,
            ContainerData status) {
    }
}
