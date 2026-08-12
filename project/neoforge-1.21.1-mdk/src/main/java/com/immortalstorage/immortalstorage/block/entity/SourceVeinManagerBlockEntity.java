package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.api.source.SourceChargeContext;
import com.immortalstorage.immortalstorage.api.source.SourceChargePlan;
import com.immortalstorage.immortalstorage.api.source.SourceChargeRegistry;
import com.immortalstorage.immortalstorage.api.source.SourceChargeReservation;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinition;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinitions;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.network.storage.SourceVeinStorageIndex;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Owner-bound source member aggregator. Its member slots are intentionally not a block capability. */
public final class SourceVeinManagerBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final String MEMBERS_TAG = "Members";
    private static final String OWNER_TAG = "Owner";

    private final OwnerBinding ownerBinding = new OwnerBinding();
    private final SourceVeinManagerInventory members = new SourceVeinManagerInventory(this::membersChanged);
    private final SourceVeinManagerDisplayState displayState = new SourceVeinManagerDisplayState();
    private final IItemHandler productItemHandler = new AggregatedItemHandler();
    private final IFluidHandler productFluidHandler = new AggregatedFluidHandler();
    private boolean loading;
    private long observedDefinitionGeneration = -1L;

    public SourceVeinManagerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOURCE_VEIN_MANAGER.get(), pos, state);
    }

    SourceVeinManagerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean tryClaimOwner(@Nullable ServerPlayer player) {
        if (player == null || player.serverLevel() != level
                || ImmortalStoragePlayerData.get(player).getStage() < 6
                || !com.immortalstorage.immortalstorage.dimension.RealmHelper.isInOwnRealm(player)) return false;
        UUID before = ownerBinding.owner();
        boolean accepted = ownerBinding.claim(player);
        if (accepted && !java.util.Objects.equals(before, ownerBinding.owner())) membersChanged();
        return accepted;
    }

    public static boolean canPlaceStackFor(ItemStack stack, @Nullable UUID placer) {
        if (stack == null || placer == null) return false;
        CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (data.isEmpty()) return true;
        CompoundTag tag = data.copyTag();
        return !tag.contains(OWNER_TAG) || tag.hasUUID(OWNER_TAG) && placer.equals(tag.getUUID(OWNER_TAG));
    }

    public @Nullable UUID getOwner() { return ownerBinding.owner(); }
    public SourceVeinManagerInventory members() { return members; }
    public int displayState() { return displayState.state(); }
    public int memberSlots() { return SourceVeinManagerInventory.SLOT_COUNT; }
    public boolean hasMember(int slot) { return members.isActiveMember(slot) && memberDefinition(slot) != null; }
    public boolean memberIsFluid(int slot) { SourceDefinition definition = memberDefinition(slot); return definition != null && definition.fluid(); }
    public boolean memberIsFree(int slot) { SourceDefinition definition = memberDefinition(slot); return definition != null && definition.free(); }
    public long memberCachedUnits(int slot) { return SourceVeinManagerInventory.cachedUnits(memberStack(slot)); }

    public synchronized long memberAvailableUnits(int slot) {
        SourceDefinition definition = memberDefinition(slot);
        if (definition == null || !isMemberVisible(slot, getOwner())) return 0L;
        if (definition.free()) return Long.MAX_VALUE;
        long cached = memberCachedUnits(slot);
        if (!(level instanceof ServerLevel serverLevel) || getOwner() == null) return cached;
        ServerPlayer player = com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(serverLevel.getServer(), getOwner());
        if (player == null) return cached;
        SourceChargePlan plan = chargePlan(definition);
        long convertible = SourceVeinBuffer.affordableRefill(Long.MAX_VALUE, 0L,
                ImmortalStoragePlayerData.get(player).getImmortalYuan(),
                plan.unitsPerBatch(), plan.outputsPerBatch());
        return Long.MAX_VALUE - cached < convertible ? Long.MAX_VALUE : cached + convertible;
    }

    public ItemStack memberItemPrototype(int slot) {
        SourceDefinition definition = memberDefinition(slot);
        if (definition == null || definition.fluid()) return ItemStack.EMPTY;
        var item = BuiltInRegistries.ITEM.get(definition.outputId());
        return item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item, 1);
    }

    public FluidStack memberFluidPrototype(int slot) {
        SourceDefinition definition = memberDefinition(slot);
        Fluid fluid = definition == null || !definition.fluid()
                ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(definition.outputId());
        return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, 1);
    }

    /** The 72 member slots stay private; this capability exposes products only. */
    public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
        return hasProductChannel(false) ? productItemHandler : null;
    }

    /** All physical faces expose the same passive, extract-only fluid view. */
    public @Nullable IFluidHandler getFluidHandler(@Nullable Direction side) {
        return hasProductChannel(true) ? productFluidHandler : null;
    }

    public boolean isMemberVisible(int slot, @Nullable UUID requestedOwner) {
        UUID owner = getOwner();
        if (owner == null || !owner.equals(requestedOwner) || isRemoved()
                || !(level instanceof ServerLevel serverLevel)
                || !ImmortalStorageDimensions.isPersonalRealmFor(serverLevel.dimension(), owner)
                || !members.isActiveMember(slot)) return false;
        SourceDefinition definition = memberDefinition(slot);
        ServerPlayer player = com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(serverLevel.getServer(), owner);
        return definition != null && player != null
                && ImmortalStoragePlayerData.get(player).getStage() >= Math.max(6, definition.minStage());
    }

    public synchronized long extractMember(int slot, long requested, boolean simulate) {
        if (!members.isActiveMember(slot)) return 0L;
        SourceDefinition definition = memberDefinition(slot);
        if (definition == null || requested <= 0L || !isMemberVisible(slot, getOwner())) return 0L;
        return extractMemberCache(slot, definition, requested, simulate);
    }

    private long extractMemberCache(int slot, SourceDefinition definition, long requested, boolean simulate) {
        long cached = memberCachedUnits(slot);
        if (definition.free()) return requested;
        long fromCache = Math.min(requested, cached);
        long deficit = requested - fromCache;
        if (deficit <= 0L) {
            if (!simulate && fromCache > 0L) {
                SourceVeinManagerInventory.setCachedUnits(memberStack(slot), cached - fromCache);
                cacheChanged(!definition.fluid(), definition.fluid());
            }
            return fromCache;
        }
        if (!(level instanceof ServerLevel serverLevel) || getOwner() == null) {
            if (!simulate && fromCache > 0L) {
                SourceVeinManagerInventory.setCachedUnits(memberStack(slot), cached - fromCache);
                cacheChanged(!definition.fluid(), definition.fluid());
            }
            return fromCache;
        }
        SourceChargePlan plan = chargePlan(definition);
        SourceChargeContext context = new SourceChargeContext(serverLevel, worldPosition, getOwner());
        if (simulate) return SourceChargeRegistry.canReserve(plan, context, deficit) ? requested : fromCache;

        SourceChargeReservation reservation = SourceChargeRegistry.reserve(plan, context, deficit);
        if (reservation == null) {
            if (fromCache > 0L) {
                SourceVeinManagerInventory.setCachedUnits(memberStack(slot), cached - fromCache);
                cacheChanged(!definition.fluid(), definition.fluid());
            }
            return fromCache;
        }
        if (fromCache > 0L) {
            SourceVeinManagerInventory.setCachedUnits(memberStack(slot), cached - fromCache);
            cacheChanged(!definition.fluid(), definition.fluid());
        }
        if (!reservation.commit(deficit)) {
            if (fromCache > 0L) {
                SourceVeinManagerInventory.setCachedUnits(memberStack(slot), cached);
                cacheChanged(!definition.fluid(), definition.fluid());
            }
            return 0L;
        }
        return requested;
    }

    private static SourceChargePlan chargePlan(SourceDefinition definition) {
        return new SourceChargePlan(SourceChargeRegistry.IMMORTAL_YUAN,
                definition.yuanCostPerBatch(), definition.outputsPerBatch());
    }

    public @Nullable MinecraftServer server() {
        return level instanceof ServerLevel serverLevel ? serverLevel.getServer() : null;
    }

    public String storageIndexPrefix() {
        String dimension = level instanceof ServerLevel serverLevel
                ? serverLevel.dimension().location().toString() : "unbound";
        return "manager:" + dimension + ":" + worldPosition.asLong() + ":";
    }

    @Override public int getContainerSize() { return memberSlots(); }
    @Override public boolean isEmpty() {
        for (int slot = 0; slot < memberSlots(); slot++) if (hasMember(slot)) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) { return memberStack(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        return members.extractItem(slot, Math.min(1, amount), false);
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = members.extractItem(slot, 1, false);
        return removed;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= memberSlots()) return;
        if (stack == null || stack.isEmpty()) {
            members.setStackInSlot(slot, ItemStack.EMPTY);
            return;
        }
        if (stack.getCount() != 1 || !members.isItemValid(slot, stack)) return;
        members.setStackInSlot(slot, stack.copyWithCount(1));
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return members.isItemValid(slot, stack); }
    @Override public boolean stillValid(Player player) {
        return level == player.level() && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D
                && com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.matches(player, getOwner());
    }
    @Override public void clearContent() {
        for (int slot = 0; slot < memberSlots(); slot++) members.setStackInSlot(slot, ItemStack.EMPTY);
    }
    @Override public Component getDisplayName() {
        return Component.translatable("block.immortalstorage.source_vein_manager");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        if (!stillValid(player)) return null;
        return new com.immortalstorage.immortalstorage.menu.custom.SourceVeinManagerMenu(id, inventory, this);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, SourceVeinManagerBlockEntity manager) {
        if (manager.getOwner() == null) return;
        long definitionGeneration = SourceDefinitions.generation();
        if (manager.observedDefinitionGeneration != definitionGeneration) {
            manager.observedDefinitionGeneration = definitionGeneration;
            manager.definitionsChanged();
        }
        boolean itemChanged = false;
        boolean fluidChanged = false;
        for (int slot = 0; slot < manager.memberSlots(); slot++) {
            if (!manager.members.isActiveMember(slot)) continue;
            SourceDefinition definition = manager.memberDefinition(slot);
            if (definition == null) continue;
            ItemStack member = manager.memberStack(slot);
            long before = SourceVeinManagerInventory.cachedUnits(member);
            if (definition.free()) {
                SourceVeinManagerInventory.reconcileMemberCache(member, definition, Long.MAX_VALUE);
            }
            if (before != SourceVeinManagerInventory.cachedUnits(member)) {
                if (definition.fluid()) fluidChanged = true;
                else itemChanged = true;
            }
        }
        manager.cacheChanged(itemChanged, fluidChanged);
    }

    private ItemStack memberStack(int slot) {
        return slot < 0 || slot >= memberSlots() ? ItemStack.EMPTY : members.getStackInSlot(slot);
    }

    private @Nullable SourceDefinition memberDefinition(int slot) {
        return SourceVeinManagerInventory.sourceDefinition(memberStack(slot));
    }

    private boolean hasProductChannel(boolean fluid) {
        return productCount(fluid) > 0;
    }

    private int productCount(boolean fluid) {
        int count = 0;
        for (int memberSlot = 0; memberSlot < memberSlots(); memberSlot++) {
            SourceDefinition definition = memberDefinition(memberSlot);
            if (members.isActiveMember(memberSlot) && definition != null && definition.fluid() == fluid) count++;
        }
        return count;
    }

    private int memberSlotForProduct(int productSlot, boolean fluid) {
        if (productSlot < 0) return -1;
        int current = 0;
        for (int memberSlot = 0; memberSlot < memberSlots(); memberSlot++) {
            SourceDefinition definition = memberDefinition(memberSlot);
            if (!members.isActiveMember(memberSlot) || definition == null || definition.fluid() != fluid) continue;
            if (current++ == productSlot) return memberSlot;
        }
        return -1;
    }

    private final class AggregatedItemHandler implements IItemHandler {
        @Override public int getSlots() { return productCount(false); }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            int memberSlot = memberSlotForProduct(slot, false);
            if (memberSlot < 0) return ItemStack.EMPTY;
            ItemStack prototype = memberItemPrototype(memberSlot);
            SourceDefinition definition = memberDefinition(memberSlot);
            long cached = memberCachedUnits(memberSlot);
            if (prototype.isEmpty() || definition == null || cached <= 0L) return ItemStack.EMPTY;
            int visible = (int) Math.min(Integer.MAX_VALUE, cached);
            return prototype.copyWithCount(visible);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            int memberSlot = memberSlotForProduct(slot, false);
            SourceDefinition definition = memberSlot < 0 ? null : memberDefinition(memberSlot);
            ItemStack prototype = memberSlot < 0 ? ItemStack.EMPTY : memberItemPrototype(memberSlot);
            if (definition == null || prototype.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            int request = Math.min(amount, prototype.getMaxStackSize());
            int extracted = (int) extractMemberCache(memberSlot, definition, request, simulate);
            return extracted <= 0 ? ItemStack.EMPTY : prototype.copyWithCount(extracted);
        }

        @Override public int getSlotLimit(int slot) {
            return memberSlotForProduct(slot, false) < 0 ? 0 : Integer.MAX_VALUE;
        }

        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    }

    private final class AggregatedFluidHandler implements IFluidHandler {
        @Override public int getTanks() { return productCount(true); }

        @Override
        public FluidStack getFluidInTank(int tank) {
            int memberSlot = memberSlotForProduct(tank, true);
            if (memberSlot < 0) return FluidStack.EMPTY;
            FluidStack prototype = memberFluidPrototype(memberSlot);
            int visible = (int) Math.min(Integer.MAX_VALUE, memberCachedUnits(memberSlot));
            return prototype.isEmpty() || visible <= 0 ? FluidStack.EMPTY : prototype.copyWithAmount(visible);
        }

        @Override public int getTankCapacity(int tank) {
            return memberSlotForProduct(tank, true) < 0 ? 0 : Integer.MAX_VALUE;
        }

        @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }

        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return FluidStack.EMPTY;
            return drainMatching(resource.getAmount(), resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            for (int memberSlot = 0; memberSlot < memberSlots(); memberSlot++) {
                FluidStack prototype = memberFluidPrototype(memberSlot);
                if (members.isActiveMember(memberSlot) && !prototype.isEmpty()
                        && memberCachedUnits(memberSlot) > 0L) {
                    return drainMatching(maxDrain, prototype, action);
                }
            }
            return FluidStack.EMPTY;
        }

        private FluidStack drainMatching(int requested, FluidStack identity, FluidAction action) {
            int request = Math.max(0, requested);
            for (int memberSlot = 0; memberSlot < memberSlots() && request > 0; memberSlot++) {
                if (!members.isActiveMember(memberSlot)) continue;
                SourceDefinition definition = memberDefinition(memberSlot);
                FluidStack prototype = memberFluidPrototype(memberSlot);
                if (definition == null || !definition.fluid() || prototype.isEmpty()
                        || !FluidStack.isSameFluidSameComponents(prototype, identity)) continue;
                int extracted = (int) extractMemberCache(memberSlot, definition, request, action.simulate());
                return extracted <= 0 ? FluidStack.EMPTY : prototype.copyWithAmount(extracted);
            }
            return FluidStack.EMPTY;
        }
    }

    private void membersChanged() {
        if (loading) return;
        boolean displayChanged = displayState.refreshFrom(this);
        setChanged();
        SourceVeinStorageIndex.register(this);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.invalidateCapabilities(worldPosition);
            if (displayChanged) {
                BlockState state = getBlockState();
                serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    private void definitionsChanged() {
        if (loading) return;
        boolean displayChanged = displayState.refreshFrom(this);
        setChanged();
        SourceVeinStorageIndex.register(this);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.invalidateCapabilities(worldPosition);
            if (displayChanged) {
                BlockState state = getBlockState();
                serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    private void cacheChanged(boolean itemChanged, boolean fluidChanged) {
        if (loading || !itemChanged && !fluidChanged) return;
        setChanged();
        SourceVeinStorageIndex.changed(this, itemChanged, fluidChanged);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ownerBinding.save(tag, OWNER_TAG);
        tag.put(MEMBERS_TAG, members.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loading = true;
        ownerBinding.load(tag, OWNER_TAG);
        if (tag.contains(MEMBERS_TAG)) {
            members.deserializeNBT(registries, tag.getCompound(MEMBERS_TAG));
            members.reconcileLoadedMembers();
            displayState.refreshFrom(this);
        }
        if (tag.contains(SourceVeinManagerDisplayState.TAG)) displayState.load(tag);
        loading = false;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this,
                (blockEntity, registryAccess) -> ((SourceVeinManagerBlockEntity) blockEntity)
                        .displayState.save(new CompoundTag()));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return displayState.save(new CompoundTag());
    }

    @Override public void onLoad() {
        super.onLoad();
        observedDefinitionGeneration = SourceDefinitions.generation();
        // Client chunks receive only the compact DisplayState update packet;
        // their private member inventory is intentionally absent. Recomputing
        // from that empty client inventory here erased the real occupancy on
        // every world reload until the menu was opened. The server owns the
        // members, so only it may derive the state from the source inventory.
        if (level instanceof ServerLevel) displayState.refreshFrom(this);
        SourceVeinStorageIndex.register(this);
    }
    @Override public void setRemoved() { SourceVeinStorageIndex.unregister(this); super.setRemoved(); }

}
