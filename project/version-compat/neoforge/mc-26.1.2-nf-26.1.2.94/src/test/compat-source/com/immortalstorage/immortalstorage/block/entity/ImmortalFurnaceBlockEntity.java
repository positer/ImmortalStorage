package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.block.custom.ImmortalFurnaceBlock;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.item.custom.ImmortalYuanItem;
import com.immortalstorage.immortalstorage.item.custom.SpiritDriveItem;
import com.immortalstorage.immortalstorage.item.custom.SpiritSwordItem;
import com.immortalstorage.immortalstorage.item.custom.SpiritSwordTempering;
import com.immortalstorage.immortalstorage.item.custom.TrueYuanItem;
import com.immortalstorage.immortalstorage.menu.custom.ImmortalFurnaceEngine;
import com.immortalstorage.immortalstorage.menu.custom.ImmortalFurnaceMenu;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent three-channel immortal furnace.
 *
 * <p>The legacy input/fuel/result indices remain 0/1/2. Two input/result
 * pairs are appended, so old three-slot NBT loads without moving or rewriting
 * any existing stack.</p>
 */
public class ImmortalFurnaceBlockEntity extends AbstractFurnaceBlockEntity implements ReinforcementPluginHost {
    public static final int INPUT_1 = 0;
    public static final int FUEL = 1;
    public static final int RESULT_1 = 2;
    public static final int INPUT_2 = 3;
    public static final int RESULT_2 = 4;
    public static final int INPUT_3 = 5;
    public static final int RESULT_3 = 6;
    public static final int PLUGIN_SLOT = 7;
    public static final int SLOT_COUNT = 8;
    public static final int DATA_COUNT = 8;

    private static final int NBT_VERSION = 2;
    private static final int[] INPUT_SLOTS = {INPUT_1, INPUT_2, INPUT_3};
    private static final int[] RESULT_SLOTS = {RESULT_1, RESULT_2, RESULT_3};
    private static final int[] FUEL_SLOTS = {FUEL};
    static final int SPIRIT_DRIVE_RETRY_TICKS = 5;

    private final ImmortalFurnaceEngine engine =
            new ImmortalFurnaceEngine(INPUT_SLOTS, FUEL, RESULT_SLOTS);
    private final Map<Identifier, Integer> observedRecipeUsage = new HashMap<>();
    private final IItemHandler[] itemHandlers = new IItemHandler[7];
    private boolean autoConsume;
    private long nextSpiritDrivePaymentTick = Long.MIN_VALUE;
    private ItemStack observedSpiritDrive = ItemStack.EMPTY;

