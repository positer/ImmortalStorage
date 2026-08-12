package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Two opposite-state stabilized regions merged into one block sharing a 54-slot
 * buffer. The normal side collects dropped items inside its range into the
 * buffer; the reversed side ejects the buffer to its target block. Each side has
 * its own range, frequency, preview, enabled state and item filter.
 */
public final class EntangledStabilizedMiniatureImmortalRuinBlockEntity extends com.immortalstorage.immortalstorage.compat.mc2612.CompatBlockEntity implements Container, MenuProvider {
    public static final int SLOT_COUNT = 54;
    private static final int FILTER_SLOTS = 20;
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final Side normal = new Side();
    private final Side reversedSide = new Side();
    private final NonNullList<ItemStack> filtersNormal = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> filtersReversed = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);
    private boolean filterMatchNormal, filterWhitelistNormal = true;
    private boolean filterMatchReversed, filterWhitelistReversed = true;

    public EntangledStabilizedMiniatureImmortalRuinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), pos, state);
    }

    public ItemStackHandler itemHandler() { return inventory; }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (normal.enabled && normal.frequency > 0 && serverLevel.getGameTime() % normal.frequency == 0) {
            collect(serverLevel, normal, 0);
        }
        if (reversedSide.enabled && reversedSide.frequency > 0 && serverLevel.getGameTime() % reversedSide.frequency == 0) {
            eject(serverLevel, reversedSide, 1);
        }
    }

    private void collect(ServerLevel level, Side side, int sideIndex) {
        AABB area = selectedArea(side);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive)) {
            if (!allows(sideIndex, entity.getItem())) continue;
            ItemStack remaining = entity.getItem().copy();
            for (int slot = 0; slot < SLOT_COUNT && !remaining.isEmpty(); slot++) {
                remaining = inventory.insertItem(slot, remaining, false);
            }
            entity.setItem(remaining);
            if (remaining.isEmpty()) entity.discard();
        }
    }

    private void eject(ServerLevel level, Side side, int sideIndex) {
        if (side.faceMask == 0) return;
        BlockPos target = worldPosition.offset(side.offsetX, side.offsetY, side.offsetZ);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stored = inventory.getStackInSlot(slot);
            if (stored.isEmpty() || !allows(sideIndex, stored)) continue;
            ItemStack removed = inventory.extractItem(slot, stored.getCount(), false);
            if (removed.isEmpty()) continue;
            for (Direction face : AdvancedRuinScheduler.enabledFaces(side.faceMask)) {
                if (removed.isEmpty()) break;
                var handler = com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.itemHandler(level.getCapability(Capabilities.Item.BLOCK, target, face));
                if (handler != null) {
                    for (int targetSlot = 0; targetSlot < handler.getSlots() && !removed.isEmpty(); targetSlot++) {
                        removed = handler.insertItem(targetSlot, removed, false);
                    }
                }
            }
            if (!removed.isEmpty() && level.getBlockState(target).isAir()) {
                level.addFreshEntity(new ItemEntity(level, target.getX() + 0.5D, target.getY() + 0.5D,
                        target.getZ() + 0.5D, removed));
                removed = ItemStack.EMPTY;
            }
            if (!removed.isEmpty()) inventory.insertItem(slot, removed, false);
        }
    }

    private AABB selectedArea(Side side) {
        BlockPos min = worldPosition.offset(side.offsetX, side.offsetY, side.offsetZ);
        BlockPos max = min.offset(side.sizeX, side.sizeY, side.sizeZ);
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    private boolean allows(int side, ItemStack stack) {
        NonNullList<ItemStack> filters = side == 0 ? filtersNormal : filtersReversed;
        boolean matched = false;
        boolean configured = false;
        for (ItemStack filter : filters) {
            if (filter.isEmpty()) continue;
            configured = true;
            boolean m = (side == 0 ? filterMatchNormal : filterMatchReversed)
                    ? ItemStack.isSameItemSameComponents(filter, stack)
                    : ItemStack.isSameItem(filter, stack);
            if (m) { matched = true; break; }
        }
        if (!configured) return true;
        boolean whitelist = side == 0 ? filterWhitelistNormal : filterWhitelistReversed;
        return whitelist ? matched : !matched;
    }

    public Side normalSide() { return normal; }
    public Side reversedSide() { return reversedSide; }

    public ItemStack filter(int side, int slot) {
        NonNullList<ItemStack> list = side == 0 ? filtersNormal : filtersReversed;
        return slot >= 0 && slot < list.size() ? list.get(slot) : ItemStack.EMPTY;
    }
    public void setFilter(int side, int slot, ItemStack stack) {
        NonNullList<ItemStack> list = side == 0 ? filtersNormal : filtersReversed;
        if (slot >= 0 && slot < list.size()) {
            list.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
            setChangedAndSync();
        }
    }
    public boolean filterMatchComponents(int side) { return side == 0 ? filterMatchNormal : filterMatchReversed; }
    public boolean filterWhitelist(int side) { return side == 0 ? filterWhitelistNormal : filterWhitelistReversed; }
    public void toggleFilterMatchComponents(int side) {
        if (side == 0) filterMatchNormal = !filterMatchNormal; else filterMatchReversed = !filterMatchReversed;
        setChangedAndSync();
    }
    public void toggleFilterWhitelist(int side) {
        if (side == 0) filterWhitelistNormal = !filterWhitelistNormal; else filterWhitelistReversed = !filterWhitelistReversed;
        setChangedAndSync();
    }

    public void setMenuValue(int side, int index, int value) {
        (side == 0 ? normal : reversedSide).set(index, value);
        setChangedAndSync();
    }

    public int sideFaceMask(int side) {
        return (side == 0 ? normal : reversedSide).faceMask;
    }

    public ContainerData menuData() {
        return new ContainerData() {
            @Override public int get(int index) {
                Side side = index < 10 ? normal : reversedSide;
                return side.get(index % 10);
            }
            @Override public void set(int index, int value) {
                Side side = index < 10 ? normal : reversedSide;
                side.set(index % 10, value);
                setChangedAndSync();
            }
            @Override public int getCount() { return 20; }
        };
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.immortalstorage.entangled_stabilized_miniature_immortal_ruin");
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.immortalstorage.immortalstorage.menu.custom.EntangledMiniatureRuinMenu(id, inventory, this, menuData());
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { for (int i = 0; i < SLOT_COUNT; i++) if (!inventory.getStackInSlot(i).isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) { return inventory.getStackInSlot(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return inventory.extractItem(slot, amount, false); }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack stack = inventory.getStackInSlot(slot); inventory.setStackInSlot(slot, ItemStack.EMPTY); return stack; }
    @Override public void setItem(int slot, ItemStack stack) { inventory.setStackInSlot(slot, stack); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { for (int i = 0; i < SLOT_COUNT; i++) inventory.setStackInSlot(i, ItemStack.EMPTY); }

    @Override
    protected void saveAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditionalLegacy(tag, registries);
        tag.put("Inventory", com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.serialize(inventory, registries));
        tag.put("Normal", normal.save());
        tag.put("Reversed", reversedSide.save());
        tag.put("FiltersNormal", saveFilters(filtersNormal, registries));
        tag.put("FiltersReversed", saveFilters(filtersReversed, registries));
        tag.putBoolean("FilterMatchNormal", filterMatchNormal);
        tag.putBoolean("FilterWhitelistNormal", filterWhitelistNormal);
        tag.putBoolean("FilterMatchReversed", filterMatchReversed);
        tag.putBoolean("FilterWhitelistReversed", filterWhitelistReversed);
    }

    @Override
    protected void loadAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditionalLegacy(tag, registries);
        if (tag.contains("Inventory")) com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.deserialize(inventory, registries, tag.getCompoundOrEmpty("Inventory"));
        normal.load(tag.getCompoundOrEmpty("Normal"));
        reversedSide.load(tag.getCompoundOrEmpty("Reversed"));
        loadFilters(tag.getListOrEmpty("FiltersNormal"), filtersNormal, registries);
        loadFilters(tag.getListOrEmpty("FiltersReversed"), filtersReversed, registries);
        filterMatchNormal = tag.getBooleanOr("FilterMatchNormal", false);
        filterWhitelistNormal = !tag.contains("FilterWhitelistNormal") || tag.getBooleanOr("FilterWhitelistNormal", false);
        filterMatchReversed = tag.getBooleanOr("FilterMatchReversed", false);
        filterWhitelistReversed = !tag.contains("FilterWhitelistReversed") || tag.getBooleanOr("FilterWhitelistReversed", false);
    }

    private static ListTag saveFilters(NonNullList<ItemStack> filters, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int slot = 0; slot < filters.size(); slot++) if (!filters.get(slot).isEmpty()) {
            CompoundTag row = new CompoundTag();
            row.putInt("Slot", slot);
            row.put("Item", com.immortalstorage.immortalstorage.compat.mc2612.CompatCodec.saveItemStack(registries, filters.get(slot)));
            list.add(row);
        }
        return list;
    }

    private static void loadFilters(ListTag list, NonNullList<ItemStack> filters, HolderLookup.Provider registries) {
        filters.replaceAll(ignored -> ItemStack.EMPTY);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag row = list.getCompoundOrEmpty(index);
            int slot = row.getIntOr("Slot", 0);
            if (slot >= 0 && slot < filters.size()) filters.set(slot, com.immortalstorage.immortalstorage.compat.mc2612.CompatCodec.parseItemStack(registries, row.getCompoundOrEmpty("Item")));
        }
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditionalLegacy(tag, registries);
        tag.remove("Inventory");
        return tag;
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Independent stabilized-style region settings (range, frequency, preview, enabled). */
    public final class Side {
        private static final int CLAMP_SIZE = 13, CLAMP_OFFSET = 13;
        int sizeX = 1, sizeY = 1, sizeZ = 1;
        int offsetX, offsetY, offsetZ;
        int frequency = 20;
        boolean preview;
        boolean enabled;
        /** Bit mask of enabled container interaction faces (one bit per Direction ordinal). */
        int faceMask = AdvancedRuinScheduler.ALL_FACES;

        public int faceMask() { return faceMask; }

        int get(int local) {
            return switch (local) {
                case 0 -> sizeX; case 1 -> sizeY; case 2 -> sizeZ;
                case 3 -> offsetX; case 4 -> offsetY; case 5 -> offsetZ;
                case 6 -> frequency; case 7 -> preview ? 1 : 0; case 8 -> enabled ? 1 : 0;
                case 9 -> faceMask;
                default -> 0;
            };
        }

        void set(int local, int value) {
            switch (local) {
                case 0 -> sizeX = clamp(value, 1, CLAMP_SIZE);
                case 1 -> sizeY = clamp(value, 1, CLAMP_SIZE);
                case 2 -> sizeZ = clamp(value, 1, CLAMP_SIZE);
                case 3 -> offsetX = clamp(value, -CLAMP_OFFSET, CLAMP_OFFSET);
                case 4 -> offsetY = clamp(value, -CLAMP_OFFSET, CLAMP_OFFSET);
                case 5 -> offsetZ = clamp(value, -CLAMP_OFFSET, CLAMP_OFFSET);
                case 6 -> frequency = clamp(value, 1, 72_000);
                case 7 -> preview = value != 0;
                case 8 -> enabled = value != 0;
                case 9 -> faceMask = value & AdvancedRuinScheduler.ALL_FACES;
                default -> { }
            }
        }

        public int sizeX() { return sizeX; }
        public int sizeY() { return sizeY; }
        public int sizeZ() { return sizeZ; }
        public int offsetX() { return offsetX; }
        public int offsetY() { return offsetY; }
        public int offsetZ() { return offsetZ; }
        public int frequency() { return frequency; }
        public boolean preview() { return preview; }
        public boolean enabled() { return enabled; }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("SizeX", sizeX); tag.putInt("SizeY", sizeY); tag.putInt("SizeZ", sizeZ);
            tag.putInt("OffsetX", offsetX); tag.putInt("OffsetY", offsetY); tag.putInt("OffsetZ", offsetZ);
            tag.putInt("Frequency", frequency); tag.putBoolean("Preview", preview); tag.putBoolean("Enabled", enabled);
            tag.putInt("FaceMask", faceMask);
            return tag;
        }

        void load(CompoundTag tag) {
            if (tag.isEmpty()) return;
            sizeX = clamp(tag.getIntOr("SizeX", 0), 1, CLAMP_SIZE);
            sizeY = clamp(tag.getIntOr("SizeY", 0), 1, CLAMP_SIZE);
            sizeZ = clamp(tag.getIntOr("SizeZ", 0), 1, CLAMP_SIZE);
            offsetX = clamp(tag.getIntOr("OffsetX", 0), -CLAMP_OFFSET, CLAMP_OFFSET);
            offsetY = clamp(tag.getIntOr("OffsetY", 0), -CLAMP_OFFSET, CLAMP_OFFSET);
            offsetZ = clamp(tag.getIntOr("OffsetZ", 0), -CLAMP_OFFSET, CLAMP_OFFSET);
            frequency = clamp(tag.getIntOr("Frequency", 0), 1, 72_000);
            preview = tag.getBooleanOr("Preview", false); enabled = tag.getBooleanOr("Enabled", false);
            faceMask = tag.contains("FaceMask") ? tag.getIntOr("FaceMask", 0) & AdvancedRuinScheduler.ALL_FACES : migrateFace(tag);
        }

        /** Old single-face tag ({@code -1} = any face): {@code -1} becomes all faces, an ordinal becomes its bit. */
        private static int migrateFace(CompoundTag tag) {
            if (!tag.contains("Face")) return AdvancedRuinScheduler.ALL_FACES;
            int value = tag.getIntOr("Face", 0);
            if (value < 0 || value >= net.minecraft.core.Direction.values().length) return AdvancedRuinScheduler.ALL_FACES;
            return 1 << value;
        }

        private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    }
}
