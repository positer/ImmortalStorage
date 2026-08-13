package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * Two advanced stabilized ruins (normal + reversed) merged into one block. Each
 * side owns an independent scheduling configuration and filter; one shared
 * 54-slot inventory backs both the storage menu and the scheduler. The block is
 * an independent container: the normal side pulls one allowed stack from each
 * nearby container into the shared inventory, the reversed side pushes the
 * shared inventory out into the containers. Rendered as two counter-rotating
 * opposite-state spheres in the blue frame.
 */
public final class AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity
        extends com.immortalstorage.immortalstorage.compat.mc2612.CompatBlockEntity implements Container, MenuProvider, ReinforcementPluginHost {
    public static final int PER_SIDE = 13;
    public static final int SLOT_COUNT = 54;
    private static final int FILTER_SLOTS = 20;
    private final SideConfig normal = new SideConfig();
    private final SideConfig reversedSide = new SideConfig();
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final NonNullList<ItemStack> filtersNormal = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> filtersReversed = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);
    private boolean filterMatchNormal, filterWhitelistNormal = true;
    private boolean filterMatchReversed, filterWhitelistReversed = true;
    private ItemStack reinforcementPlugin = ItemStack.EMPTY;

    public AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), pos, state);
    }

    public ItemStackHandler itemHandler() { return inventory; }

    public void setMenuValue(int side, int index, int value) {
        if (index < 0 || index >= PER_SIDE) return;
        (side == 0 ? normal : reversedSide).set(index, value);
        setChangedAndSync();
    }

    public int sideFaceMask(int side) {
        return (side == 0 ? normal : reversedSide).faceMask;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        runSide(serverLevel, normal, false);
        runSide(serverLevel, reversedSide, true);
    }

    private void runSide(ServerLevel serverLevel, SideConfig side, boolean reversed) {
        if (!side.enabled || side.frequency <= 0 || serverLevel.getGameTime() % side.frequency != 0) return;
        List<AdvancedRuinScheduler.Target> targets = AdvancedRuinScheduler.scan(
                serverLevel, worldPosition, side.offsetX, side.offsetY, side.offsetZ,
                side.sizeX, side.sizeY, side.sizeZ, side.orderMode == SideConfig.ORDER_FAR_FIRST, side.faceMask);
        boolean forcePoll = side.accessMode == SideConfig.ACCESS_FORCE_POLL;
        boolean itemByItem = side.splitMode == SideConfig.SPLIT_ITEM_BY_ITEM;
        var allows = (java.util.function.Predicate<ItemStack>) (stack -> allows(reversed ? 1 : 0, stack));
        if (reversed) {
            for (int group = 0; group < reinforcementMultiplier(); group++)
                if (!AdvancedRuinScheduler.eject(inventory, targets, forcePoll, itemByItem,
                        allows, side.groupCursor)) break;
        } else {
            AdvancedRuinScheduler.collect(inventory, targets, forcePoll, itemByItem, allows);
        }
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

    public SideConfig normalSide() { return normal; }
    public SideConfig reversedSide() { return reversedSide; }

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

    public ContainerData menuData() {
        return new ContainerData() {
            @Override public int get(int index) {
                SideConfig side = index < PER_SIDE ? normal : reversedSide;
                return side.get(index % PER_SIDE);
            }
            @Override public void set(int index, int value) {
                SideConfig side = index < PER_SIDE ? normal : reversedSide;
                side.set(index % PER_SIDE, value);
                setChangedAndSync();
            }
            @Override public int getCount() { return PER_SIDE * 2; }
        };
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.immortalstorage.advanced_entangled_stabilized_miniature_immortal_ruin");
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.immortalstorage.immortalstorage.menu.custom.AdvancedEntangledMiniatureRuinMenu(
                id, inventory, this, menuData());
    }

    @Override public int getContainerSize() { return SLOT_COUNT + 1; }
    @Override public boolean isEmpty() { for (int i = 0; i < SLOT_COUNT; i++) if (!inventory.getStackInSlot(i).isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) { return slot == SLOT_COUNT ? reinforcementPlugin : inventory.getStackInSlot(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { if (slot == SLOT_COUNT) { ItemStack out = reinforcementPlugin.split(amount); setChanged(); return out; } return inventory.extractItem(slot, amount, false); }
    @Override public ItemStack removeItemNoUpdate(int slot) { if (slot == SLOT_COUNT) { ItemStack out = reinforcementPlugin; reinforcementPlugin = ItemStack.EMPTY; return out; } ItemStack stack = inventory.getStackInSlot(slot); inventory.setStackInSlot(slot, ItemStack.EMPTY); return stack; }
    @Override public void setItem(int slot, ItemStack stack) { if (slot == SLOT_COUNT) setReinforcementPlugin(stack); else inventory.setStackInSlot(slot, stack); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == SLOT_COUNT && ReinforcementPluginHost.isPlugin(stack); }
    @Override public ItemStack reinforcementPlugin() { return reinforcementPlugin; }
    @Override public void setReinforcementPlugin(ItemStack stack) { reinforcementPlugin = ReinforcementPluginHost.isPlugin(stack) ? stack.copyWithCount(1) : ItemStack.EMPTY; setChangedAndSync(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { for (int i = 0; i < SLOT_COUNT; i++) inventory.setStackInSlot(i, ItemStack.EMPTY); reinforcementPlugin = ItemStack.EMPTY; }

    @Override
    protected void saveAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditionalLegacy(tag, registries);
        tag.put("Normal", normal.save());
        tag.put("Reversed", reversedSide.save());
        tag.put("Inventory", com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.serialize(inventory, registries));
        tag.put("FiltersNormal", saveFilters(filtersNormal, registries));
        tag.put("FiltersReversed", saveFilters(filtersReversed, registries));
        tag.putBoolean("FilterMatchNormal", filterMatchNormal);
        tag.putBoolean("FilterWhitelistNormal", filterWhitelistNormal);
        tag.putBoolean("FilterMatchReversed", filterMatchReversed);
        tag.putBoolean("FilterWhitelistReversed", filterWhitelistReversed);
        if (!reinforcementPlugin.isEmpty()) tag.put("ReinforcementPlugin", com.immortalstorage.immortalstorage.compat.mc2612.CompatCodec.saveItemStack(registries, reinforcementPlugin));
    }

    @Override
    protected void loadAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditionalLegacy(tag, registries);
        normal.load(tag.getCompoundOrEmpty("Normal"));
        reversedSide.load(tag.getCompoundOrEmpty("Reversed"));
        if (tag.contains("Inventory")) com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.deserialize(inventory, registries, tag.getCompoundOrEmpty("Inventory"));
        loadFilters(tag.getListOrEmpty("FiltersNormal"), filtersNormal, registries);
        loadFilters(tag.getListOrEmpty("FiltersReversed"), filtersReversed, registries);
        filterMatchNormal = tag.getBooleanOr("FilterMatchNormal", false);
        filterWhitelistNormal = !tag.contains("FilterWhitelistNormal") || tag.getBooleanOr("FilterWhitelistNormal", false);
        filterMatchReversed = tag.getBooleanOr("FilterMatchReversed", false);
        filterWhitelistReversed = !tag.contains("FilterWhitelistReversed") || tag.getBooleanOr("FilterWhitelistReversed", false);
        reinforcementPlugin = tag.contains("ReinforcementPlugin") ? com.immortalstorage.immortalstorage.compat.mc2612.CompatCodec.parseItemStack(registries,
                tag.getCompoundOrEmpty("ReinforcementPlugin")) : ItemStack.EMPTY;
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

    /** Independent advanced-ruin scheduling configuration (staging buffer shared at BE level). */
    public final class SideConfig {
        public static final int ACCESS_POLL_SKIP = 0, ACCESS_FORCE_POLL = 1;
        public static final int SPLIT_ITEM_BY_ITEM = 0, SPLIT_GROUP_BY_GROUP = 1;
        public static final int ORDER_FAR_FIRST = 0, ORDER_NEAR_FIRST = 1;
        private static final int CLAMP_SIZE = 13, CLAMP_OFFSET = 13;

        public final int[] groupCursor = {0};
        int sizeX = 1, sizeY = 1, sizeZ = 1;
        int offsetX, offsetY, offsetZ;
        int frequency = 20;
        boolean preview;
        boolean enabled;
        int accessMode = ACCESS_POLL_SKIP;
        int splitMode = SPLIT_ITEM_BY_ITEM;
        int orderMode = ORDER_NEAR_FIRST;
        /** Bit mask of enabled container interaction faces (one bit per Direction ordinal). */
        int faceMask = AdvancedRuinScheduler.ALL_FACES;

        public int faceMask() { return faceMask; }

        int get(int local) {
            return switch (local) {
                case 0 -> sizeX; case 1 -> sizeY; case 2 -> sizeZ;
                case 3 -> offsetX; case 4 -> offsetY; case 5 -> offsetZ;
                case 6 -> frequency; case 7 -> preview ? 1 : 0; case 8 -> enabled ? 1 : 0;
                case 9 -> accessMode; case 10 -> splitMode; case 11 -> orderMode;
                case 12 -> faceMask;
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
                case 9 -> accessMode = value == 0 ? ACCESS_POLL_SKIP : ACCESS_FORCE_POLL;
                case 10 -> splitMode = value == 0 ? SPLIT_ITEM_BY_ITEM : SPLIT_GROUP_BY_GROUP;
                case 11 -> orderMode = value == 0 ? ORDER_FAR_FIRST : ORDER_NEAR_FIRST;
                case 12 -> faceMask = value & AdvancedRuinScheduler.ALL_FACES;
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
            tag.putInt("AccessMode", accessMode); tag.putInt("SplitMode", splitMode); tag.putInt("OrderMode", orderMode);
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
            accessMode = tag.getIntOr("AccessMode", 0) == ACCESS_FORCE_POLL ? ACCESS_FORCE_POLL : ACCESS_POLL_SKIP;
            splitMode = tag.getIntOr("SplitMode", 0) == SPLIT_GROUP_BY_GROUP ? SPLIT_GROUP_BY_GROUP : SPLIT_ITEM_BY_ITEM;
            orderMode = tag.getIntOr("OrderMode", 0) == ORDER_FAR_FIRST ? ORDER_FAR_FIRST : ORDER_NEAR_FIRST;
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
