package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceInventory;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog;
import com.immortalstorage.immortalstorage.compat.XianqiaoInterfaceCompatHooks;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import com.immortalstorage.core.resource.ResourceChannelKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.List;

/**
 * Server-authoritative menu for the Xianqiao Interface.
 *
 * <p>The first row is a synchronized ghost configuration mirror and never
 * participates in item capabilities. The second row is backed by the nine
 * real output buffers. Clicking a ghost slot copies the carried stack without
 * consuming it; clicking it with an empty cursor clears the target.</p>
 */
public final class XianqiaoInterfaceMenu extends AbstractContainerMenu {
    public static final long DEFAULT_EXTERNAL_CACHE_AMOUNT = 1_000L;
    public static final int CONFIG_SLOT_COUNT = XianqiaoInterfaceInventory.SLOT_COUNT;
    public static final int BUFFER_SLOT_COUNT = XianqiaoInterfaceInventory.SLOT_COUNT;
    public static final int BUFFER_START = CONFIG_SLOT_COUNT;
    public static final int PLAYER_START = BUFFER_START + BUFFER_SLOT_COUNT;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int MENU_SLOT_COUNT = PLAYER_START + PLAYER_SLOT_COUNT;

    public static final int CONFIG_Y = 31;
    public static final int BUFFER_Y = 67;
    public static final int PLAYER_INVENTORY_Y = 152;
    public static final int HOTBAR_Y = 210;
    private static final int SIDE_DATA_START = 0;
    private static final int REVISION_LOW_DATA = SIDE_DATA_START + 6;
    private static final int REVISION_HIGH_DATA = REVISION_LOW_DATA + 1;
    private static final int TYPE_DATA_START = REVISION_HIGH_DATA + 1;
    private static final int AMOUNT_LOW_DATA_START = TYPE_DATA_START + CONFIG_SLOT_COUNT;
    private static final int AMOUNT_HIGH_DATA_START = AMOUNT_LOW_DATA_START + CONFIG_SLOT_COUNT;
    private static final int CACHED_LOW_DATA_START = AMOUNT_HIGH_DATA_START + CONFIG_SLOT_COUNT;
    private static final int CACHED_HIGH_DATA_START = CACHED_LOW_DATA_START + CONFIG_SLOT_COUNT;
    private static final int ITEM_LIMIT_DATA = CACHED_HIGH_DATA_START + CONFIG_SLOT_COUNT;
    private static final int FLUID_LIMIT_DATA = ITEM_LIMIT_DATA + 1;
    private static final int SLOT_MASK_DATA_START = FLUID_LIMIT_DATA + 1;
    private static final int ACTIVE_PULL_DATA = SLOT_MASK_DATA_START + CONFIG_SLOT_COUNT;
    private static final int ACTIVE_PUSH_DATA = ACTIVE_PULL_DATA + 1;
    private static final int CONFIG_DATA_COUNT = ACTIVE_PUSH_DATA + 1;
    public static final String FLUID_DISPLAY_TAG = "ImmortalStorageInterfaceFluid";
    public static final String EXTERNAL_DISPLAY_TAG = "ImmortalStorageInterfaceExternalResource";

    private final XianqiaoInterfaceBlockEntity blockEntity;
    private final XianqiaoInterfaceInventory backend;
    private final Player openingPlayer;
    private final SimpleContainer configurationMirror = new SimpleContainer(CONFIG_SLOT_COUNT);
    private final SimpleContainer bufferMirror = new SimpleContainer(BUFFER_SLOT_COUNT);
    private final ContainerData configurationData;

