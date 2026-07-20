package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.item.custom.ImmortalYuanItem;
import com.immortalstorage.immortalstorage.item.custom.SpiritSwordItem;
import com.immortalstorage.immortalstorage.item.custom.SpiritSwordTempering;
import com.immortalstorage.immortalstorage.item.custom.TrueYuanItem;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.recipe.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Persistent server-authoritative furnace owned by one player's cultivation data.
 * Slot zero/input, slot one/fuel and slot two/result remain the legacy first
 * channel; two additional input/result pairs extend the same container to
 * three independent work channels sharing one fuel slot.
 */
public final class EmbeddedImmortalFurnaceBackend extends SimpleContainer {
    private static final int NBT_VERSION = 2;
    private static final String ITEMS_TAG = "Items";
    private static final String RECIPE_USAGE_TAG = "RecipeUsage";
    private static final String RECALL_BACKEND_TAG = "spiritSwordRecallBackend";
    private static final String RECALL_TOKEN_TAG = "spiritSwordRecallToken";
    private static final String RECALL_OWNER_TAG = "spiritSwordRecallOwner";
    private static final String RECALL_CHANNEL_TAG = "spiritSwordRecallChannel";
    static final int INPUT = 0;
    static final int FUEL = 1;
    static final int RESULT = 2;
    static final int INPUT_2 = 3;
    static final int RESULT_2 = 4;
    static final int INPUT_3 = 5;
    static final int RESULT_3 = 6;
    static final int DATA_COUNT = 10;

    private static final int TRUE_YUAN_BURN_TICKS = 150;
    private static final int IMMORTAL_YUAN_BURN_TICKS = 500;
    private static final int[] INPUT_SLOTS = {INPUT, INPUT_2, INPUT_3};
    private static final int[] RESULT_SLOTS = {RESULT, RESULT_2, RESULT_3};
    private static final ImmortalFurnaceEngine.FuelResolver BUILT_IN_FUEL_RESOLVER =
            new ImmortalFurnaceEngine.FuelResolver() {
                @Override
                public ImmortalFurnaceEngine.FuelProfile resolve(ItemStack stack) {
                    return fuelProfile(stack);
                }

                @Override
                public ImmortalFurnaceEngine.FuelProfile consume(
                        ItemStack stack, ImmortalFurnaceEngine.FuelProfile profile) {
                    if (stack == null || stack.isEmpty()) return ImmortalFurnaceEngine.NO_FUEL;
                    stack.shrink(1);
                    return profile;
                }
            };