    private final ContainerData furnaceData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == 0) return engine.litTime();
            if (index == 1) return engine.litDuration();
            if (index >= 2 && index < DATA_COUNT) {
                int channel = (index - 2) / 2;
                return (index & 1) == 0 ? engine.progress(channel) : engine.totalTime(channel);
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) engine.setLitTime(value);
            else if (index == 1) engine.setLitDuration(value);
            else if (index >= 2 && index < DATA_COUNT) {
                int channel = (index - 2) / 2;
                if ((index & 1) == 0) engine.setProgress(channel, value);
                else engine.setTotalTime(channel, value);
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private final ImmortalFurnaceEngine.FuelResolver fuelResolver =
            new ImmortalFurnaceEngine.FuelResolver() {
                @Override
                public ImmortalFurnaceEngine.FuelProfile resolve(ItemStack stack) {
                    return fuelProfile(stack);
                }

                @Override
                public List<ImmortalFurnaceEngine.FuelProfile> candidates(ItemStack stack) {
                    if (stack != null && stack.getItem() instanceof SpiritDriveItem) {
                        return SpiritDriveItem.owner(stack).isPresent()
                                ? List.of(ImmortalFurnaceEngine.IMMORTAL_YUAN,
                                ImmortalFurnaceEngine.TRUE_YUAN)
                                : List.of();
                    }
                    ImmortalFurnaceEngine.FuelProfile profile = fuelProfile(stack);
                    return profile.usable() ? List.of(profile) : List.of();
                }

                @Override
                public boolean canAttemptPayment(ItemStack stack) {
                    if (!(stack != null && stack.getItem() instanceof SpiritDriveItem)) return true;
                    return prepareSpiritDriveAttempt(stack, level instanceof ServerLevel serverLevel
                            ? serverLevel : null);
                }

                @Override
                public ImmortalFurnaceEngine.FuelProfile authorize(
                        ItemStack stack, ImmortalFurnaceEngine.FuelProfile candidate) {
                    if (!(stack != null && stack.getItem() instanceof SpiritDriveItem)) return candidate;
                    return availableBoundSpiritDriveFuel(stack,
                            level instanceof ServerLevel serverLevel ? serverLevel : null, candidate);
                }

                @Override
                public ImmortalFurnaceEngine.FuelProfile consume(
                        ItemStack stack, ImmortalFurnaceEngine.FuelProfile profile) {
                    if (stack != null && stack.getItem() instanceof SpiritDriveItem) {
                        return payBoundSpiritDrive(stack, level instanceof ServerLevel serverLevel
                                ? serverLevel : null, profile);
                    }
                    consumeFuelCharge(stack);
                    return profile;
                }

                @Override
                public void paymentSucceeded(ItemStack stack, ImmortalFurnaceEngine.FuelProfile profile) {
                    if (stack != null && stack.getItem() instanceof SpiritDriveItem) clearSpiritDrivePolling();
                }

                @Override
                public void paymentFailed(ItemStack stack) {
                    if (stack != null && stack.getItem() instanceof SpiritDriveItem
                            && level instanceof ServerLevel serverLevel) {
                        nextSpiritDrivePaymentTick = saturatingAdd(
                                serverLevel.getGameTime(), SPIRIT_DRIVE_RETRY_TICKS);
                    }
                }

                @Override
                public void notRunnable(ItemStack stack) {
                    if (stack != null && stack.getItem() instanceof SpiritDriveItem) clearSpiritDrivePolling();
                }
            };

    public ImmortalFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IMMORTAL_FURNACE.get(), pos, state,
                ModRecipes.IMMORTAL_FURNACE_TYPE.get());
        this.items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    }

    public boolean isAutoConsume() {
        return autoConsume;
    }

    public void setAutoConsume(boolean value) {
        if (autoConsume != value) {
            autoConsume = value;
            setChanged();
        }
    }

    /**
     * Transfers one immortal-yuan unit from the closest player into the real
     * fuel slot. The unit is never destroyed here; the engine consumes it only
     * after it has verified that at least one channel can complete atomically.
     */
    public boolean tryAutoRefuelFromRealmOwner(ServerLevel level) {
        if (level == null || engine.isLit() || !getItem(FUEL).isEmpty() || !hasAnyInput()) return false;
        UUID ownerId = autoConsumeOwner(level.dimension()).orElse(null);
        if (ownerId == null) return false;
        ServerPlayer owner = com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level), ownerId);
        if (owner == null) return false;
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(owner);
        if (data == null || data.getStage() < 6 || !data.consumeImmortalYuan(1L)) return false;
        setItem(FUEL, new ItemStack(ModItems.IMMORTAL_YUAN.get()));
        return true;
    }

    static Optional<UUID> autoConsumeOwner(ResourceKey<Level> dimension) {
        return dimension == null ? Optional.empty() : ImmortalStorageDimensions.personalRealmOwner(dimension);
    }

    private boolean hasAnyInput() {
        for (int slot : INPUT_SLOTS) {
            if (!getItem(slot).isEmpty()) return true;
        }
        return false;
    }

    private boolean prepareSpiritDriveAttempt(ItemStack drive, @Nullable ServerLevel serverLevel) {
        if (serverLevel == null) return false;
        if (!sameSpiritDrive(observedSpiritDrive, drive)) {
            observedSpiritDrive = drive == null ? ItemStack.EMPTY : drive.copy();
            nextSpiritDrivePaymentTick = Long.MIN_VALUE;
        }
        if (drive == null || drive.isEmpty() || !hasAnyInput()) {
            clearSpiritDrivePolling();
            return false;
        }
        return retryDue(serverLevel.getGameTime(), nextSpiritDrivePaymentTick);
    }

    private ImmortalFurnaceEngine.FuelProfile availableBoundSpiritDriveFuel(
            ItemStack drive, @Nullable ServerLevel serverLevel,
            ImmortalFurnaceEngine.FuelProfile candidate) {
        if (serverLevel == null || drive == null || drive.isEmpty()
                || !sameSpiritDrive(observedSpiritDrive, drive)) return ImmortalFurnaceEngine.NO_FUEL;
        UUID ownerId = SpiritDriveItem.owner(drive).orElse(null);
        ServerPlayer owner = ownerId == null ? null
                : com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(serverLevel.getServer(), ownerId);
        ImmortalStoragePlayerData data = owner == null ? null : ImmortalStoragePlayerData.get(owner);
        if (data == null) return ImmortalFurnaceEngine.NO_FUEL;
        ItemStack currency = fuelCurrency(candidate);
        return !currency.isEmpty() && !data.simulateExtractStack(currency, 1).isEmpty()
                ? candidate : ImmortalFurnaceEngine.NO_FUEL;
    }

    private ImmortalFurnaceEngine.FuelProfile payBoundSpiritDrive(
            ItemStack drive, @Nullable ServerLevel serverLevel,
            ImmortalFurnaceEngine.FuelProfile offered) {
        if (serverLevel == null || drive == null || drive.isEmpty() || !hasAnyInput()
                || !sameSpiritDrive(observedSpiritDrive, drive) || offered == null || !offered.usable()) {
            clearSpiritDrivePolling();
            return ImmortalFurnaceEngine.NO_FUEL;
        }
        UUID ownerId = SpiritDriveItem.owner(drive).orElse(null);
        ServerPlayer owner = ownerId == null ? null
                : com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(serverLevel.getServer(), ownerId);
        ImmortalStoragePlayerData data = owner == null ? null : ImmortalStoragePlayerData.get(owner);
        ItemStack paidStack = data == null ? ItemStack.EMPTY : extractFuelPayment(data, offered);
        boolean paid = !paidStack.isEmpty();
        return paid ? offered : ImmortalFurnaceEngine.NO_FUEL;
    }

    private static ItemStack extractFuelPayment(
            ImmortalStoragePlayerData data, ImmortalFurnaceEngine.FuelProfile offered) {
        ItemStack currency = fuelCurrency(offered);
        if (currency.isEmpty()) return ItemStack.EMPTY;
        ItemStack extracted = data.extractStack(currency, 1);
        if (extracted.isEmpty()) return ItemStack.EMPTY;
        boolean paidMatchingKind = offered.equals(ImmortalFurnaceEngine.IMMORTAL_YUAN)
                ? extracted.getItem() instanceof ImmortalYuanItem
                : extracted.getItem() instanceof TrueYuanItem;
        if (!paidMatchingKind) {
            data.insertStack(extracted, true);
            return ItemStack.EMPTY;
        }
        return extracted;
    }

    private static ItemStack fuelCurrency(ImmortalFurnaceEngine.FuelProfile profile) {
        if (ImmortalFurnaceEngine.IMMORTAL_YUAN.equals(profile)) {
            return new ItemStack(ModItems.IMMORTAL_YUAN.get());
        }
        if (ImmortalFurnaceEngine.TRUE_YUAN.equals(profile)) {
            return new ItemStack(ModItems.TRUE_YUAN.get());
        }
        return ItemStack.EMPTY;
    }

    private void refreshSpiritDriveRetryState() {
        if (nextSpiritDrivePaymentTick == Long.MIN_VALUE) return;
        ItemStack fuel = getItem(FUEL);
        if (!(fuel.getItem() instanceof SpiritDriveItem) || !hasAnyInput()
                || !sameSpiritDrive(observedSpiritDrive, fuel)) {
            observedSpiritDrive = ItemStack.EMPTY;
            clearSpiritDrivePolling();
        }
    }

    private void clearSpiritDrivePolling() {
        nextSpiritDrivePaymentTick = Long.MIN_VALUE;
    }

    static boolean retryDue(long gameTime, long nextRetryTick) {
        return nextRetryTick == Long.MIN_VALUE || gameTime >= nextRetryTick;
    }

    private static boolean sameSpiritDrive(ItemStack left, ItemStack right) {
        return left != null && right != null && !left.isEmpty() && !right.isEmpty()
                && ItemStack.isSameItemSameComponents(left, right);
    }

    private static long saturatingAdd(long value, int delta) {
        return value > Long.MAX_VALUE - delta ? Long.MAX_VALUE : value + delta;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.immortalstorage.immortal_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ImmortalFurnaceMenu(id, inventory, this);
    }

    public ContainerData getDataAccessPublic() {
        return furnaceData;
    }

    public boolean isLitPublic() {
        return engine.isLit();
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        int index = side == null ? 6 : side.ordinal();
        IItemHandler current = itemHandlers[index];
        if (current == null) {
            current = new SidedInvWrapper(this, side);
            itemHandlers[index] = current;
        }
        return current;
    }

    public boolean isRecipeInput(ItemStack stack) {
        return stack != null && !stack.isEmpty() && findRecipe(stack).isPresent();
    }

    public static boolean isImmortalFuel(ItemStack stack) {
        return fuelProfile(stack).usable();
    }

    static ImmortalFurnaceEngine.FuelProfile fuelProfile(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ImmortalFurnaceEngine.NO_FUEL;
        if (stack.getItem() instanceof TrueYuanItem) return ImmortalFurnaceEngine.TRUE_YUAN;
        if (stack.getItem() instanceof ImmortalYuanItem) return ImmortalFurnaceEngine.IMMORTAL_YUAN;
        if (stack.getItem() instanceof SpiritDriveItem && SpiritDriveItem.owner(stack).isPresent()) {
            return ImmortalFurnaceEngine.IMMORTAL_YUAN;
        }
        return ImmortalFurnaceEngine.NO_FUEL;
    }

    static void consumeFuelCharge(ItemStack stack) {
        consumeFuelCharge(stack, stack != null && stack.getItem() instanceof SpiritDriveItem);
    }

    static void consumeFuelCharge(ItemStack stack, boolean reusableContainer) {
        if (stack == null || stack.isEmpty()) return;
        if (!reusableContainer) stack.shrink(1);
    }

    @Override
    protected int getBurnDuration(FuelValues fuelValues, ItemStack stack) {
        return fuelProfile(stack).burnTicks();
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        ItemStack safe = stack == null ? ItemStack.EMPTY : stack;
        items.set(slot, safe);
        safe.limitSize(getMaxStackSize(safe));
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == PLUGIN_SLOT) return ReinforcementPluginHost.isPlugin(stack);
        if (slot == FUEL) {
            return stack != null && stack.getItem() instanceof SpiritDriveItem || isImmortalFuel(stack);
        }
        if (isInputSlot(slot)) return isRecipeInput(stack);
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return automationSlots(side);
    }

    public static int[] automationSlots(Direction side) {
        if (side == Direction.UP) return INPUT_SLOTS.clone();
        if (side == Direction.DOWN) return RESULT_SLOTS.clone();
        return FUEL_SLOTS.clone();
    }

    public static boolean automationCanInsert(int slot, @Nullable Direction side) {
        if (side == Direction.UP) return isInputSlot(slot);
        return side != Direction.DOWN && slot == FUEL;
    }

    public static boolean automationCanExtract(int slot, Direction side) {
        return side == Direction.DOWN && isResultSlot(slot);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return automationCanInsert(slot, side) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return automationCanExtract(slot, side);
    }

    private static boolean isInputSlot(int slot) {
        return slot == INPUT_1 || slot == INPUT_2 || slot == INPUT_3;
    }

    private static boolean isResultSlot(int slot) {
        return slot == RESULT_1 || slot == RESULT_2 || slot == RESULT_3;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
                                  ImmortalFurnaceBlockEntity furnace) {
        furnace.refreshSpiritDriveRetryState();
        boolean wasLit = furnace.engine.isLit();
        boolean changed = furnace.engine.tick(level.getGameTime(), furnace, furnace.fuelResolver,
                furnace::findRecipe, channel -> false, furnace.reinforcementMultiplier());
        furnace.recordCompletedRecipes((RecipeManager) level.recipeAccess());
        boolean isLit = furnace.engine.isLit();
        if (wasLit != isLit && state.hasProperty(ImmortalFurnaceBlock.LIT)) {
            level.setBlock(pos, state.setValue(ImmortalFurnaceBlock.LIT, isLit), 3);
            changed = true;
        }
        if (changed) setChanged(level, pos, state);
    }

    @Override public ItemStack reinforcementPlugin() { return items.get(PLUGIN_SLOT); }
    @Override public void setReinforcementPlugin(ItemStack stack) {
        setItem(PLUGIN_SLOT, stack.copyWithCount(1));
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void recordCompletedRecipes(RecipeManager manager) {
        for (Map.Entry<Identifier, Integer> entry : engine.combinedRecipeUsage().entrySet()) {
            int previous = observedRecipeUsage.getOrDefault(entry.getKey(), 0);
            int delta = Math.max(0, entry.getValue() - previous);
            if (delta > 0) {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatRecipeAccess.byKey(manager, entry.getKey()).ifPresent(holder -> {
                    for (int i = 0; i < delta; i++) setRecipeUsed(holder);
                });
                observedRecipeUsage.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private Optional<ImmortalFurnaceEngine.RecipePlan> findRecipe(ItemStack stack) {
        if (stack == null || stack.isEmpty() || level == null) return Optional.empty();
        SingleRecipeInput input = new SingleRecipeInput(stack);
        RecipeManager manager = (RecipeManager) level.recipeAccess();
        List<RecipeCandidate> candidates = new ArrayList<>();
        collectRecipes(candidates, manager, ModRecipes.IMMORTAL_FURNACE_TYPE.get(), 0, input, level);
        collectRecipes(candidates, manager, RecipeType.SMELTING, 1, input, level);
        collectRecipes(candidates, manager, RecipeType.BLASTING, 2, input, level);
        candidates.sort(Comparator.comparingInt(RecipeCandidate::priority)
                .thenComparing(candidate -> candidate.holder().id().identifier().toString()));

        Set<Identifier> seen = new HashSet<>();
        for (RecipeCandidate candidate : candidates) {
            RecipeHolder<? extends AbstractCookingRecipe> holder = candidate.holder();
            if (!seen.add(holder.id().identifier())) continue;
            ItemStack result = holder.value().assemble(input);
            if (!result.isEmpty()) {
                return Optional.of(stack.getItem() instanceof SpiritSwordItem
                        ? new ImmortalFurnaceEngine.RecipePlan(holder.id().identifier(),
                        SpiritSwordTempering.temper(stack), true)
                        : new ImmortalFurnaceEngine.RecipePlan(holder.id().identifier(), result));
            }
        }
        return Optional.empty();
    }

    private static <T extends AbstractCookingRecipe> void collectRecipes(
            List<RecipeCandidate> target, RecipeManager manager, RecipeType<T> type,
            int priority, SingleRecipeInput input, Level level) {
        for (RecipeHolder<T> holder : com.immortalstorage.immortalstorage.compat.mc2612.CompatRecipeAccess.getRecipesFor(manager, type, input, level)) {
            if (priority > 0 && "immortalstorage".equals(holder.id().identifier().getNamespace())) continue;
            target.add(new RecipeCandidate(priority, holder));
        }
    }

    private void saveAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        
        tag.putInt("ImmortalFurnaceVersion", NBT_VERSION);
        tag.putBoolean("autoConsume", autoConsume);
        tag.putInt("BurnTime", engine.litTime());
        tag.putInt("ImmortalBurnDuration", engine.litDuration());
        tag.putInt("ImmortalFuelKind", fuelKind(engine.activeFuel()));
        int[] progress = new int[ImmortalFurnaceEngine.CHANNEL_COUNT];
        int[] totals = new int[ImmortalFurnaceEngine.CHANNEL_COUNT];
        CompoundTag activeRecipes = new CompoundTag();
        for (int channel = 0; channel < ImmortalFurnaceEngine.CHANNEL_COUNT; channel++) {
            progress[channel] = engine.progress(channel);
            totals[channel] = engine.totalTime(channel);
            Identifier recipe = engine.activeRecipe(channel);
            if (recipe != null) activeRecipes.putString(Integer.toString(channel), recipe.toString());
        }
        tag.putIntArray("ImmortalProgress", progress);
        tag.putIntArray("ImmortalTotalTime", totals);
        tag.put("ImmortalActiveRecipes", activeRecipes);
    }

    private void loadAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        
        if (items.size() != SLOT_COUNT) {
            NonNullList<ItemStack> expanded = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
            for (int i = 0; i < Math.min(items.size(), SLOT_COUNT); i++) expanded.set(i, items.get(i));
            items = expanded;
        }
        autoConsume = tag.getBooleanOr("autoConsume", false);
        engine.setLitTime(tag.getIntOr("BurnTime", 0));
        int version = tag.getIntOr("ImmortalFurnaceVersion", 0);
        if (version >= NBT_VERSION) {
            engine.setLitDuration(tag.getIntOr("ImmortalBurnDuration", 0));
            engine.setActiveFuel(fuelKind(tag.getIntOr("ImmortalFuelKind", 0)));
            int[] progress = tag.getIntArray("ImmortalProgress").orElseGet(() -> new int[0]);
            int[] totals = tag.getIntArray("ImmortalTotalTime").orElseGet(() -> new int[0]);
            CompoundTag activeRecipes = tag.getCompoundOrEmpty("ImmortalActiveRecipes");
            for (int channel = 0; channel < ImmortalFurnaceEngine.CHANNEL_COUNT; channel++) {
                if (channel < progress.length) engine.setProgress(channel, progress[channel]);
                if (channel < totals.length) engine.setTotalTime(channel, totals[channel]);
                String key = Integer.toString(channel);
                if (activeRecipes.contains(key)) {
                    engine.setActiveRecipe(channel, Identifier.tryParse(activeRecipes.getStringOr(key, "")));
                }
            }
        } else {
            migrateLegacyState(tag);
        }
        observedRecipeUsage.clear();
    }

    private void migrateLegacyState(CompoundTag tag) {
        int legacyProgress = tag.getIntOr("CookTime", 0);
        int legacyTotal = Math.max(1, tag.getIntOr("CookTimeTotal", 0));
        engine.setProgress(0, legacyProgress);
        engine.setTotalTime(0, legacyTotal);
        ImmortalFurnaceEngine.FuelProfile storedFuel = fuelProfile(getItem(FUEL));
        if (!storedFuel.usable() && engine.litTime() > 0) {
            storedFuel = engine.litTime() > ImmortalFurnaceEngine.TRUE_YUAN.burnTicks()
                    ? ImmortalFurnaceEngine.IMMORTAL_YUAN
                    : ImmortalFurnaceEngine.TRUE_YUAN;
        }
        engine.setActiveFuel(storedFuel);
        engine.setLitDuration(storedFuel.usable() ? storedFuel.burnTicks() : 0);
    }

    private static int fuelKind(ImmortalFurnaceEngine.FuelProfile profile) {
        if (ImmortalFurnaceEngine.IMMORTAL_YUAN.equals(profile)) return 2;
        if (ImmortalFurnaceEngine.TRUE_YUAN.equals(profile)) return 1;
        return 0;
    }

    private static ImmortalFurnaceEngine.FuelProfile fuelKind(int id) {
        return switch (id) {
            case 1 -> ImmortalFurnaceEngine.TRUE_YUAN;
            case 2 -> ImmortalFurnaceEngine.IMMORTAL_YUAN;
            default -> ImmortalFurnaceEngine.NO_FUEL;
        };
    }

    private record RecipeCandidate(int priority,
                                   RecipeHolder<? extends AbstractCookingRecipe> holder) {
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag legacy = new CompoundTag();
        saveAdditionalLegacy(legacy, level != null ? level.registryAccess() : RegistryAccess.EMPTY);
        output.store(legacy);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag legacy = input.read(com.mojang.serialization.MapCodec.assumeMapUnsafe(CompoundTag.CODEC))
                .orElseGet(CompoundTag::new);
        loadAdditionalLegacy(legacy, input.lookup());
    }}