    public XianqiaoInterfaceMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, resolveBlockEntity(inventory, buffer));
    }

    public XianqiaoInterfaceMenu(
            int id, Inventory inventory, XianqiaoInterfaceBlockEntity blockEntity) {
        super(ModMenus.XIANQIAO_INTERFACE.get(), id);
        this.blockEntity = blockEntity;
        this.openingPlayer = inventory.player;
        this.backend = blockEntity == null ? disconnectedBackend() : blockEntity.getInventory();
        this.configurationData = blockEntity == null || blockEntity.getLevel() == null
                || blockEntity.getLevel().isClientSide()
                ? new SimpleContainerData(CONFIG_DATA_COUNT)
                : new ContainerData() {
                    @Override
                    public int get(int index) {
                        if (index >= 0 && index < Direction.values().length) {
                            return blockEntity.getSideMode(Direction.values()[index]).ordinal();
                        }
                        long revision = blockEntity.getConfigRevision();
                        if (index == REVISION_LOW_DATA) return (int) revision;
                        if (index == REVISION_HIGH_DATA) return (int) (revision >>> 32);
                        if (index >= TYPE_DATA_START && index < AMOUNT_LOW_DATA_START) {
                            int slot = index - TYPE_DATA_START;
                            if (!blockEntity.getInventory().getFluidTarget(slot).isEmpty()) return 1;
                            return blockEntity.getInventory().getExternalTarget(slot) == null ? 0 : 2;
                        }
                        if (index >= AMOUNT_LOW_DATA_START && index < AMOUNT_HIGH_DATA_START) {
                            return (int) configuredAmount(blockEntity.getInventory(),
                                    index - AMOUNT_LOW_DATA_START);
                        }
                        if (index >= AMOUNT_HIGH_DATA_START && index < CACHED_LOW_DATA_START) {
                            return (int) (configuredAmount(blockEntity.getInventory(),
                                    index - AMOUNT_HIGH_DATA_START) >>> 32);
                        }
                        if (index >= CACHED_LOW_DATA_START && index < CACHED_HIGH_DATA_START) {
                            return (int) cachedAmount(blockEntity.getInventory(),
                                    index - CACHED_LOW_DATA_START);
                        }
                        if (index >= CACHED_HIGH_DATA_START && index < ITEM_LIMIT_DATA) {
                            return (int) (cachedAmount(blockEntity.getInventory(),
                                    index - CACHED_HIGH_DATA_START) >>> 32);
                        }
                        if (index == ITEM_LIMIT_DATA) {
                            return blockEntity.getInventory().getItemTargetLimit();
                        }
                        if (index == FLUID_LIMIT_DATA) {
                            return blockEntity.getInventory().getFluidTargetLimitMb();
                        }
                        if (index >= SLOT_MASK_DATA_START && index < ACTIVE_PULL_DATA) {
                            return blockEntity.getInventory().getOutputFaceMask(index - SLOT_MASK_DATA_START);
                        }
                        if (index == ACTIVE_PULL_DATA) return blockEntity.isActivePullEnabled() ? 1 : 0;
                        if (index == ACTIVE_PUSH_DATA) return blockEntity.isActivePushEnabled() ? 1 : 0;
                        return 0;
                    }

                    @Override public void set(int index, int value) { }
                    @Override public int getCount() { return CONFIG_DATA_COUNT; }
                };
        addDataSlots(configurationData);

        for (int column = 0; column < CONFIG_SLOT_COUNT; column++) {
            configurationMirror.setItem(column, displayTarget(backend, column));
            addSlot(new ConfigurationSlot(configurationMirror, column,
                    8 + column * 18, CONFIG_Y));
        }
        for (int column = 0; column < BUFFER_SLOT_COUNT; column++) {
            addSlot(new BufferOutputSlot(column,
                    8 + column * 18, BUFFER_Y));
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
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity != null && blockEntity.getLevel() != null
                && !blockEntity.getLevel().isClientSide() && hasLiveAccess(openingPlayer)) {
            for (int slot = 0; slot < CONFIG_SLOT_COUNT; slot++) {
                ItemStack target = displayTarget(backend, slot);
                ItemStack mirrored = configurationMirror.getItem(slot);
                if (!sameStackAndCount(target, mirrored)) {
                    configurationMirror.setItem(slot, target);
                }
            }
        }
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < CONFIG_SLOT_COUNT) {
            if (clickType != ClickType.PICKUP || !hasLiveAccess(player)) return;
            if (configureTargetFromCarried(backend, slotId, getCarried(), button)) {
                configurationMirror.setItem(slotId, displayTarget(backend, slotId));
                broadcastChanges();
            }
            return;
        }
        if (slotId >= BUFFER_START && slotId < PLAYER_START) {
            if (!hasLiveAccess(player)) return;
            if (clickType == ClickType.PICKUP && !getCarried().isEmpty()) {
                uploadCarried(slotId - BUFFER_START, button == 1 ? 1 : getCarried().getCount());
                broadcastChanges();
                return;
            }
            if (clickType == ClickType.PICKUP) {
                Slot output = slots.get(slotId);
                ItemStack available = blockEntity != null && blockEntity.getLevel() != null
                        && !blockEntity.getLevel().isClientSide()
                        ? backend.getBufferedStack(slotId - BUFFER_START)
                        : output.getItem();
                if (available.isEmpty()) return;
                int amount = button == 1 ? 1
                        : Math.min(available.getCount(), available.getMaxStackSize());
                ItemStack extracted = output.remove(amount);
                if (!extracted.isEmpty()) setCarried(extracted);
                broadcastChanges();
                return;
            }
            // Quick-move uses the guarded implementation below. Other click
            // modes could invoke Slot#set and are rejected for this proxy.
            if (clickType != ClickType.PICKUP && clickType != ClickType.QUICK_MOVE) return;
        }
        if (!hasLiveAccess(player)) return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!hasLiveAccess(player) || index < BUFFER_START || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        if (index < PLAYER_START) {
            int backendSlot = index - BUFFER_START;
            ItemStack available = backend.getBufferedStack(backendSlot);
            if (available.isEmpty()) return ItemStack.EMPTY;
            int planned = Math.min(available.getCount(), simulatedPlayerCapacity(available));
            if (planned <= 0) return ItemStack.EMPTY;

            // Extract first. Player slots are mutated only after the exact
            // real buffer transfer is committed, closing the old copy window.
            ItemStack extracted = backend.extractItem(backendSlot, planned, false);
            if (extracted.getCount() != planned
                    || !ItemStack.isSameItemSameComponents(extracted, available)) {
                restoreOrReturn(backendSlot, extracted, player);
                return ItemStack.EMPTY;
            }

            ItemStack remainder = extracted.copy();
            moveItemStackTo(remainder, PLAYER_START, MENU_SLOT_COUNT, true);
            int moved = extracted.getCount() - remainder.getCount();
            if (!remainder.isEmpty()) restoreOrReturn(backendSlot, remainder, player);
            return moved > 0 ? extracted.copyWithCount(moved) : ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack moving = slot.getItem();
        ItemStack original = moving.copy();
        ItemStack remainder = backend.insertItem(0, moving.copy(), false);
        int accepted = moving.getCount() - remainder.getCount();
        if (accepted <= 0) return ItemStack.EMPTY;
        moving.shrink(accepted);

        if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (moving.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, moving);
        return original;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !(slot instanceof ConfigurationSlot) && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof ConfigurationSlot) && super.canDragTo(slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return hasLiveAccess(player);
    }

    public XianqiaoInterfaceBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public long getConfigRevision() {
        return Integer.toUnsignedLong(configurationData.get(REVISION_LOW_DATA))
                | ((long) configurationData.get(REVISION_HIGH_DATA) << 32);
    }

    public XianqiaoInterfaceBlockEntity.SideMode getSideMode(Direction side) {
        if (side == null) return XianqiaoInterfaceBlockEntity.SideMode.DISABLED;
        int encoded = configurationData.get(side.ordinal());
        XianqiaoInterfaceBlockEntity.SideMode[] modes = XianqiaoInterfaceBlockEntity.SideMode.values();
        return encoded >= 0 && encoded < modes.length
                ? modes[encoded] : XianqiaoInterfaceBlockEntity.SideMode.DISABLED;
    }

    public ItemStack getConfiguredTarget(int slot) {
        if (slot < 0 || slot >= CONFIG_SLOT_COUNT) return ItemStack.EMPTY;
        return configurationMirror.getItem(slot).copy();
    }

    public boolean isFluidTarget(int slot) {
        return slot >= 0 && slot < CONFIG_SLOT_COUNT
                && configurationData.get(TYPE_DATA_START + slot) == 1;
    }

    public boolean isExternalTarget(int slot) {
        return slot >= 0 && slot < CONFIG_SLOT_COUNT
                && configurationData.get(TYPE_DATA_START + slot) == 2;
    }

    public long getConfiguredAmount(int slot) {
        return slot >= 0 && slot < CONFIG_SLOT_COUNT
                ? decodeLong(AMOUNT_LOW_DATA_START, AMOUNT_HIGH_DATA_START, slot) : 0L;
    }

    public long getCachedAmount(int slot) {
        return slot >= 0 && slot < CONFIG_SLOT_COUNT
                ? decodeLong(CACHED_LOW_DATA_START, CACHED_HIGH_DATA_START, slot) : 0L;
    }

    public ResourceChannelKey getExternalTarget(int slot) {
        if (!isExternalTarget(slot)) return null;
        ItemStack display = getConfiguredTarget(slot);
        CompoundTag marker = display.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!marker.contains(EXTERNAL_DISPLAY_TAG, Tag.TAG_COMPOUND)) return null;
        CompoundTag external = marker.getCompound(EXTERNAL_DISPLAY_TAG);
        try {
            return new ResourceChannelKey(external.getString("Channel"), external.getString("Resource"));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public List<ResourceChannelKey> availableExternalResources() {
        return ExternalResourceCatalog.available().stream()
                .filter(key -> !"mekanism_chemical".equals(key.channel()))
                .toList();
    }

    public int getItemTargetLimit() {
        return Math.max(1, configurationData.get(ITEM_LIMIT_DATA));
    }

    public int getFluidTargetLimitMb() {
        return Math.max(1, configurationData.get(FLUID_LIMIT_DATA));
    }

    public boolean isSlotFaceEnabled(int slot, Direction side) {
        return slot >= 0 && slot < CONFIG_SLOT_COUNT && side != null
                && (configurationData.get(SLOT_MASK_DATA_START + slot) & (1 << side.ordinal())) != 0;
    }

    public boolean isActivePullEnabled() { return configurationData.get(ACTIVE_PULL_DATA) != 0; }
    public boolean isActivePushEnabled() { return configurationData.get(ACTIVE_PUSH_DATA) != 0; }

    public static boolean configureTarget(
            XianqiaoInterfaceInventory backend, int slot, ItemStack carried) {
        if (backend == null || slot < 0 || slot >= CONFIG_SLOT_COUNT || carried == null) return false;
        ItemStack configured = carried.isEmpty() ? ItemStack.EMPTY : carried.copy();
        return backend.setTarget(slot, configured);
    }

    static boolean configureTargetFromCarried(
            XianqiaoInterfaceInventory backend, int slot, ItemStack carried, int button) {
        if (backend == null || carried == null || slot < 0 || slot >= CONFIG_SLOT_COUNT) return false;
        if (carried.isEmpty()) return backend.clearSlot(slot);
        if (button == 1) {
            var containedFluid = FluidUtil.getFluidContained(carried.copyWithCount(1));
            if (containedFluid.isPresent()) {
                FluidStack fluid = containedFluid.get();
                int amount = Math.min(backend.getFluidTargetLimitMb(),
                        Math.max(1, fluid.getAmount()));
                return backend.setFluidTarget(slot, fluid.copyWithAmount(amount));
            }
            var external = XianqiaoInterfaceCompatHooks.containedExternalResource(carried);
            if (external.isPresent()) {
                var content = external.get();
                return backend.setExternalTarget(
                        slot, content.key(), content.amount());
            }
        }
        return configureTarget(backend, slot, carried);
    }

    private ItemStack displayTarget(XianqiaoInterfaceInventory backend, int slot) {
        ItemStack item = backend.getTarget(slot);
        if (!item.isEmpty()) return item.copyWithCount(1);
        FluidStack fluid = backend.getFluidTarget(slot);
        if (fluid.isEmpty()) {
            ResourceChannelKey external = backend.getExternalTarget(slot);
            if (external == null) return ItemStack.EMPTY;
            ItemStack display = new ItemStack(ModBlocks.XIANQIAO_INTERFACE.get());
            CompoundTag marker = display.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            CompoundTag encoded = new CompoundTag();
            encoded.putString("Channel", external.channel());
            encoded.putString("Resource", external.resourceId());
            marker.put(EXTERNAL_DISPLAY_TAG, encoded);
            display.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
            return display;
        }
        ItemStack display = FluidUtil.getFilledBucket(fluid);
        if (display.isEmpty()) display = new ItemStack(Items.BUCKET);
        if (blockEntity != null && blockEntity.getLevel() != null) {
            Tag encoded = fluid.copyWithAmount(1).saveOptional(blockEntity.getLevel().registryAccess());
            if (encoded instanceof CompoundTag compound) {
                CompoundTag marker = display.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                marker.put(FLUID_DISPLAY_TAG, compound.copy());
                display.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
            }
        }
        return display.copyWithCount(1);
    }

    private long decodeLong(int lowStart, int highStart, int slot) {
        return Integer.toUnsignedLong(configurationData.get(lowStart + slot))
                | ((long) configurationData.get(highStart + slot) << 32);
    }

    private static long configuredAmount(XianqiaoInterfaceInventory inventory, int slot) {
        ResourceChannelKey external = inventory.getExternalTarget(slot);
        if (external != null) return inventory.getExternalDesiredAmount(slot);
        FluidStack fluid = inventory.getFluidTarget(slot);
        return fluid.isEmpty() ? inventory.getTarget(slot).getCount() : fluid.getAmount();
    }

    private static long cachedAmount(XianqiaoInterfaceInventory inventory, int slot) {
        ResourceChannelKey external = inventory.getExternalTarget(slot);
        if (external != null) return inventory.getExternalCachedAmount(slot);
        FluidStack fluid = inventory.getBufferedFluid(slot);
        return fluid.isEmpty() ? inventory.getBufferedStack(slot).getCount() : fluid.getAmount();
    }

    private void uploadCarried(int backendSlot, int amount) {
        ItemStack carried = getCarried();
        if (carried.isEmpty() || amount <= 0) return;
        int offered = Math.min(amount, carried.getCount());
        ItemStack remainder = backend.insertItem(
                backendSlot, carried.copyWithCount(offered), false);
        int accepted = offered - remainder.getCount();
        if (accepted > 0) carried.shrink(accepted);
    }

    private int simulatedPlayerCapacity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        long capacity = 0L;
        for (int index = PLAYER_START; index < MENU_SLOT_COUNT; index++) {
            Slot slot = slots.get(index);
            if (!slot.isActive() || !slot.mayPlace(stack)) continue;
            ItemStack present = slot.getItem();
            if (present.isEmpty() || !ItemStack.isSameItemSameComponents(present, stack)) continue;
            int limit = Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack));
            capacity += Math.max(0, limit - present.getCount());
        }
        for (int index = PLAYER_START; index < MENU_SLOT_COUNT; index++) {
            Slot slot = slots.get(index);
            if (!slot.isActive() || !slot.mayPlace(stack) || slot.hasItem()) continue;
            capacity += Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack));
        }
        return (int) Math.min(stack.getCount(), capacity);
    }

    private void restoreOrReturn(int backendSlot, ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty()) return;
        ItemStack remainder = backend.restoreExtractedItem(backendSlot, stack);
        if (!remainder.isEmpty()) remainder = backend.insertItem(backendSlot, remainder, false);
        if (!remainder.isEmpty()) player.getInventory().placeItemBackInInventory(remainder);
    }

    private boolean hasLiveAccess(Player player) {
        if (blockEntity == null || blockEntity.getLevel() == null) return false;
        Player actor = player;
        if (actor == null && blockEntity.getLevel().isClientSide()) return true;
        if (actor == null || !blockEntity.canUse(actor)) return false;
        return blockEntity.getLevel() == actor.level()
                && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
                && actor.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5D,
                blockEntity.getBlockPos().getY() + 0.5D,
                blockEntity.getBlockPos().getZ() + 0.5D) <= 64.0D;
    }

    private static XianqiaoInterfaceBlockEntity resolveBlockEntity(
            Inventory inventory, FriendlyByteBuf buffer) {
        if (inventory == null || inventory.player == null || buffer == null) return null;
        if (inventory.player.level().getBlockEntity(buffer.readBlockPos())
                instanceof XianqiaoInterfaceBlockEntity blockEntity) {
            return blockEntity;
        }
        return null;
    }

    private static boolean sameStackAndCount(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) return left.isEmpty() && right.isEmpty();
        return left.getCount() == right.getCount()
                && ItemStack.isSameItemSameComponents(left, right);
    }

    private static XianqiaoInterfaceInventory disconnectedBackend() {
        TerminalItemStorage storage = new TerminalItemStorage() {
            @Override public long revision() { return 0L; }
            @Override public List<StorageItemSummary> snapshot() { return List.of(); }
            @Override public long insert(
                    TerminalEntryKey key, long amount, TerminalStorageAction action) { return 0L; }
            @Override public long extract(
                    TerminalEntryKey key, long amount, TerminalStorageAction action) { return 0L; }
        };
        return new XianqiaoInterfaceInventory(storage, () -> false);
    }

    private static final class ConfigurationSlot extends Slot {
        private ConfigurationSlot(SimpleContainer mirror, int slot, int x, int y) {
            super(mirror, slot, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }

        @Override
        public boolean mayPickup(Player player) {
            return false; // Ghost targets are never real inventory extraction slots.
        }

        @Override public ItemStack remove(int amount) { return ItemStack.EMPTY; }
        @Override public boolean isFake() { return true; }
    }

    private final class BufferOutputSlot extends Slot {
        private final int backendSlot;

        private BufferOutputSlot(int slot, int x, int y) {
            super(bufferMirror, slot, x, y);
            this.backendSlot = slot;
        }

        @Override
        public ItemStack getItem() {
            if (blockEntity != null && blockEntity.getLevel() != null
                    && !blockEntity.getLevel().isClientSide()) {
                ItemStack buffered = backend.getBufferedStack(backendSlot);
                return buffered.isEmpty() ? ItemStack.EMPTY : buffered.copyWithCount(1);
            }
            return bufferMirror.getItem(backendSlot);
        }

        /** Remote slot synchronization writes only the client-side display mirror. */
        @Override
        public void set(ItemStack stack) {
            bufferMirror.setItem(backendSlot,
                    stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }

        @Override
        public ItemStack remove(int amount) {
            if (amount <= 0) return ItemStack.EMPTY;
            if (blockEntity != null && blockEntity.getLevel() != null
                    && !blockEntity.getLevel().isClientSide()) {
                return backend.extractItem(backendSlot, amount, false);
            }
            return bufferMirror.removeItem(backendSlot, amount);
        }

        @Override
        public ItemStack safeInsert(ItemStack stack) {
            return safeInsert(stack, stack == null ? 0 : stack.getCount());
        }

        @Override
        public ItemStack safeInsert(ItemStack stack, int increment) {
            if (stack == null || stack.isEmpty() || increment <= 0
                    || !hasLiveAccess(openingPlayer)) return stack;
            int offered = Math.min(increment, stack.getCount());
            ItemStack upload = stack.copyWithCount(offered);
            ItemStack remainder = backend.insertItem(backendSlot, upload, false);
            int accepted = offered - remainder.getCount();
            if (accepted > 0) stack.shrink(accepted);
            return stack;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack != null && !stack.isEmpty() && hasLiveAccess(openingPlayer)
                    && (blockEntity == null || blockEntity.getLevel() == null
                    || blockEntity.getLevel().isClientSide()
                    || backend.isItemValid(backendSlot, stack));
        }

        @Override
        public boolean mayPickup(Player player) {
            return hasLiveAccess(player) && !getItem().isEmpty();
        }

        @Override public int getMaxStackSize() { return 64; }
        @Override public int getMaxStackSize(ItemStack stack) {
            return stack == null || stack.isEmpty() ? 64 : Math.min(64, stack.getMaxStackSize());
        }
    }
}