    private final ImmortalFurnaceEngine engine =
            new ImmortalFurnaceEngine(INPUT_SLOTS, FUEL, RESULT_SLOTS);
    private boolean autoConsume;
    private boolean autoFill;
    private final ItemStack[] refillTemplates = {
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };
    private UUID backendId = UUID.randomUUID();
    private final UUID[] recallTokens = new UUID[ImmortalFurnaceEngine.CHANNEL_COUNT];
    private boolean internalMutation;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == 0) return engine.litTime();
            if (index == 1) return engine.litDuration();
            if (index >= 2 && index < 8) {
                int channel = (index - 2) / 2;
                return (index & 1) == 0 ? engine.progress(channel) : engine.totalTime(channel);
            }
            if (index == 8) return autoConsume ? 1 : 0;
            if (index == 9) return autoFill ? 1 : 0;
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) engine.setLitTime(value);
            else if (index == 1) engine.setLitDuration(value);
            else if (index >= 2 && index < 8) {
                int channel = (index - 2) / 2;
                if ((index & 1) == 0) engine.setProgress(channel, value);
                else engine.setTotalTime(channel, value);
            } else if (index == 8) autoConsume = value != 0;
            else if (index == 9) autoFill = value != 0;
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public EmbeddedImmortalFurnaceBackend() {
        super(7);
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", NBT_VERSION);
        ListTag items = new ListTag();
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", slot);
            entry.put("Item", ImmortalStoragePlayerData.saveStack(registries, stack));
            items.add(entry);
        }
        tag.put(ITEMS_TAG, items);
        tag.putBoolean("AutoConsume", autoConsume);
        tag.putBoolean("AutoFill", autoFill);
        tag.putUUID("BackendId", backendId);
        ListTag reservations = new ListTag();
        for (int channel = 0; channel < recallTokens.length; channel++) {
            UUID token = recallTokens[channel];
            if (token == null) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Channel", channel);
            entry.putUUID("Token", token);
            reservations.add(entry);
        }
        tag.put("SpiritSwordReservations", reservations);
        ListTag refillItems = new ListTag();
        for (int channel = 0; channel < refillTemplates.length; channel++) {
            ItemStack template = refillTemplates[channel];
            if (template.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Channel", channel);
            entry.put("Item", ImmortalStoragePlayerData.saveStack(registries, template));
            refillItems.add(entry);
        }
        tag.put("RefillTemplates", refillItems);
        tag.putInt("LitTime", engine.litTime());
        tag.putInt("LitDuration", engine.litDuration());
        tag.putInt("FuelKind", fuelKind(engine.activeFuel()));
        int[] progress = new int[ImmortalFurnaceEngine.CHANNEL_COUNT];
        int[] totals = new int[ImmortalFurnaceEngine.CHANNEL_COUNT];
        CompoundTag activeRecipes = new CompoundTag();
        ListTag recipeUsage = new ListTag();
        for (int channel = 0; channel < ImmortalFurnaceEngine.CHANNEL_COUNT; channel++) {
            progress[channel] = engine.progress(channel);
            totals[channel] = engine.totalTime(channel);
            ResourceLocation activeRecipe = engine.activeRecipe(channel);
            if (activeRecipe != null) activeRecipes.putString(Integer.toString(channel), activeRecipe.toString());
            for (Map.Entry<ResourceLocation, Integer> usage : engine.recipeUsage(channel).entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Channel", channel);
                entry.putString("Recipe", usage.getKey().toString());
                entry.putInt("Count", usage.getValue());
                recipeUsage.add(entry);
            }
        }
        tag.putIntArray("Progress", progress);
        tag.putIntArray("TotalTime", totals);
        tag.put("ActiveRecipes", activeRecipes);
        tag.put(RECIPE_USAGE_TAG, recipeUsage);
        return tag;
    }

    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        clearContent();
        engine.setLitTime(0);
        engine.setLitDuration(0);
        engine.setActiveFuel(ImmortalFurnaceEngine.NO_FUEL);
        engine.clearRecipeUsage();
        autoConsume = false;
        autoFill = false;
        java.util.Arrays.fill(refillTemplates, ItemStack.EMPTY);
        java.util.Arrays.fill(recallTokens, null);
        for (int channel = 0; channel < ImmortalFurnaceEngine.CHANNEL_COUNT; channel++) {
            engine.setProgress(channel, 0);
            engine.setTotalTime(channel, ImmortalFurnaceEngine.TRUE_YUAN.cookingTicks());
            engine.setActiveRecipe(channel, null);
        }
        if (tag == null || tag.isEmpty()) return;
        backendId = tag.hasUUID("BackendId") ? tag.getUUID("BackendId") : UUID.randomUUID();

        ListTag items = tag.getList(ITEMS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag entry = items.getCompound(index);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < getContainerSize()) {
                setItem(slot, ImmortalStoragePlayerData.loadStack(registries, entry.getCompound("Item")));
            }
        }
        autoConsume = tag.getBoolean("AutoConsume");
        autoFill = tag.getBoolean("AutoFill");
        ListTag reservations = tag.getList("SpiritSwordReservations", Tag.TAG_COMPOUND);
        for (int index = 0; index < reservations.size(); index++) {
            CompoundTag entry = reservations.getCompound(index);
            int channel = entry.getInt("Channel");
            if (channel >= 0 && channel < recallTokens.length && entry.hasUUID("Token")) {
                recallTokens[channel] = entry.getUUID("Token");
            }
        }
        ListTag refillItems = tag.getList("RefillTemplates", Tag.TAG_COMPOUND);
        for (int index = 0; index < refillItems.size(); index++) {
            CompoundTag entry = refillItems.getCompound(index);
            int channel = entry.getInt("Channel");
            if (channel >= 0 && channel < refillTemplates.length) {
                refillTemplates[channel] = ImmortalStoragePlayerData.loadStack(
                        registries, entry.getCompound("Item"));
            }
        }
        engine.setLitTime(tag.getInt("LitTime"));
        engine.setLitDuration(tag.getInt("LitDuration"));
        engine.setActiveFuel(fuelKind(tag.getInt("FuelKind")));
        int[] progress = tag.getIntArray("Progress");
        int[] totals = tag.getIntArray("TotalTime");
        CompoundTag activeRecipes = tag.getCompound("ActiveRecipes");
        @SuppressWarnings("unchecked")
        Map<ResourceLocation, Integer>[] usageByChannel = new Map[] {
                new java.util.LinkedHashMap<>(), new java.util.LinkedHashMap<>(), new java.util.LinkedHashMap<>()
        };
        ListTag recipeUsage = tag.getList(RECIPE_USAGE_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < recipeUsage.size(); index++) {
            CompoundTag entry = recipeUsage.getCompound(index);
            int channel = entry.getInt("Channel");
            ResourceLocation recipe = ResourceLocation.tryParse(entry.getString("Recipe"));
            int count = entry.getInt("Count");
            if (channel >= 0 && channel < usageByChannel.length && recipe != null && count > 0) {
                usageByChannel[channel].merge(recipe, count, EmbeddedImmortalFurnaceBackend::saturatingAdd);
            }
        }
        for (int channel = 0; channel < ImmortalFurnaceEngine.CHANNEL_COUNT; channel++) {
            if (channel < progress.length) engine.setProgress(channel, progress[channel]);
            if (channel < totals.length) engine.setTotalTime(channel, totals[channel]);
            String key = Integer.toString(channel);
            if (activeRecipes.contains(key, Tag.TAG_STRING)) {
                engine.setActiveRecipe(channel, ResourceLocation.tryParse(activeRecipes.getString(key)));
            }
            engine.setRecipeUsage(channel, usageByChannel[channel]);
        }
        setChanged();
    }

    ContainerData dataAccess() {
        return dataAccess;
    }

    boolean isLit() {
        return engine.isLit();
    }

    boolean isAutoConsume() {
        return autoConsume;
    }

    void setAutoConsume(boolean value) {
        if (autoConsume == value) return;
        autoConsume = value;
        setChanged();
    }

    boolean isAutoFill() {
        return autoFill;
    }

    void setAutoFill(boolean value) {
        if (autoFill == value) return;
        autoFill = value;
        if (value) rememberInputTemplates();
        setChanged();
    }

    boolean tryAutoRefuel(Supplier<ItemStack> source) {
        if (!autoConsume || engine.isLit() || !getItem(FUEL).isEmpty() || !hasAnyInput() || source == null) {
            return false;
        }
        ItemStack reserved = source.get();
        if (reserved == null || reserved.isEmpty()) return false;
        setItem(FUEL, reserved);
        return true;
    }

    int litProgress() {
        int duration = engine.litDuration() <= 0 ? TRUE_YUAN_BURN_TICKS : engine.litDuration();
        return Mth.ceil(engine.litTime() * 13.0F / duration);
    }

    int burnProgress() {
        return burnProgress(0);
    }

    int burnProgress(int channel) {
        int total = Math.max(1, engine.totalTime(channel));
        return engine.progress(channel) * 24 / total;
    }

    int channelProgress(int channel) {
        return engine.progress(channel);
    }

    Map<ResourceLocation, Integer> recipeUsage(int channel) {
        return engine.recipeUsage(channel);
    }

    int data(int index) {
        return dataAccess.get(index);
    }

    boolean isFuel(ItemStack stack) {
        return fuelProfile(stack).usable();
    }

    boolean isRecipeInput(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return false;
        return findRecipe(player, stack).isPresent();
    }

    public boolean summonSpiritSword(ServerPlayer player, InteractionHand hand) {
        if (player == null || hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) return false;
        for (int channel = 0; channel < INPUT_SLOTS.length; channel++) {
            ItemStack sword = getItem(INPUT_SLOTS[channel]);
            if (!(sword.getItem() instanceof SpiritSwordItem) || recallTokens[channel] != null) continue;
            UUID token = UUID.randomUUID();
            ItemStack summoned = sword.copy();
            writeRecall(summoned, player.getUUID(), channel, token);
            recallTokens[channel] = token;
            internalMutation = true;
            try {
                setItem(INPUT_SLOTS[channel], ItemStack.EMPTY);
            } finally {
                internalMutation = false;
            }
            player.setItemInHand(hand, summoned);
            setChanged();
            return true;
        }
        return false;
    }

    public boolean storeSpiritSword(ServerPlayer player, InteractionHand hand) {
        if (player == null) return false;
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof SpiritSwordItem) || held.getCount() != 1) return false;
        RecallIdentity identity = readRecall(held);
        int channel = matchingReservedChannel(player, identity);
        boolean resume = channel >= 0;
        if (!resume) channel = firstAvailableChannel();
        if (channel < 0) return false;

        ItemStack stored = held.copy();
        clearRecall(stored);
        internalMutation = true;
        try {
            setItem(INPUT_SLOTS[channel], stored);
        } finally {
            internalMutation = false;
        }
        player.setItemInHand(hand, ItemStack.EMPTY);
        if (resume) {
            recallTokens[channel] = null;
        } else {
            engine.setProgress(channel, 0);
            engine.setActiveRecipe(channel, null);
            refillTemplates[channel] = stored.copy();
        }
        setChanged();
        return true;
    }

    public boolean isRecallReserved(int channel) {
        return channel >= 0 && channel < recallTokens.length && recallTokens[channel] != null;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        int channel = channelForInputSlot(slot);
        if (!internalMutation && channel >= 0 && stack != null && !stack.isEmpty()
                && recallTokens[channel] != null) {
            recallTokens[channel] = null;
            engine.setProgress(channel, 0);
            engine.setActiveRecipe(channel, null);
        }
        super.setItem(slot, stack);
    }

    /** Advances this player-owned furnace exactly once from the server player tick. */
    public void tick(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        ImmortalStoragePlayerData storage = ImmortalStoragePlayerData.get(player);
        if (autoFill) {
            rememberInputTemplates();
            returnOutputsToStorage(storage);
            refillInputsFromStorage(storage);
        }
        tryAutoRefuel(() -> {
            if (storage == null) return ItemStack.EMPTY;
            if (storage.getStage() < 6) {
                return storage.consumeTrueYuan(1L)
                        ? new ItemStack(ModItems.TRUE_YUAN.get()) : ItemStack.EMPTY;
            }
            return storage.consumeImmortalYuan(1L)
                    ? new ItemStack(ModItems.IMMORTAL_YUAN.get()) : ItemStack.EMPTY;
        });
        tickCoreWithBuiltInFuel(level.getGameTime(), stack -> findRecipe(player, stack));
        if (autoFill) {
            returnOutputsToStorage(storage);
            refillInputsFromStorage(storage);
        }
    }

    private void rememberInputTemplates() {
        for (int channel = 0; channel < INPUT_SLOTS.length; channel++) {
            if (isRecallReserved(channel)) continue;
            ItemStack input = getItem(INPUT_SLOTS[channel]);
            if (input.isEmpty()) continue;
            ItemStack remembered = refillTemplates[channel];
            if (remembered.isEmpty()
                    || !ItemStack.isSameItemSameComponents(remembered, input)
                    || input.getCount() > remembered.getCount()) {
                refillTemplates[channel] = input.copy();
            }
        }
    }

    private void refillInputsFromStorage(ImmortalStoragePlayerData storage) {
        if (storage == null) return;
        for (int channel = 0; channel < INPUT_SLOTS.length; channel++) {
            if (isRecallReserved(channel)) continue;
            ItemStack template = refillTemplates[channel];
            if (template.isEmpty()) continue;
            int slot = INPUT_SLOTS[channel];
            ItemStack current = getItem(slot);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, template)) continue;
            int targetCount = template.getMaxStackSize();
            int missing = targetCount - (current.isEmpty() ? 0 : current.getCount());
            if (missing <= 0) continue;
            ItemStack extracted = storage.extractStack(template, missing);
            if (extracted.isEmpty()) continue;
            if (current.isEmpty()) setItem(slot, extracted);
            else current.grow(extracted.getCount());
        }
    }

    private void returnOutputsToStorage(ImmortalStoragePlayerData storage) {
        if (storage == null) return;
        for (int slot : RESULT_SLOTS) {
            ItemStack output = getItem(slot);
            if (output.isEmpty()) continue;
            ItemStack leftover = storage.insertStack(output.copy(), true);
            setItem(slot, leftover);
        }
    }

    private boolean hasAnyInput() {
        for (int slot : INPUT_SLOTS) {
            if (!getItem(slot).isEmpty()) return true;
        }
        return false;
    }

    void tickCoreWithBuiltInFuel(long gameTick, ImmortalFurnaceEngine.RecipeResolver recipeResolver) {
        tickCore(gameTick, BUILT_IN_FUEL_RESOLVER, recipeResolver);
    }

    void tickCore(long gameTick, ImmortalFurnaceEngine.FuelResolver fuelResolver,
                  ImmortalFurnaceEngine.RecipeResolver recipeResolver) {
        if (engine.tick(gameTick, this, fuelResolver, recipeResolver, this::isRecallReserved)) setChanged();
    }

    private int firstAvailableChannel() {
        for (int channel = 0; channel < INPUT_SLOTS.length; channel++) {
            if (!isRecallReserved(channel) && getItem(INPUT_SLOTS[channel]).isEmpty()) return channel;
        }
        return -1;
    }

    private int matchingReservedChannel(ServerPlayer player, RecallIdentity identity) {
        if (identity == null || !player.getUUID().equals(identity.owner())
                || !backendId.equals(identity.backend()) || identity.channel() < 0
                || identity.channel() >= recallTokens.length) return -1;
        UUID authoritative = recallTokens[identity.channel()];
        return authoritative != null && authoritative.equals(identity.token()) ? identity.channel() : -1;
    }

    private int channelForInputSlot(int slot) {
        for (int channel = 0; channel < INPUT_SLOTS.length; channel++) {
            if (INPUT_SLOTS[channel] == slot) return channel;
        }
        return -1;
    }

    private void writeRecall(ItemStack stack, UUID owner, int channel, UUID token) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putUUID(RECALL_OWNER_TAG, owner);
        tag.putUUID(RECALL_BACKEND_TAG, backendId);
        tag.putUUID(RECALL_TOKEN_TAG, token);
        tag.putInt(RECALL_CHANNEL_TAG, channel);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }

    private static RecallIdentity readRecall(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (!tag.hasUUID(RECALL_OWNER_TAG) || !tag.hasUUID(RECALL_BACKEND_TAG)
                || !tag.hasUUID(RECALL_TOKEN_TAG) || !tag.contains(RECALL_CHANNEL_TAG, Tag.TAG_INT)) return null;
        return new RecallIdentity(tag.getUUID(RECALL_OWNER_TAG), tag.getUUID(RECALL_BACKEND_TAG),
                tag.getUUID(RECALL_TOKEN_TAG), tag.getInt(RECALL_CHANNEL_TAG));
    }

    private static void clearRecall(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.remove(RECALL_OWNER_TAG);
        tag.remove(RECALL_BACKEND_TAG);
        tag.remove(RECALL_TOKEN_TAG);
        tag.remove(RECALL_CHANNEL_TAG);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }

    private record RecallIdentity(UUID owner, UUID backend, UUID token, int channel) {}

    void awardUsedRecipes(Player player) {
        Map<ResourceLocation, Integer> recipesUsed = engine.combinedRecipeUsage();
        if (!(player instanceof ServerPlayer serverPlayer) || recipesUsed.isEmpty()) return;
        ServerLevel level = serverPlayer.serverLevel();
        for (Map.Entry<ResourceLocation, Integer> entry : recipesUsed.entrySet()) {
            level.getRecipeManager().byKey(entry.getKey()).ifPresent(holder -> {
                serverPlayer.awardRecipes(List.of(holder));
                if (holder.value() instanceof AbstractCookingRecipe cooking) {
                    int experience = experienceFor(cooking.getExperience(), entry.getValue(), level);
                    if (experience > 0) ExperienceOrb.award(level, serverPlayer.position(), experience);
                }
            });
        }
        engine.clearRecipeUsage();
    }

    private static int fuelKind(ImmortalFurnaceEngine.FuelProfile profile) {
        if (ImmortalFurnaceEngine.IMMORTAL_YUAN.equals(profile)) return 2;
        if (ImmortalFurnaceEngine.TRUE_YUAN.equals(profile)) return 1;
        return 0;
    }

    private static ImmortalFurnaceEngine.FuelProfile fuelKind(int kind) {
        return switch (kind) {
            case 1 -> ImmortalFurnaceEngine.TRUE_YUAN;
            case 2 -> ImmortalFurnaceEngine.IMMORTAL_YUAN;
            default -> ImmortalFurnaceEngine.NO_FUEL;
        };
    }

    private static int saturatingAdd(int left, int right) {
        return left > Integer.MAX_VALUE - right ? Integer.MAX_VALUE : left + right;
    }

    static int fuelTicks(ResourceLocation id) {
        if (id == null || !"immortalstorage".equals(id.getNamespace())) return 0;
        return switch (id.getPath()) {
            case "true_yuan" -> TRUE_YUAN_BURN_TICKS;
            case "immortal_yuan" -> IMMORTAL_YUAN_BURN_TICKS;
            default -> 0;
        };
    }

    static boolean canAcceptResult(ItemStack current, ItemStack assembled) {
        return ImmortalFurnaceEngine.canAcceptResult(current, assembled, 1);
    }

    static boolean isAllowedFallbackRecipe(RecipeType<?> type, ResourceLocation id) {
        return id != null && !"immortalstorage".equals(id.getNamespace())
                && (type == RecipeType.SMELTING || type == RecipeType.BLASTING);
    }

    static int recipeSourcePriority(RecipeSource source) {
        return source.priority;
    }

    static ImmortalFurnaceEngine.FuelProfile fuelProfile(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ImmortalFurnaceEngine.NO_FUEL;
        if (stack.getItem() instanceof TrueYuanItem) return ImmortalFurnaceEngine.TRUE_YUAN;
        if (stack.getItem() instanceof ImmortalYuanItem) return ImmortalFurnaceEngine.IMMORTAL_YUAN;
        return ImmortalFurnaceEngine.NO_FUEL;
    }

    private Optional<ImmortalFurnaceEngine.RecipePlan> findRecipe(Player player, ItemStack stack) {
        if (stack == null || stack.isEmpty() || player == null) return Optional.empty();
        SingleRecipeInput input = new SingleRecipeInput(stack);
        RecipeManager manager = player.level().getRecipeManager();
        List<RecipeCandidate> candidates = new ArrayList<>();
        collectRecipes(candidates, manager, ModRecipes.IMMORTAL_FURNACE_TYPE.get(),
                RecipeSource.IMMORTAL_FURNACE, input, player);
        collectRecipes(candidates, manager, RecipeType.SMELTING, RecipeSource.SMELTING, input, player);
        collectRecipes(candidates, manager, RecipeType.BLASTING, RecipeSource.BLASTING, input, player);
        candidates.sort(Comparator.comparingInt((RecipeCandidate candidate) -> candidate.source().priority)
                .thenComparing(candidate -> candidate.holder().id().toString()));

        Set<ResourceLocation> seen = new HashSet<>();
        for (RecipeCandidate candidate : candidates) {
            ResourceLocation id = candidate.holder().id();
            if (!seen.add(id)) continue;
            ItemStack result = candidate.holder().value().assemble(input, player.level().registryAccess());
            if (!result.isEmpty()) return Optional.of(stack.getItem() instanceof SpiritSwordItem
                    ? new ImmortalFurnaceEngine.RecipePlan(id, SpiritSwordTempering.temper(stack), true)
                    : new ImmortalFurnaceEngine.RecipePlan(id, result));
        }
        return Optional.empty();
    }

    private static <T extends AbstractCookingRecipe> void collectRecipes(
            List<RecipeCandidate> target, RecipeManager manager, RecipeType<T> type, RecipeSource source,
            SingleRecipeInput input, Player player) {
        for (RecipeHolder<T> holder : manager.getRecipesFor(type, input, player.level())) {
            if (source != RecipeSource.IMMORTAL_FURNACE && !isAllowedFallbackRecipe(type, holder.id())) continue;
            target.add(new RecipeCandidate(source, holder));
        }
    }

    private static int experienceFor(float experience, int crafts, ServerLevel level) {
        float total = Math.max(0.0F, experience) * Math.max(0, crafts);
        int whole = Mth.floor(total);
        if (whole < Mth.ceil(total) && level.random.nextFloat() < total - whole) whole++;
        return whole;
    }

    enum RecipeSource {
        IMMORTAL_FURNACE(0),
        SMELTING(1),
        BLASTING(2);

        private final int priority;

        RecipeSource(int priority) {
            this.priority = priority;
        }
    }

    private record RecipeCandidate(RecipeSource source,
                                   RecipeHolder<? extends AbstractCookingRecipe> holder) {
    }
}
