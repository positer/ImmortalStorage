package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** 54-slot area collector/ejector with bounded offsets and persistent settings. */
public class StabilizedMiniatureImmortalRuinBlockEntity extends com.immortalstorage.immortalstorage.compat.mc2612.CompatBlockEntity implements Container, MenuProvider {
    public static final int SLOT_COUNT = 54;
    protected final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    protected int sizeX = 1, sizeY = 1, sizeZ = 1;
    protected int offsetX, offsetY, offsetZ;
    protected int frequency = 20;
    protected boolean preview;
    protected boolean enabled;
    protected boolean reversed;
    protected final NonNullList<ItemStack> filters = NonNullList.withSize(20, ItemStack.EMPTY);
    protected boolean filterMatchComponents;
    protected boolean filterWhitelist = true;
    protected BlockPos linkedPos;
    protected boolean portableRemoval;
    /** Bit mask of enabled container interaction faces (one bit per Direction ordinal). */
    protected int faceMask = AdvancedRuinScheduler.ALL_FACES;

    public StabilizedMiniatureImmortalRuinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), pos, state);
    }

    protected StabilizedMiniatureImmortalRuinBlockEntity(
            net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStackHandler itemHandler() { return activeInventory(); }
    public boolean previewEnabled() { return preview; }
    public boolean reversed() { return reversed; }
    public int sizeX() { return sizeX; }
    public int sizeY() { return sizeY; }
    public int sizeZ() { return sizeZ; }
    public int offsetX() { return offsetX; }
    public int offsetY() { return offsetY; }
    public int offsetZ() { return offsetZ; }
    public int frequency() { return frequency; }
    public boolean enabled() { return enabled; }
    public ItemStack filter(int slot) { return slot >= 0 && slot < filters.size() ? filters.get(slot) : ItemStack.EMPTY; }
    public void setFilter(int slot, ItemStack stack) { if (slot >= 0 && slot < filters.size()) { filters.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1)); setChangedAndSync(); } }
    public boolean filterMatchComponents() { return filterMatchComponents; }
    public boolean filterWhitelist() { return filterWhitelist; }
    public void toggleFilterMatchComponents() { filterMatchComponents = !filterMatchComponents; setChangedAndSync(); }
    public void toggleFilterWhitelist() { filterWhitelist = !filterWhitelist; setChangedAndSync(); }
    public BlockPos linkedPos() { return linkedPos; }

    public int faceMask() { return faceMask; }

    public void setMenuValue(int index, int value) { menuData().set(index, value); }

    public ContainerData menuData() {
        return new ContainerData() {
            @Override public int get(int index) { return switch (index) {
                case 0 -> sizeX; case 1 -> sizeY; case 2 -> sizeZ;
                case 3 -> offsetX; case 4 -> offsetY; case 5 -> offsetZ;
                case 6 -> frequency; case 7 -> preview ? 1 : 0; case 8 -> enabled ? 1 : 0;
                case 9 -> reversed ? 1 : 0; case 10 -> faceMask;
                default -> 0;
            }; }
            @Override public void set(int index, int value) {
                switch (index) {
                    case 0 -> sizeX = clamp(value, 1, 13); case 1 -> sizeY = clamp(value, 1, 13); case 2 -> sizeZ = clamp(value, 1, 13);
                    case 3 -> offsetX = clamp(value, -13, 13); case 4 -> offsetY = clamp(value, -13, 13); case 5 -> offsetZ = clamp(value, -13, 13);
                    case 6 -> frequency = clamp(value, 1, 72_000); case 7 -> preview = value != 0; case 8 -> enabled = value != 0;
                    case 10 -> faceMask = value & AdvancedRuinScheduler.ALL_FACES;
                    default -> { }
                }
                if (reversed) sizeX = sizeY = sizeZ = 1;
                setChangedAndSync();
            }
            @Override public int getCount() { return 11; }
        };
    }

    public void toggleReversed() {
        reversed = !reversed;
        if (reversed) sizeX = sizeY = sizeZ = 1;
        setChangedAndSync();
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel) || !enabled || frequency <= 0
                || serverLevel.getGameTime() % frequency != 0) return;
        if (reversed) eject(serverLevel); else collect(serverLevel);
    }

    private AABB selectedArea() {
        BlockPos min = worldPosition.offset(offsetX, offsetY, offsetZ);
        BlockPos max = min.offset(sizeX, sizeY, sizeZ);
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    private void collect(ServerLevel level) {
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, selectedArea(), ItemEntity::isAlive)) {
            if (!allows(entity.getItem())) continue;
            ItemStack remaining = entity.getItem().copy();
            ItemStackHandler targetInventory = activeInventory();
            for (int slot = 0; slot < SLOT_COUNT && !remaining.isEmpty(); slot++) {
                remaining = targetInventory.insertItem(slot, remaining, false);
            }
            entity.setItem(remaining);
            if (remaining.isEmpty()) entity.discard();
        }
    }

    private void eject(ServerLevel level) {
        if (faceMask == 0) return;
        BlockPos target = worldPosition.offset(offsetX, offsetY, offsetZ);
        ItemStackHandler sourceInventory = activeInventory();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stored = sourceInventory.getStackInSlot(slot);
            if (stored.isEmpty() || !allows(stored)) continue;
            ItemStack removed = sourceInventory.extractItem(slot, stored.getCount(), false);
            if (removed.isEmpty()) continue;
            for (Direction face : AdvancedRuinScheduler.enabledFaces(faceMask)) {
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
            if (!removed.isEmpty()) sourceInventory.insertItem(slot, removed, false);
        }
    }

    protected boolean allows(ItemStack stack) {
        boolean matched = false;
        boolean configured = false;
        for (ItemStack filter : filters) {
            if (filter.isEmpty()) continue;
            configured = true;
            if (filterMatchComponents ? ItemStack.isSameItemSameComponents(filter, stack)
                    : ItemStack.isSameItem(filter, stack)) { matched = true; break; }
        }
        if (!configured) return true;
        return filterWhitelist ? matched : !matched;
    }

    protected ItemStackHandler activeInventory() {
        if (linkedPos != null && level != null && level.getBlockEntity(linkedPos) instanceof StabilizedMiniatureImmortalRuinBlockEntity peer
                && peer.linkedPos != null && peer.linkedPos.equals(worldPosition)
                && peer.worldPosition.asLong() < worldPosition.asLong()) return peer.inventory;
        return inventory;
    }

    public void linkWith(StabilizedMiniatureImmortalRuinBlockEntity peer, ServerLevel level) {
        if (peer == null || peer == this) return;
        linkedPos = peer.worldPosition.immutable();
        peer.linkedPos = worldPosition.immutable();
        StabilizedMiniatureImmortalRuinBlockEntity primary = worldPosition.asLong() <= peer.worldPosition.asLong() ? this : peer;
        StabilizedMiniatureImmortalRuinBlockEntity secondary = primary == this ? peer : this;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack remaining = secondary.inventory.getStackInSlot(slot).copy();
            secondary.inventory.setStackInSlot(slot, ItemStack.EMPTY);
            for (int target = 0; target < SLOT_COUNT && !remaining.isEmpty(); target++) {
                remaining = primary.inventory.insertItem(target, remaining, false);
            }
            if (!remaining.isEmpty()) level.addFreshEntity(new ItemEntity(level,
                    primary.worldPosition.getX() + 0.5D, primary.worldPosition.getY() + 1.0D,
                    primary.worldPosition.getZ() + 0.5D, remaining));
        }
        setChangedAndSync(); peer.setChangedAndSync();
    }

    public boolean unlinkForBreak() {
        if (linkedPos == null || level == null) return false;
        boolean peerFound = false;
        if (level.getBlockEntity(linkedPos) instanceof StabilizedMiniatureImmortalRuinBlockEntity peer) {
            peerFound = true;
            ItemStackHandler authoritative = activeInventory();
            if (authoritative == inventory) {
                for (int slot = 0; slot < SLOT_COUNT; slot++) {
                    peer.inventory.setStackInSlot(slot, inventory.getStackInSlot(slot).copy());
                    inventory.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
            peer.linkedPos = null;
            peer.setChangedAndSync();
        }
        linkedPos = null;
        setChangedAndSync();
        return peerFound;
    }

    public void preparePortableRemoval() { unlinkForBreak(); portableRemoval = true; }

    public void handleBlockRemoval() {
        boolean transferred = unlinkForBreak();
        if (!portableRemoval && !transferred && level != null) {
            net.minecraft.world.Containers.dropContents(level, worldPosition, this);
        }
    }

    @Override public Component getDisplayName() { return Component.translatable("block.immortalstorage.stabilized_miniature_immortal_ruin"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new com.immortalstorage.immortalstorage.menu.custom.StabilizedMiniatureImmortalRuinMenu(
                id, playerInventory, this, menuData());
    }
    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { for (int i = 0; i < SLOT_COUNT; i++) if (!activeInventory().getStackInSlot(i).isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) { return activeInventory().getStackInSlot(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return activeInventory().extractItem(slot, amount, false); }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStackHandler active = activeInventory(); ItemStack stack = active.getStackInSlot(slot); active.setStackInSlot(slot, ItemStack.EMPTY); return stack; }
    @Override public void setItem(int slot, ItemStack stack) { activeInventory().setStackInSlot(slot, stack); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { for (int i = 0; i < SLOT_COUNT; i++) activeInventory().setStackInSlot(i, ItemStack.EMPTY); }

    @Override
    protected void saveAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditionalLegacy(tag, registries);
        tag.put("Inventory", com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.serialize(inventory, registries));
        tag.putInt("SizeX", sizeX); tag.putInt("SizeY", sizeY); tag.putInt("SizeZ", sizeZ);
        tag.putInt("OffsetX", offsetX); tag.putInt("OffsetY", offsetY); tag.putInt("OffsetZ", offsetZ);
        tag.putInt("Frequency", frequency); tag.putBoolean("Preview", preview); tag.putBoolean("Enabled", enabled);
        tag.putBoolean("Reversed", reversed);
        net.minecraft.nbt.ListTag filterList = new net.minecraft.nbt.ListTag();
        for (int slot = 0; slot < filters.size(); slot++) if (!filters.get(slot).isEmpty()) {
            CompoundTag row = new CompoundTag(); row.putInt("Slot", slot);
            row.put("Item", com.immortalstorage.immortalstorage.compat.mc2612.CompatCodec.saveItemStack(registries, filters.get(slot))); filterList.add(row);
        }
        tag.put("Filters", filterList); tag.putBoolean("FilterMatchComponents", filterMatchComponents);
        tag.putBoolean("FilterWhitelist", filterWhitelist);
        tag.putInt("FaceMask", faceMask);
        if (linkedPos != null) tag.putLong("LinkedPos", linkedPos.asLong());
    }

    @Override
    protected void loadAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditionalLegacy(tag, registries);
        if (tag.contains("Inventory")) com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.deserialize(inventory, registries, tag.getCompoundOrEmpty("Inventory"));
        sizeX = clamp(tag.getIntOr("SizeX", 0), 1, 13); sizeY = clamp(tag.getIntOr("SizeY", 0), 1, 13); sizeZ = clamp(tag.getIntOr("SizeZ", 0), 1, 13);
        offsetX = clamp(tag.getIntOr("OffsetX", 0), -13, 13); offsetY = clamp(tag.getIntOr("OffsetY", 0), -13, 13); offsetZ = clamp(tag.getIntOr("OffsetZ", 0), -13, 13);
        frequency = clamp(tag.getIntOr("Frequency", 0), 1, 72_000); preview = tag.getBooleanOr("Preview", false); enabled = tag.getBooleanOr("Enabled", false);
        reversed = tag.getBooleanOr("Reversed", false);
        if (reversed) sizeX = sizeY = sizeZ = 1;
        filters.replaceAll(ignored -> ItemStack.EMPTY);
        net.minecraft.nbt.ListTag filterList = tag.getListOrEmpty("Filters");
        for (int index = 0; index < filterList.size(); index++) {
            CompoundTag row = filterList.getCompoundOrEmpty(index); int slot = row.getIntOr("Slot", 0);
            if (slot >= 0 && slot < filters.size()) filters.set(slot, com.immortalstorage.immortalstorage.compat.mc2612.CompatCodec.parseItemStack(registries, row.getCompoundOrEmpty("Item")));
        }
        filterMatchComponents = tag.getBooleanOr("FilterMatchComponents", false);
        filterWhitelist = !tag.contains("FilterWhitelist") || tag.getBooleanOr("FilterWhitelist", false);
        faceMask = tag.contains("FaceMask") ? tag.getIntOr("FaceMask", 0) & AdvancedRuinScheduler.ALL_FACES : migrateFace(tag);
        linkedPos = tag.contains("LinkedPos") ? BlockPos.of(tag.getLongOr("LinkedPos", 0L)) : null;
    }

    /** Old single-face tag ({@code -1} = any face): {@code -1} becomes all faces, an ordinal becomes its bit. */
    private static int migrateFace(CompoundTag tag) {
        if (!tag.contains("Face")) return AdvancedRuinScheduler.ALL_FACES;
        int value = tag.getIntOr("Face", 0);
        if (value < 0 || value >= Direction.values().length) return AdvancedRuinScheduler.ALL_FACES;
        return 1 << value;
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditionalLegacy(tag, registries);
        tag.remove("Inventory");
        return tag;
    }

    protected void setChangedAndSync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    protected static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
