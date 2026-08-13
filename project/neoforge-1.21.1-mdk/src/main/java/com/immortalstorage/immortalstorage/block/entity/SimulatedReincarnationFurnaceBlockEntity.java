package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.block.custom.SimulatedReincarnationFurnaceBlock;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.item.custom.SoulCatcherItem;
import com.immortalstorage.immortalstorage.item.custom.SpiritDriveItem;
import com.immortalstorage.immortalstorage.menu.custom.SimulatedReincarnationFurnaceMenu;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageNetwork;
import com.immortalstorage.immortalstorage.worldshard.WorldShardOutputRouter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class SimulatedReincarnationFurnaceBlockEntity extends BlockEntity
        implements Container, MenuProvider, ReinforcementPluginHost {
    public static final int SOURCE_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int WEAPON_SLOT = 2;
    public static final int OUTPUT_START = 3;
    public static final int OUTPUT_COUNT = 12;
    public static final int OUTPUT_END = OUTPUT_START + OUTPUT_COUNT;
    public static final int PLUGIN_SLOT = WEAPON_SLOT;
    public static final int SLOT_COUNT = OUTPUT_END;
    public static final int PROCESS_TICKS = 50;
    public static final int DATA_COUNT = 11;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SOURCE_SLOT) return stack.getItem() instanceof SpawnEggItem
                    || stack.getItem() instanceof com.immortalstorage.immortalstorage.item.custom.SoulCatcherItem;
            if (slot == FUEL_SLOT) return stack.is(ModItems.TRUE_YUAN.get())
                    || stack.is(ModItems.IMMORTAL_YUAN.get()) || stack.is(ModItems.SPIRIT_DRIVE.get());
            if (slot == WEAPON_SLOT) return !stack.isEmpty();
            return false;
        }
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (level instanceof ServerLevel serverLevel) {
                setChangedAndMaybeSync(serverLevel);
            }
        }
    };
    private int progress;
    private int burnTicks;
    private int storedExperience;
    private boolean xianqiaoOutput = true;
    private boolean automaticOutput = true;
    private final boolean[] outputFaces = new boolean[Direction.values().length];
    private long completedCycles;
    private long lastSyncTick = Long.MIN_VALUE;
    private long syncInvocationCount;
    private long lastSyncInvocation = Long.MIN_VALUE;
    private ItemStack legacyPluginOverflow = ItemStack.EMPTY;
    private final List<ItemStack> pendingOutput = new ArrayList<>();
    private @Nullable PersonalStorageNetwork.Endpoint cachedOutputEndpoint;
    private @Nullable UUID cachedEndpointOwner;
    private @Nullable ServerPlayer cachedEndpointPlayer;
    private @Nullable net.minecraft.server.MinecraftServer cachedEndpointServer;
    private @Nullable net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> cachedEndpointDimension;
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> burnTicks;
                case 2 -> storedExperience;
                case 3 -> xianqiaoOutput ? 1 : 0;
                case 4 -> automaticOutput ? 1 : 0;
                case 5, 6, 7, 8, 9, 10 -> outputFaces[index - 5] ? 1 : 0;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = Math.max(0, value);
                case 1 -> burnTicks = Math.max(0, value);
                case 2 -> storedExperience = Math.max(0, value);
                case 3 -> xianqiaoOutput = value != 0;
                case 4 -> automaticOutput = value != 0;
                case 5, 6, 7, 8, 9, 10 -> outputFaces[index - 5] = value != 0;
                default -> { }
            }
        }

        @Override public int getCount() { return DATA_COUNT; }
    };

    public SimulatedReincarnationFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIMULATED_REINCARNATION_FURNACE.get(), pos, state);
    }

    public ItemStackHandler itemHandler() { return items; }
    public ContainerData dataAccess() { return data; }
    public int progress() { return progress; }
    public int burnTicks() { return burnTicks; }
    public int storedExperience() { return storedExperience; }
    public boolean xianqiaoOutput() { return xianqiaoOutput; }
    public boolean automaticOutput() { return automaticOutput; }
    public boolean outputFace(Direction side) { return side != null && outputFaces[side.ordinal()]; }
    public void toggleXianqiaoOutput() {
        xianqiaoOutput = !xianqiaoOutput;
        if (level instanceof ServerLevel serverLevel) {
            PersonalStorageNetwork.Endpoint endpoint = outputEndpoint(
                    serverLevel, effectiveXianqiaoOwner(serverLevel));
            if (xianqiaoOutput) {
                flushOutputCacheToXianqiao(endpoint);
            }
            publishPendingOutput(serverLevel, endpoint);
        }
        setChangedAndSync();
    }
    public void toggleAutomaticOutput() { automaticOutput = !automaticOutput; setChangedAndSync(); }
    public void toggleOutputFace(Direction side) {
        if (side == null) return;
        outputFaces[side.ordinal()] = !outputFaces[side.ordinal()];
        setChangedAndSync();
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
                                  SimulatedReincarnationFurnaceBlockEntity furnace) {
        if (!furnace.legacyPluginOverflow.isEmpty()) {
            Block.popResource(level, pos, furnace.legacyPluginOverflow);
            furnace.legacyPluginOverflow = ItemStack.EMPTY;
            furnace.setChanged();
        }
        // Do not wait for another simulated kill: existing output buffers are
        // drained to every configured face and then to the owner's Xianqiao.
        boolean changed = furnace.pushCachedToFaces(level);
        UUID outputOwner = furnace.effectiveXianqiaoOwner(level);
        PersonalStorageNetwork.Endpoint endpoint = furnace.outputEndpoint(level, outputOwner);
        changed |= furnace.flushOutputCacheToXianqiao(endpoint);
        boolean hadPendingOutput = !furnace.pendingOutput.isEmpty();
        boolean pendingSettled = furnace.publishPendingOutput(level, endpoint);
        changed |= hadPendingOutput && pendingSettled;
        if (!pendingSettled) {
            furnace.progress = 0;
            furnace.updateWorkingState(level, pos, state, false);
            furnace.setChangedAndMaybeSync(level);
            return;
        }
        if (!furnace.hasSpecimenSource() || !furnace.ensureFuel(level)) {
            furnace.progress = 0;
            furnace.updateWorkingState(level, pos, state, false);
            if (changed) furnace.setChangedAndMaybeSync(level);
            return;
        }
        furnace.updateWorkingState(level, pos, state, true);
        furnace.burnTicks--;
        if (++furnace.progress < PROCESS_TICKS) {
            if (changed) furnace.setChangedAndMaybeSync(level);
            return;
        }
        furnace.progress = 0;
        LivingEntity specimen = furnace.createSpecimen(level);
        if (specimen == null) {
            furnace.updateWorkingState(level, pos, state, false);
            furnace.setChangedAndMaybeSync(level);
            return;
        }
        furnace.produce(level, specimen, outputOwner, endpoint);
    }

    private boolean pushCachedToFaces(ServerLevel level) {
        return MachineOutputScheduler.pushItemsToFaces(
                level, worldPosition, automaticOutput, outputFaces,
                items, OUTPUT_START, OUTPUT_END);
    }

    private boolean flushOutputCacheToXianqiao(@Nullable PersonalStorageNetwork.Endpoint endpoint) {
        return MachineOutputScheduler.flushItemsToXianqiao(
                items, OUTPUT_START, OUTPUT_END, endpoint);
    }

    private @Nullable PersonalStorageNetwork.Endpoint outputEndpoint(
            ServerLevel level, @Nullable UUID outputOwner) {
        if (outputOwner == null) return null;
        net.minecraft.server.MinecraftServer server = level.getServer();
        ServerPlayer player = com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity
                .onlinePlayer(server, outputOwner);
        if (player == null) {
            cachedOutputEndpoint = null;
            cachedEndpointOwner = null;
            cachedEndpointPlayer = null;
            return null;
        }
        if (outputOwner.equals(cachedEndpointOwner)
                && player == cachedEndpointPlayer
                && server == cachedEndpointServer
                && level.dimension().equals(cachedEndpointDimension)
                && cachedOutputEndpoint != null
                && cachedOutputEndpoint.online()
                && outputOwner.equals(cachedOutputEndpoint.owner())
                && cachedOutputEndpoint.stage() >= 6
                && (!(cachedOutputEndpoint
                instanceof PersonalStorageNetwork.Endpoint concrete)
                || concrete.data() == com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData.get(player))) {
            return cachedOutputEndpoint;
        }
        PersonalStorageNetwork.Endpoint resolved = ImmortalStorageDimensions.isPersonalRealmFor(
                level.dimension(), outputOwner)
                ? PersonalStorageNetwork.resolveInOwnerRealm(level, outputOwner, this::setChanged)
                : PersonalStorageNetwork.resolve(level.getServer(), outputOwner, this::setChanged);
        cachedOutputEndpoint = resolved;
        cachedEndpointOwner = outputOwner;
        cachedEndpointPlayer = player;
        cachedEndpointServer = server;
        cachedEndpointDimension = level.dimension();
        return resolved;
    }

    private void updateWorkingState(ServerLevel level, BlockPos pos, BlockState state, boolean working) {
        if (state.hasProperty(SimulatedReincarnationFurnaceBlock.LIT)
                && state.getValue(SimulatedReincarnationFurnaceBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(SimulatedReincarnationFurnaceBlock.LIT, working), 3);
        }
    }

    private boolean ensureFuel(ServerLevel level) {
        if (burnTicks > 0) return true;
        ItemStack fuel = items.getStackInSlot(FUEL_SLOT);
        if (fuel.is(ModItems.TRUE_YUAN.get())) {
            items.extractItem(FUEL_SLOT, 1, false); burnTicks = 50; return true;
        }
        if (fuel.is(ModItems.IMMORTAL_YUAN.get())) {
            items.extractItem(FUEL_SLOT, 1, false); burnTicks = 500; return true;
        }
        XianqiaoBindingPolicy.Binding binding = XianqiaoBindingPolicy.resolve(level, fuel);
        UUID payer = binding.isBound() ? binding.owner() : null;
        ServerPlayer ownerPlayer = payer == null ? null
                : com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(level.getServer(), payer);
        if (ownerPlayer != null) {
            var data = com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData.get(ownerPlayer);
            if (data.consumeImmortalYuan(1L)) { burnTicks = 500; return true; }
            if (binding.source() == XianqiaoBindingPolicy.BindingSource.SPIRIT_DRIVE
                    && data.consumeTrueYuan(1L)) { burnTicks = 50; return true; }
        }
        return false;
    }

    private @Nullable LivingEntity createSpecimen(ServerLevel level) {
        ItemStack source = items.getStackInSlot(SOURCE_SLOT);
        Entity entity = null;
        if (source.getItem() instanceof SpawnEggItem egg) {
            entity = egg.getType(source).create(level);
        } else if (source.getItem() instanceof com.immortalstorage.immortalstorage.item.custom.SoulCatcherItem
                && SoulCatcherItem.hasEntity(source)) {
            entity = EntityType.loadEntityRecursive(SoulCatcherItem.containedEntity(source), level, loaded -> loaded);
        }
        return entity instanceof LivingEntity living ? living : null;
    }

    private boolean hasSpecimenSource() {
        ItemStack source = items.getStackInSlot(SOURCE_SLOT);
        return source.getItem() instanceof SpawnEggItem
                || source.getItem() instanceof SoulCatcherItem && SoulCatcherItem.hasEntity(source);
    }

    public @Nullable LivingEntity createDisplayEntity(net.minecraft.world.level.Level displayLevel) {
        ItemStack source = items.getStackInSlot(SOURCE_SLOT);
        Entity entity = null;
        if (source.getItem() instanceof SpawnEggItem egg) {
            entity = egg.getType(source).create(displayLevel);
        } else if (source.getItem() instanceof SoulCatcherItem && SoulCatcherItem.hasEntity(source)) {
            entity = EntityType.loadEntityRecursive(SoulCatcherItem.containedEntity(source), displayLevel, loaded -> loaded);
        }
        return entity instanceof LivingEntity living ? living : null;
    }

    private void produce(ServerLevel level, LivingEntity specimen,
                         @Nullable UUID outputOwner,
                         @Nullable PersonalStorageNetwork.Endpoint endpoint) {
        ServerPlayer killer = outputOwner == null ? null
                : com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(level.getServer(), outputOwner);
        if (killer == null) killer = level.getNearestPlayer(worldPosition.getX(), worldPosition.getY(),
                worldPosition.getZ(), 32.0D, false) instanceof ServerPlayer nearby ? nearby : null;
        ItemStack weapon = items.getStackInSlot(WEAPON_SLOT).copyWithCount(1);
        net.minecraft.world.damagesource.DamageSource damage = killer == null
                ? level.damageSources().generic() : killer.damageSources().playerAttack(killer);
        net.minecraft.world.level.storage.loot.LootParams.Builder params =
                new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, specimen)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                                worldPosition.getCenter())
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, damage)
                        .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY, killer)
                        .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                        .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER, killer)
                        .withLuck(killer == null ? 0.0F : killer.getLuck());
        var table = level.getServer().reloadableRegistries().getLootTable(specimen.getLootTable());
        ItemStack previous = killer == null ? ItemStack.EMPTY : killer.getMainHandItem().copy();
        if (killer != null && !weapon.isEmpty()) {
            killer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
        }
        List<ItemStack> drops;
        try {
            drops = table.getRandomItems(params.create(
                    net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY));
        } finally {
            if (killer != null && !weapon.isEmpty()) {
                killer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, previous);
            }
        }
        int processMultiplier = Math.max(1, items.getStackInSlot(SOURCE_SLOT).getCount())
                * reinforcementMultiplier();
        drops = multiplyDrops(drops, processMultiplier);
        int xp = (int) Math.min(Integer.MAX_VALUE,
                (long) Math.max(0, specimen.getExperienceReward(level, killer)) * processMultiplier);
        if (outputOwner != null && killer != null) {
            killer.giveExperiencePoints(xp);
        } else {
            storedExperience = Math.min(Integer.MAX_VALUE, storedExperience + xp);
        }
        queueOutput(level, endpoint, drops);
        if (killer != null) {
            com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers.SIMULATED_KILL.trigger(killer);
            completedCycles++;
            if (completedCycles >= 10L) {
                com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers
                        .SIMULATED_KILL_TEN.trigger(killer);
            }
        }
        setChanged();
    }

    private @Nullable UUID effectiveXianqiaoOwner(ServerLevel level) {
        if (!xianqiaoOutput) return null;
        UUID boundOwner = XianqiaoBindingPolicy.resolve(level, items.getStackInSlot(FUEL_SLOT)).owner();
        return boundOwner != null && com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity
                .onlinePlayer(level.getServer(), boundOwner) != null ? boundOwner : null;
    }

    private void queueOutput(
            ServerLevel level,
            @Nullable PersonalStorageNetwork.Endpoint endpoint,
            List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) pendingOutput.add(drop.copy());
        }
        publishPendingOutput(level, endpoint);
    }

    /**
     * Settles an already completed kill batch. Xianqiao output is an atomic
     * direct transaction and therefore never observes local cache capacity or
     * creates overflow entities. Rejection leaves the complete batch pending.
     */
    private boolean publishPendingOutput(
            ServerLevel level,
            @Nullable PersonalStorageNetwork.Endpoint endpoint) {
        if (pendingOutput.isEmpty()) return true;
        if (xianqiaoOutput) {
            WorldShardOutputRouter.RouteResult route =
                    WorldShardOutputRouter.routeDirect(pendingOutput, endpoint);
            if (route.unaccepted() > 0L) return false;
            pendingOutput.clear();
            setChanged();
            return true;
        }
        return flushPendingOutputToCache(level);
    }

    /**
     * Fills the visible cache first and then enabled accepting faces. Any
     * exact remainder stays in the persisted pending list, and the machine
     * cannot begin another cycle until that list has been drained completely.
     */
    private boolean flushPendingOutputToCache(ServerLevel level) {
        if (pendingOutput.isEmpty()) return true;
        List<ItemStack> temporary = new ArrayList<>();
        for (ItemStack queued : List.copyOf(pendingOutput)) {
            ItemStack remaining = MachineOutputScheduler.insertIntoInternalSlots(
                    items, OUTPUT_START, OUTPUT_END, queued);
            if (!remaining.isEmpty()) {
                remaining = MachineOutputScheduler.pushItemToFaces(
                        level, worldPosition, automaticOutput, outputFaces, remaining);
                if (!remaining.isEmpty()) temporary.add(remaining.copy());
            }
        }
        pendingOutput.clear();
        pendingOutput.addAll(temporary);
        setChanged();
        return pendingOutput.isEmpty();
    }

    private static List<ItemStack> multiplyDrops(List<ItemStack> source, int multiplier) {
        java.util.ArrayList<ItemStack> result = new java.util.ArrayList<>();
        for (ItemStack stack : source) {
            long remaining = Math.min(Integer.MAX_VALUE, (long) stack.getCount() * multiplier);
            while (remaining > 0L) {
                int count = (int) Math.min(stack.getMaxStackSize(), remaining);
                result.add(stack.copyWithCount(count));
                remaining -= count;
            }
        }
        return result;
    }

    public void releaseExperience(ServerPlayer player) {
        if (storedExperience <= 0) return;
        player.giveExperiencePoints(storedExperience);
        storedExperience = 0;
        setChangedAndSync();
    }

    public void dropAsItem(ServerPlayer player) {
        ItemStack dropped = new ItemStack(getBlockState().getBlock());
        dropped.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(saveWithFullMetadata(player.registryAccess())));
        net.minecraft.world.level.block.Block.popResource(player.level(), worldPosition, dropped);
        player.level().removeBlock(worldPosition, false);
    }

    private void setChangedAndSync() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            lastSyncTick = serverLevel.getGameTime();
            lastSyncInvocation = ++syncInvocationCount;
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            broadcastOpenMenu(serverLevel);
        }
    }

    private void setChangedAndMaybeSync(ServerLevel serverLevel) {
        setChanged();
        long invocation = ++syncInvocationCount;
        if (!MachineTickSync.due(serverLevel, lastSyncTick, invocation, lastSyncInvocation)) return;
        lastSyncTick = serverLevel.getGameTime();
        lastSyncInvocation = invocation;
        serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        broadcastOpenMenu(serverLevel);
    }

    private void broadcastOpenMenu(ServerLevel serverLevel) {
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (player.level() == serverLevel
                    && player.containerMenu instanceof SimulatedReincarnationFurnaceMenu menu
                    && menu.blockPos().equals(worldPosition)) {
                menu.broadcastChanges();
            }
        }
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.immortalstorage.simulated_reincarnation_furnace");
    }
    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(saveWithFullMetadata(registries)));
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SimulatedReincarnationFurnaceMenu(id, inventory, this);
    }
    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() {
        if (!pendingOutput.isEmpty()) return false;
        for (int i=0;i<SLOT_COUNT;i++) if(!getItem(i).isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) { return items.getStackInSlot(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return items.extractItem(slot, amount, false); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return items.extractItem(slot, Integer.MAX_VALUE, false); }
    @Override public void setItem(int slot, ItemStack stack) { items.setStackInSlot(slot, stack); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return items.isItemValid(slot, stack); }
    @Override public ItemStack reinforcementPlugin() { return items.getStackInSlot(PLUGIN_SLOT); }
    @Override public void setReinforcementPlugin(ItemStack stack) {
        items.setStackInSlot(PLUGIN_SLOT, stack.copyWithCount(1));
        setChangedAndSync();
    }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() {
        for(int i=0;i<SLOT_COUNT;i++) items.setStackInSlot(i, ItemStack.EMPTY);
        pendingOutput.clear();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Progress", progress); tag.putInt("BurnTicks", burnTicks);
        tag.putInt("StoredExperience", storedExperience);
        tag.putBoolean("XianqiaoOutput", xianqiaoOutput);
        tag.putBoolean("AutomaticFaceOutput", automaticOutput);
        int[] faces = new int[outputFaces.length];
        for (Direction side : Direction.values()) faces[side.ordinal()] = outputFaces[side.ordinal()] ? 1 : 0;
        tag.putIntArray("OutputFaces", faces);
        tag.putLong("CompletedCycles", completedCycles);
        if (!legacyPluginOverflow.isEmpty()) {
            tag.put("LegacyPluginOverflow", legacyPluginOverflow.save(registries));
        }
        if (!pendingOutput.isEmpty()) {
            ListTag pending = new ListTag();
            for (ItemStack stack : pendingOutput) pending.add(stack.save(registries));
            tag.put("PendingOutput", pending);
        }
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag savedItems = tag.getCompound("Items").copy();
        int savedSize = Math.max(SLOT_COUNT, savedItems.getInt("Size"));
        ItemStackHandler loaded = new ItemStackHandler(savedSize);
        loaded.deserializeNBT(registries, savedItems);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            items.setStackInSlot(slot, loaded.getStackInSlot(slot));
        }
        legacyPluginOverflow = tag.contains("LegacyPluginOverflow")
                ? ItemStack.parseOptional(registries, tag.getCompound("LegacyPluginOverflow"))
                : ItemStack.EMPTY;
        pendingOutput.clear();
        ListTag pending = tag.getList("PendingOutput", Tag.TAG_COMPOUND);
        for (int index = 0; index < pending.size(); index++) {
            ItemStack stack = ItemStack.parseOptional(registries, pending.getCompound(index));
            if (!stack.isEmpty()) pendingOutput.add(stack);
        }
        if (savedSize > OUTPUT_END) {
            migrateLegacyPlugin(loaded.getStackInSlot(OUTPUT_END));
        }
        progress=tag.getInt("Progress"); burnTicks=tag.getInt("BurnTicks"); storedExperience=tag.getInt("StoredExperience");
        completedCycles=Math.max(0L, tag.getLong("CompletedCycles"));
        xianqiaoOutput = tag.contains("XianqiaoOutput")
                ? tag.getBoolean("XianqiaoOutput")
                : !tag.contains("AutomaticOutput") || tag.getBoolean("AutomaticOutput");
        automaticOutput = !tag.contains("AutomaticFaceOutput") || tag.getBoolean("AutomaticFaceOutput");
        Arrays.fill(outputFaces, false);
        int[] faces = tag.getIntArray("OutputFaces");
        for (int i = 0; i < Math.min(faces.length, outputFaces.length); i++) outputFaces[i] = faces[i] != 0;
    }

    private void migrateLegacyPlugin(ItemStack legacy) {
        if (!ReinforcementPluginHost.isPlugin(legacy)) return;
        if (items.getStackInSlot(PLUGIN_SLOT).isEmpty()) {
            items.setStackInSlot(PLUGIN_SLOT, legacy.copyWithCount(1));
            return;
        }
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            if (items.getStackInSlot(slot).isEmpty()) {
                items.setStackInSlot(slot, legacy.copyWithCount(1));
                return;
            }
        }
        legacyPluginOverflow = legacy.copyWithCount(1);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
