package com.cultivation.cultivation.menu.custom;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Pure three-channel state machine shared by embedded and placed immortal furnaces. */
public final class ImmortalFurnaceEngine {
    public static final int CHANNEL_COUNT = 3;
    public static final FuelProfile NO_FUEL = new FuelProfile(0, 1, false);
    public static final FuelProfile TRUE_YUAN = new FuelProfile(150, 50, false);
    public static final FuelProfile IMMORTAL_YUAN = new FuelProfile(500, 25, true);

    private final int[] inputSlots;
    private final int fuelSlot;
    private final int[] resultSlots;
    private final int[] progress = new int[CHANNEL_COUNT];
    private final int[] totalTime = {TRUE_YUAN.cookingTicks(), TRUE_YUAN.cookingTicks(),
            TRUE_YUAN.cookingTicks()};
    private final ResourceLocation[] activeRecipes = new ResourceLocation[CHANNEL_COUNT];
    @SuppressWarnings("unchecked")
    private final Map<ResourceLocation, Integer>[] recipeUsage = new Map[] {
            new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>()
    };

    private long lastGameTick = Long.MIN_VALUE;
    private int litTime;
    private int litDuration;
    private FuelProfile activeFuel = NO_FUEL;

    public ImmortalFurnaceEngine(int[] inputSlots, int fuelSlot, int[] resultSlots) {
        if (inputSlots == null || resultSlots == null
                || inputSlots.length != CHANNEL_COUNT || resultSlots.length != CHANNEL_COUNT) {
            throw new IllegalArgumentException("the immortal furnace requires exactly three channels");
        }
        this.inputSlots = inputSlots.clone();
        this.fuelSlot = fuelSlot;
        this.resultSlots = resultSlots.clone();
    }

    public boolean tick(long gameTick, Container inventory, FuelResolver fuelResolver, RecipeResolver recipeResolver) {
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(fuelResolver, "fuelResolver");
        Objects.requireNonNull(recipeResolver, "recipeResolver");
        if (lastGameTick == gameTick) return false;
        lastGameTick = gameTick;

        boolean changed = false;
        if (litTime > 0) {
            litTime--;
            changed = true;
        }

        RecipePlan[] plans = resolvePlans(inventory, recipeResolver);
        if (!isLit()) {
            ItemStack fuelStack = inventory.getItem(fuelSlot);
            List<FuelProfile> candidates = fuelStack.isEmpty()
                    ? List.of() : sanitizeCandidates(fuelResolver.candidates(fuelStack));
            boolean hasRunnableCandidate = candidates.stream()
                    .anyMatch(candidate -> canRunAnyChannel(inventory, plans, candidate));
            if (!hasRunnableCandidate) {
                fuelResolver.notRunnable(fuelStack);
            } else if (fuelResolver.canAttemptPayment(fuelStack)) {
                for (FuelProfile candidate : candidates) {
                    if (!canRunAnyChannel(inventory, plans, candidate)) continue;
                    FuelProfile authorized = fuelResolver.authorize(fuelStack, candidate);
                    if (authorized == null || !authorized.equals(candidate)) continue;
                    FuelProfile paidFuel = fuelResolver.consume(fuelStack, candidate);
                    if (paidFuel == null || !paidFuel.equals(candidate)) continue;
                    if (!paidFuel.equals(activeFuel)) {
                        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
                            changed |= resetChannel(channel);
                        }
                    }
                    activeFuel = paidFuel;
                    litTime = paidFuel.burnTicks();
                    litDuration = paidFuel.burnTicks();
                    if (fuelStack.isEmpty()) inventory.setItem(fuelSlot, ItemStack.EMPTY);
                    fuelResolver.paymentSucceeded(fuelStack, paidFuel);
                    changed = true;
                    break;
                }
                if (!isLit()) fuelResolver.paymentFailed(fuelStack);
            }
        }

        if (!isLit() || !activeFuel.usable()) return changed;
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            ItemStack input = inventory.getItem(inputSlots[channel]);
            RecipePlan plan = plans[channel];
            if (input.isEmpty() || plan == null) {
                changed |= resetChannel(channel);
                continue;
            }
            if (!plan.id().equals(activeRecipes[channel])) {
                progress[channel] = 0;
                activeRecipes[channel] = plan.id();
                changed = true;
            }
            if (totalTime[channel] != activeFuel.cookingTicks()) {
                totalTime[channel] = activeFuel.cookingTicks();
                changed = true;
            }

