package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.block.entity.ReinforcementPluginHost;
import com.immortalstorage.immortalstorage.block.entity.WorldShardMinerBlockEntity;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerCache;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerStatus;
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

/** World Shard Miner cache and settings surface; layout mirrors the Treasure Basin. */
public final class WorldShardMinerMenu extends AbstractContainerMenu {
    public static final int CACHE_SLOT_COUNT = WorldShardMinerCache.SLOT_COUNT;
    public static final int PLUGIN_MENU_SLOT = CACHE_SLOT_COUNT;
    public static final int PLAYER_START = PLUGIN_MENU_SLOT + 1;
    public static final int PLAYER_INVENTORY_END = PLAYER_START + 27;
    public static final int PLAYER_END = PLAYER_START + Inventory.INVENTORY_SIZE;
    public static final int CACHE_Y = 36;
    public static final int PLAYER_INVENTORY_Y = 103;
    public static final int HOTBAR_Y = 161;
    public static final int DATA_COUNT = 11;

    private final Container cacheContainer;
    private final ContainerData data;
    private final @Nullable WorldShardMinerBlockEntity miner;
    private boolean settingsVisible;

    public WorldShardMinerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, resolveContext(inventory, buffer));
    }

    public WorldShardMinerMenu(int id, Inventory inventory, WorldShardMinerBlockEntity miner) {
        this(id, inventory, miner, liveData(miner), miner);
    }

    public WorldShardMinerMenu(int id, Inventory inventory,
                               Container container, ContainerData data) {
        this(id, inventory, container, data,
                container instanceof WorldShardMinerBlockEntity blockEntity ? blockEntity : null);
    }

    private WorldShardMinerMenu(int id, Inventory inventory, ResolvedContext context) {
        this(id, inventory, context.container(), context.data(), context.miner());
    }

    private WorldShardMinerMenu(int id, Inventory inventory, Container container,
                                ContainerData data, @Nullable WorldShardMinerBlockEntity miner) {
        super(ModMenus.WORLD_SHARD_MINER.get(), id);
        checkContainerSize(container, CACHE_SLOT_COUNT + 1);
        checkContainerDataCount(data, DATA_COUNT);
        this.cacheContainer = container;
        this.data = data;
        this.miner = miner;
        container.startOpen(inventory.player);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(container, column + row * 9,
                        8 + column * 18, CACHE_Y + row * 18));
            }
        }
        addSlot(new Slot(container, PLUGIN_MENU_SLOT, 190, 160) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ReinforcementPluginHost.isPlugin(stack);
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
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack moving = slot.getItem();
        ItemStack original = moving.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(moving, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (ReinforcementPluginHost.isPlugin(moving)) {
            if (!moveItemStackTo(moving, PLUGIN_MENU_SLOT, PLUGIN_MENU_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(moving, 0, PLUGIN_MENU_SLOT, false)) {
            if (index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(moving, PLAYER_INVENTORY_END, PLAYER_END, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(moving, PLAYER_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (moving.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, moving);
        return original;
    }

    @Override public boolean stillValid(Player player) {
        if (miner == null) return cacheContainer.stillValid(player);
        return miner.getLevel() == player.level()
                && miner.getLevel().getBlockEntity(miner.getBlockPos()) == miner
                && player.distanceToSqr(miner.getBlockPos().getCenter()) <= 64.0D;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        cacheContainer.stopOpen(player);
    }

    public Container getCacheContainer() { return cacheContainer; }
    public int getFilledSlots() { return data.get(1); }
    public int getCacheCapacity() { return CACHE_SLOT_COUNT; }
    public int getActiveLevel() { return data.get(2); }
    public @Nullable ResourceLocation getActiveMode() { return miner == null ? null : miner.getActiveMode(); }
    public WorldShardMinerStatus getOperatingStatus() {
        int ordinal = Math.max(0, Math.min(WorldShardMinerStatus.values().length - 1, data.get(0)));
        return WorldShardMinerStatus.values()[ordinal];
    }
    public boolean xianqiaoOutput() { return data.get(3) != 0; }
    public boolean automaticOutput() { return data.get(4) != 0; }
    public boolean outputFace(int side) { return side >= 0 && side < 6 && data.get(5 + side) != 0; }
    public void setSettingsVisible(boolean visible) { settingsVisible = visible; }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (miner == null) return false;
        if (id == 2) { settingsVisible = !settingsVisible; return true; }
        if (id == 0) { miner.toggleXianqiaoOutput(); return true; }
        if (id == 1) { miner.toggleAutomaticOutput(); return true; }
        if (id >= 10 && id < 16) {
            miner.toggleOutputFace(net.minecraft.core.Direction.from3DDataValue(id - 10));
            return true;
        }
        return false;
    }

    private static ContainerData liveData(WorldShardMinerBlockEntity miner) {
        if (miner.getLevel() != null && miner.getLevel().isClientSide()) {
            return new SimpleContainerData(DATA_COUNT);
        }
        return new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> miner.getOperatingStatus().ordinal();
                    case 1 -> filledSlots(miner);
                    case 2 -> miner.getActiveLevel();
                    case 3 -> miner.xianqiaoOutput() ? 1 : 0;
                    case 4 -> miner.automaticOutput() ? 1 : 0;
                    case 5, 6, 7, 8, 9, 10 -> miner.outputFace(
                            net.minecraft.core.Direction.from3DDataValue(index - 5)) ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return DATA_COUNT; }
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
            if (inventory.player.level().getBlockEntity(pos) instanceof WorldShardMinerBlockEntity miner) {
                return new ResolvedContext(miner, miner, new SimpleContainerData(DATA_COUNT));
            }
        }
        return new ResolvedContext(null,
                new SimpleContainer(CACHE_SLOT_COUNT + 1), new SimpleContainerData(DATA_COUNT));
    }

    private record ResolvedContext(@Nullable WorldShardMinerBlockEntity miner,
                                   Container container, ContainerData data) { }
}
