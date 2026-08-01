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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class SimulatedReincarnationFurnaceBlockEntity extends BlockEntity
        implements Container, MenuProvider {
    public static final int SOURCE_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int WEAPON_SLOT = 2;
    public static final int OUTPUT_START = 3;
    public static final int OUTPUT_COUNT = 12;
    public static final int SLOT_COUNT = OUTPUT_START + OUTPUT_COUNT;
    public static final int PROCESS_TICKS = 50;

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
                serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };
    private int progress;
    private int burnTicks;
    private int storedExperience;
    private boolean automaticOutput = true;
    private final boolean[] outputFaces = new boolean[Direction.values().length];
    private UUID owner;
    private long completedCycles;

    public SimulatedReincarnationFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIMULATED_REINCARNATION_FURNACE.get(), pos, state);
    }

    public ItemStackHandler itemHandler() { return items; }
    public int progress() { return progress; }
    public int burnTicks() { return burnTicks; }
    public int storedExperience() { return storedExperience; }
    public boolean automaticOutput() { return automaticOutput; }
    public boolean outputFace(Direction side) { return side != null && outputFaces[side.ordinal()]; }
    public void setOwner(UUID owner) { this.owner = owner; setChanged(); }
    public void toggleAutomaticOutput() { automaticOutput = !automaticOutput; setChanged(); }
    public void toggleOutputFace(Direction side) {
        if (side == null) return;
        outputFaces[side.ordinal()] = !outputFaces[side.ordinal()];
        setChanged();
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
                                  SimulatedReincarnationFurnaceBlockEntity furnace) {
        LivingEntity specimen = furnace.createSpecimen(level);
        if (specimen == null || !furnace.ensureFuel(level)) {
            furnace.progress = 0;
            furnace.updateWorkingState(level, pos, state, false);
            return;
        }
        furnace.updateWorkingState(level, pos, state, true);
        furnace.burnTicks--;
        if (++furnace.progress < PROCESS_TICKS) return;
        furnace.progress = 0;
        furnace.produce(level, specimen);
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
        if (fuel.is(ModItems.SPIRIT_DRIVE.get())) {
            UUID driveOwner = SpiritDriveItem.owner(fuel).orElse(null);
            ServerPlayer player = driveOwner == null ? null : level.getServer().getPlayerList().getPlayer(driveOwner);
            if (player != null) {
                var data = com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData.get(player);
                if (data.consumeImmortalYuan(1L)) { burnTicks = 500; return true; }
                if (data.consumeTrueYuan(1L)) { burnTicks = 50; return true; }
            }
        }
        UUID realmOwner = ImmortalStorageDimensions.personalRealmOwner(level.dimension()).orElse(null);
        ServerPlayer ownerPlayer = realmOwner == null ? null
                : level.getServer().getPlayerList().getPlayer(realmOwner);
        if (ownerPlayer != null) {
            var data = com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData.get(ownerPlayer);
            if (data.consumeImmortalYuan(1L)) { burnTicks = 500; return true; }
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

    private void produce(ServerLevel level, LivingEntity specimen) {
        UUID outputOwner = effectiveOutputOwner(level);
        ServerPlayer killer = outputOwner == null ? null : level.getServer().getPlayerList().getPlayer(outputOwner);
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
        int xp = Math.max(0, specimen.getExperienceReward(level, killer));
        if (outputOwner != null && automaticOutput && killer != null) {
            killer.giveExperiencePoints(xp);
        } else {
            storedExperience = Math.min(Integer.MAX_VALUE, storedExperience + xp);
        }
        route(level, drops, outputOwner);
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

    private @Nullable UUID effectiveOutputOwner(ServerLevel level) {
        if (!automaticOutput) return null;
        if (owner != null && ImmortalStorageDimensions.isPersonalRealmFor(level.dimension(), owner)
                && level.getServer().getPlayerList().getPlayer(owner) != null) return owner;
        UUID driveOwner = SpiritDriveItem.owner(items.getStackInSlot(FUEL_SLOT)).orElse(null);
        return driveOwner != null && level.getServer().getPlayerList().getPlayer(driveOwner) != null
                ? driveOwner : null;
    }

    private void route(ServerLevel level, List<ItemStack> drops, @Nullable UUID outputOwner) {
        PersonalStorageNetwork.Endpoint endpoint = outputOwner == null ? null
                : (ImmortalStorageDimensions.isPersonalRealmFor(level.dimension(), outputOwner)
                ? PersonalStorageNetwork.resolveInOwnerRealm(level, outputOwner, this::setChanged)
                : PersonalStorageNetwork.resolve(level.getServer(), outputOwner, this::setChanged));
        for (ItemStack drop : drops) {
            ItemStack remainder = endpoint == null ? drop : endpoint.insert(drop, false);
            for (int slot = OUTPUT_START; !remainder.isEmpty() && slot < SLOT_COUNT; slot++) {
                remainder = items.insertItem(slot, remainder, false);
            }
        }
    }

    public void releaseExperience(ServerPlayer player) {
        if (storedExperience <= 0) return;
        player.giveExperiencePoints(storedExperience);
        storedExperience = 0;
        setChanged();
    }

    public void dropAsItem(ServerPlayer player) {
        ItemStack dropped = new ItemStack(getBlockState().getBlock());
        CompoundTag preserved = new CompoundTag();
        saveAdditional(preserved, player.registryAccess());
        dropped.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(preserved));
        net.minecraft.world.level.block.Block.popResource(player.level(), worldPosition, dropped);
        player.level().removeBlock(worldPosition, false);
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.immortalstorage.simulated_reincarnation_furnace");
    }
    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag preserved = new CompoundTag();
        saveAdditional(preserved, registries);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(preserved));
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
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Progress", progress); tag.putInt("BurnTicks", burnTicks);
        tag.putInt("StoredExperience", storedExperience); tag.putBoolean("AutomaticOutput", automaticOutput);
        int[] faces = new int[outputFaces.length];
        for (Direction side : Direction.values()) faces[side.ordinal()] = outputFaces[side.ordinal()] ? 1 : 0;
        tag.putIntArray("OutputFaces", faces);
        tag.putLong("CompletedCycles", completedCycles);
        if (owner != null) tag.putUUID("Owner", owner);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); items.deserializeNBT(registries, tag.getCompound("Items"));
        progress=tag.getInt("Progress"); burnTicks=tag.getInt("BurnTicks"); storedExperience=tag.getInt("StoredExperience");
        completedCycles=Math.max(0L, tag.getLong("CompletedCycles"));
        automaticOutput=!tag.contains("AutomaticOutput") || tag.getBoolean("AutomaticOutput"); owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;
        Arrays.fill(outputFaces, false);
        int[] faces = tag.getIntArray("OutputFaces");
        for (int i = 0; i < Math.min(faces.length, outputFaces.length); i++) outputFaces[i] = faces[i] != 0;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
