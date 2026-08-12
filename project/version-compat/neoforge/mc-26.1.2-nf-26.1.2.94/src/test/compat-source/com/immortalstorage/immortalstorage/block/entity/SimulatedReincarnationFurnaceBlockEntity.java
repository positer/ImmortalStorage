package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.block.custom.SimulatedReincarnationFurnaceBlock;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.item.custom.SoulCatcherItem;
import com.immortalstorage.immortalstorage.item.custom.SpiritDriveItem;
import com.immortalstorage.immortalstorage.menu.custom.SimulatedReincarnationFurnaceMenu;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class SimulatedReincarnationFurnaceBlockEntity extends com.immortalstorage.immortalstorage.compat.mc2612.CompatBlockEntity
        implements Container, MenuProvider {
    public static final int SOURCE_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int WEAPON_SLOT = 2;
    public static final int OUTPUT_START = 3;
    public static final int OUTPUT_COUNT = 12;
    public static final int SLOT_COUNT = OUTPUT_START + OUTPUT_COUNT;
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
        if (xianqiaoOutput && level instanceof ServerLevel serverLevel) {
            flushOutputCacheToXianqiao(outputEndpoint(serverLevel, effectiveXianqiaoOwner(serverLevel)));
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
        // Do not wait for another simulated kill: existing output buffers are
        // drained to every configured face and then to the owner's Xianqiao.
        boolean changed = furnace.pushCachedToFaces(level);
        UUID outputOwner = furnace.effectiveXianqiaoOwner(level);
        PersonalStorageNetwork.Endpoint endpoint = furnace.outputEndpoint(level, outputOwner);
        changed |= furnace.flushOutputCacheToXianqiao(endpoint);
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
                items, OUTPUT_START, SLOT_COUNT);
    }

    private boolean flushOutputCacheToXianqiao(@Nullable PersonalStorageNetwork.Endpoint endpoint) {
        return MachineOutputScheduler.flushItemsToXianqiao(
                items, OUTPUT_START, SLOT_COUNT, endpoint);
    }

    private @Nullable PersonalStorageNetwork.Endpoint outputEndpoint(
            ServerLevel level, @Nullable UUID outputOwner) {
        if (outputOwner == null) return null;
        net.minecraft.server.MinecraftServer server = com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level);
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
                : PersonalStorageNetwork.resolve(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level), outputOwner, this::setChanged);
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
                : com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level), payer);
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
            entity = egg.getType(source).create(level, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
        } else if (source.getItem() instanceof com.immortalstorage.immortalstorage.item.custom.SoulCatcherItem
                && SoulCatcherItem.hasEntity(source)) {
            entity = EntityType.loadEntityRecursive(SoulCatcherItem.containedEntity(source), level, net.minecraft.world.entity.EntitySpawnReason.LOAD, loaded -> loaded);
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
            entity = egg.getType(source).create(displayLevel, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
        } else if (source.getItem() instanceof SoulCatcherItem && SoulCatcherItem.hasEntity(source)) {
            entity = EntityType.loadEntityRecursive(SoulCatcherItem.containedEntity(source), displayLevel, net.minecraft.world.entity.EntitySpawnReason.LOAD, loaded -> loaded);
        }
        return entity instanceof LivingEntity living ? living : null;
    }

    private void produce(ServerLevel level, LivingEntity specimen,
                         @Nullable UUID outputOwner,
                         @Nullable PersonalStorageNetwork.Endpoint endpoint) {
        ServerPlayer killer = outputOwner == null ? null
                : com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level), outputOwner);
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
        var table = com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level).reloadableRegistries().getLootTable(specimen.getLootTable().orElseThrow());
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
        int xp = Math.max(0, specimen.getExperienceReward(level, killer));
        if (outputOwner != null && killer != null) {
            killer.giveExperiencePoints(xp);
        } else {
            storedExperience = Math.min(Integer.MAX_VALUE, storedExperience + xp);
        }
        route(level, drops, endpoint);
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
                .onlinePlayer(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level), boundOwner) != null ? boundOwner : null;
    }

    private void route(ServerLevel level, List<ItemStack> drops,
                       @Nullable PersonalStorageNetwork.Endpoint endpoint) {
        for (ItemStack drop : drops) {
            ItemStack remainder = MachineOutputScheduler.pushItemToFaces(
                    level, worldPosition, automaticOutput, outputFaces, drop.copy());
            if (endpoint != null && !remainder.isEmpty()) {
                remainder = endpoint.insert(remainder, false);
            }
            for (int slot = OUTPUT_START; !remainder.isEmpty() && slot < SLOT_COUNT; slot++) {
                remainder = items.insertItem(slot, remainder, false);
            }
            if (!remainder.isEmpty()) Block.popResource(level, worldPosition, remainder);
        }
    }

    public void releaseExperience(ServerPlayer player) {
        if (storedExperience <= 0) return;
        player.giveExperiencePoints(storedExperience);
        storedExperience = 0;
        setChangedAndSync();
    }

    public void dropAsItem(ServerPlayer player) {
        ItemStack dropped = new ItemStack(getBlockState().getBlock());
        dropped.set(DataComponents.CUSTOM_DATA, CustomData.of(saveWithFullMetadata(player.registryAccess())));
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
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(saveWithFullMetadata(registries)));
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SimulatedReincarnationFurnaceMenu(id, inventory, this);
    }
    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { for (int i=0;i<SLOT_COUNT;i++) if(!getItem(i).isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) { return items.getStackInSlot(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return items.extractItem(slot, amount, false); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return items.extractItem(slot, Integer.MAX_VALUE, false); }
    @Override public void setItem(int slot, ItemStack stack) { items.setStackInSlot(slot, stack); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { for(int i=0;i<SLOT_COUNT;i++) items.setStackInSlot(i, ItemStack.EMPTY); }
    @Override protected void saveAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditionalLegacy(tag, registries); tag.put("Items", com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.serialize(items, registries));
        tag.putInt("Progress", progress); tag.putInt("BurnTicks", burnTicks);
        tag.putInt("StoredExperience", storedExperience);
        tag.putBoolean("XianqiaoOutput", xianqiaoOutput);
        tag.putBoolean("AutomaticFaceOutput", automaticOutput);
        int[] faces = new int[outputFaces.length];
        for (Direction side : Direction.values()) faces[side.ordinal()] = outputFaces[side.ordinal()] ? 1 : 0;
        tag.putIntArray("OutputFaces", faces);
        tag.putLong("CompletedCycles", completedCycles);
    }
    @Override protected void loadAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditionalLegacy(tag, registries); com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.deserialize(items, registries, tag.getCompoundOrEmpty("Items"));
        progress=tag.getIntOr("Progress", 0); burnTicks=tag.getIntOr("BurnTicks", 0); storedExperience=tag.getIntOr("StoredExperience", 0);
        completedCycles=Math.max(0L, tag.getLongOr("CompletedCycles", 0L));
        xianqiaoOutput = tag.contains("XianqiaoOutput")
                ? tag.getBooleanOr("XianqiaoOutput", false)
                : !tag.contains("AutomaticOutput") || tag.getBooleanOr("AutomaticOutput", false);
        automaticOutput = !tag.contains("AutomaticFaceOutput") || tag.getBooleanOr("AutomaticFaceOutput", false);
        Arrays.fill(outputFaces, false);
        int[] faces = tag.getIntArray("OutputFaces").orElseGet(() -> new int[0]);
        for (int i = 0; i < Math.min(faces.length, outputFaces.length); i++) outputFaces[i] = faces[i] != 0;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