            int operations = plan.cycleInput() ? 1 : activeFuel.wholeStack() ? input.getCount() : 1;
            ItemStack result = inventory.getItem(resultSlots[channel]);
            if (!plan.cycleInput() && !canAcceptResult(result, plan.result(), operations)) {
                continue; // A blocked result freezes this channel's progress exactly where it is.
            }

            progress[channel]++;
            changed = true;
            if (progress[channel] >= totalTime[channel]) {
                cook(inventory, channel, plan, operations);
                progress[channel] = 0;
            }
        }
        return changed;
    }

    private RecipePlan[] resolvePlans(Container inventory, RecipeResolver resolver) {
        RecipePlan[] plans = new RecipePlan[CHANNEL_COUNT];
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            ItemStack input = inventory.getItem(inputSlots[channel]);
            if (!input.isEmpty()) plans[channel] = resolver.resolve(input).orElse(null);
        }
        return plans;
    }

    private boolean canRunAnyChannel(Container inventory, RecipePlan[] plans, FuelProfile fuel) {
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            ItemStack input = inventory.getItem(inputSlots[channel]);
            RecipePlan plan = plans[channel];
            if (input.isEmpty() || plan == null) continue;
            int operations = plan.cycleInput() ? 1 : fuel.wholeStack() ? input.getCount() : 1;
            if (plan.cycleInput()
                    || canAcceptResult(inventory.getItem(resultSlots[channel]), plan.result(), operations)) return true;
        }
        return false;
    }

    private static List<FuelProfile> sanitizeCandidates(List<FuelProfile> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        return candidates.stream()
                .filter(Objects::nonNull)
                .filter(FuelProfile::usable)
                .distinct()
                .toList();
    }

    private void cook(Container inventory, int channel, RecipePlan plan, int operations) {
        ItemStack input = inventory.getItem(inputSlots[channel]);
        if (plan.cycleInput()) {
            inventory.setItem(inputSlots[channel], plan.result());
            recipeUsage[channel].merge(plan.id(), 1, ImmortalFurnaceEngine::saturatingAdd);
            return;
        }
        ItemStack output = inventory.getItem(resultSlots[channel]);
        int produced = Math.multiplyExact(plan.result().getCount(), operations);
        input.shrink(operations);
        if (input.isEmpty()) inventory.setItem(inputSlots[channel], ItemStack.EMPTY);
        if (output.isEmpty()) inventory.setItem(resultSlots[channel], plan.result().copyWithCount(produced));
        else output.grow(produced);
        recipeUsage[channel].merge(plan.id(), operations, ImmortalFurnaceEngine::saturatingAdd);
    }

    private boolean resetChannel(int channel) {
        boolean changed = progress[channel] != 0 || activeRecipes[channel] != null;
        progress[channel] = 0;
        activeRecipes[channel] = null;
        return changed;
    }

    public boolean isLit() {
        return litTime > 0;
    }

    public int litTime() {
        return litTime;
    }

    public void setLitTime(int value) {
        litTime = Math.max(0, value);
    }

    public int litDuration() {
        return litDuration;
    }

    public void setLitDuration(int value) {
        litDuration = Math.max(0, value);
    }

    public FuelProfile activeFuel() {
        return activeFuel;
    }

    public void setActiveFuel(FuelProfile value) {
        activeFuel = value == null ? NO_FUEL : value;
    }

    public int progress(int channel) {
        checkChannel(channel);
        return progress[channel];
    }

    public void setProgress(int channel, int value) {
        checkChannel(channel);
        progress[channel] = Math.max(0, value);
    }

    public int totalTime(int channel) {
        checkChannel(channel);
        return totalTime[channel];
    }

    public void setTotalTime(int channel, int value) {
        checkChannel(channel);
        totalTime[channel] = Math.max(1, value);
    }

    public ResourceLocation activeRecipe(int channel) {
        checkChannel(channel);
        return activeRecipes[channel];
    }

    public void setActiveRecipe(int channel, ResourceLocation value) {
        checkChannel(channel);
        activeRecipes[channel] = value;
    }

    public Map<ResourceLocation, Integer> recipeUsage(int channel) {
        checkChannel(channel);
        return Map.copyOf(recipeUsage[channel]);
    }

    public void setRecipeUsage(int channel, Map<ResourceLocation, Integer> value) {
        checkChannel(channel);
        recipeUsage[channel].clear();
        Map<ResourceLocation, Integer> sanitized = value == null ? Collections.emptyMap() : value;
        sanitized.forEach((id, count) -> {
            if (id != null && count != null && count > 0) recipeUsage[channel].put(id, count);
        });
    }

    public Map<ResourceLocation, Integer> combinedRecipeUsage() {
        Map<ResourceLocation, Integer> combined = new LinkedHashMap<>();
        for (Map<ResourceLocation, Integer> channel : recipeUsage) {
            channel.forEach((id, count) -> combined.merge(id, count, ImmortalFurnaceEngine::saturatingAdd));
        }
        return Map.copyOf(combined);
    }

    public void clearRecipeUsage() {
        for (Map<ResourceLocation, Integer> channel : recipeUsage) channel.clear();
    }

    public static boolean canAcceptResult(ItemStack current, ItemStack assembled, int operations) {
        if (assembled == null || assembled.isEmpty() || operations <= 0) return false;
        long produced = (long) assembled.getCount() * operations;
        if (produced <= 0L || produced > assembled.getMaxStackSize()) return false;
        if (current == null || current.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(current, assembled)
                && current.getCount() + produced <= current.getMaxStackSize();
    }

    private static int saturatingAdd(int left, int right) {
        return left > Integer.MAX_VALUE - right ? Integer.MAX_VALUE : left + right;
    }

    private static void checkChannel(int channel) {
        if (channel < 0 || channel >= CHANNEL_COUNT) {
            throw new IndexOutOfBoundsException("channel must be in [0, 2]");
        }
    }

    public record FuelProfile(int burnTicks, int cookingTicks, boolean wholeStack) {
        public FuelProfile {
            if (burnTicks < 0 || cookingTicks <= 0) {
                throw new IllegalArgumentException("fuel timings must be non-negative and cooking time positive");
            }
        }

        public boolean usable() {
            return burnTicks > 0;
        }
    }

    public record RecipePlan(ResourceLocation id, ItemStack result, boolean cycleInput) {
        public RecipePlan(ResourceLocation id, ItemStack result) {
            this(id, result, false);
        }
        public RecipePlan {
            id = Objects.requireNonNull(id, "id");
            if (result == null || result.isEmpty()) throw new IllegalArgumentException("recipe result is required");
            result = result.copy();
        }

        @Override
        public ItemStack result() {
            return result.copy();
        }
    }

    @FunctionalInterface
    public interface FuelResolver {
        FuelProfile resolve(ItemStack stack);

        /** Ordered, side-effect-free profiles that this fuel may potentially provide. */
        default List<FuelProfile> candidates(ItemStack stack) {
            FuelProfile resolved = resolve(stack);
            return resolved == null || !resolved.usable() ? List.of() : List.of(resolved);
        }

        /** Retry/backoff gate evaluated only after at least one candidate can actually run. */
        default boolean canAttemptPayment(ItemStack stack) {
            return true;
        }

        /** Read-only availability check for one already-runnable candidate. */
        default FuelProfile authorize(ItemStack stack, FuelProfile candidate) {
            return candidate;
        }

        /**
         * Commits one ignition payment and returns the profile actually paid.
         * Reusable credentials may leave the fuel-slot stack unchanged; a
         * failed or concurrently invalidated payment returns {@link #NO_FUEL}.
         */
        default FuelProfile consume(ItemStack stack, FuelProfile profile) {
            stack.shrink(1);
            return profile;
        }

        default void paymentSucceeded(ItemStack stack, FuelProfile profile) {}

        default void paymentFailed(ItemStack stack) {}

        default void notRunnable(ItemStack stack) {}
    }

    @FunctionalInterface
    public interface RecipeResolver {
        Optional<RecipePlan> resolve(ItemStack input);
    }
}
