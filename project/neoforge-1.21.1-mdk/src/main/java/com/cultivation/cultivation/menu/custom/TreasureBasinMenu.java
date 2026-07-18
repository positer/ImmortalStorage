package com.cultivation.cultivation.menu.custom;

import com.cultivation.cultivation.block.entity.TreasureBasinBlockEntity;
import com.cultivation.cultivation.menu.ModMenus;
import com.cultivation.cultivation.worldshard.TreasureBasinStatus;
import com.cultivation.cultivation.worldshard.WorldShardMinerCache;
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
public final class TreasureBasinMenu extends AbstractContainerMenu {
    public static final int CACHE_SLOT_COUNT = WorldShardMinerCache.SLOT_COUNT;
    public static final int PLAYER_SLOT_COUNT = Inventory.INVENTORY_SIZE;
    public static final int CACHE_START = 0;
    public static final int PLAYER_START = CACHE_START + CACHE_SLOT_COUNT;
    public static final int PLAYER_INVENTORY_END = PLAYER_START + 27;
    public static final int PLAYER_END = PLAYER_START + PLAYER_SLOT_COUNT;

    public static final int CACHE_Y = 36;
    public static final int PLAYER_INVENTORY_Y = 103;
    public static final int HOTBAR_Y = 161;

    private static final int DATA_STATUS = 0;
    private static final int DATA_FILLED_SLOTS = 1;
    public static final int DATA_COUNT = 2;

    private final Container cacheContainer;
    private final ContainerData statusData;
    private final @Nullable TreasureBasinBlockEntity basin;

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
        cacheContainer.startOpen(inventory.player);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(cacheContainer, column + row * 9,
                        8 + column * 18, CACHE_Y + row * 18));
            }
        }
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
        } else if (!moveItemStackTo(moving, CACHE_START, PLAYER_START, false)) {
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
