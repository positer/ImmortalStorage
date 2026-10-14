package com.immortalstorage.immortalstorage.compressor;
import net.minecraft.world.item.crafting.RecipeManager;

import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.recipe.ModRecipes;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinitions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

public final class SpiritCompressorRecipes {
    public record Entry(Identifier id, Ingredient input, long count, ItemStack output,
                        List<ItemStack> additionalOutputs, boolean reversed,
                        List<com.immortalstorage.immortalstorage.recipe.SpiritCompressionIngredient> additionalInputs) {
        public Entry(Identifier id, Ingredient input, long count, ItemStack output) {
            this(id, input, count, output, List.of(), false, List.of());
        }
        public Entry(Identifier id, Ingredient input, long count, ItemStack output,
                     List<ItemStack> additionalOutputs, boolean reversed) {
            this(id, input, count, output, additionalOutputs, reversed, List.of());
        }
        public List<com.immortalstorage.immortalstorage.recipe.SpiritCompressionIngredient> inputs() {
            var result = new ArrayList<com.immortalstorage.immortalstorage.recipe.SpiritCompressionIngredient>();
            result.add(new com.immortalstorage.immortalstorage.recipe.SpiritCompressionIngredient(input, Math.toIntExact(count)));
            result.addAll(additionalInputs); return List.copyOf(result);
        }
        public long totalInputCount() { return count + additionalInputs.stream().mapToLong(i -> i.count()).sum(); }
        public List<ItemStack> outputs() {
            var result = new ArrayList<ItemStack>(); result.add(output); result.addAll(additionalOutputs); return List.copyOf(result);
        }
    }

    private static volatile List<Entry> clientEntries = List.of();
    private static final java.util.Map<Level, List<Entry>> cache = new java.util.WeakHashMap<>();
    public static void installClient(List<Entry> entries) { clientEntries = List.copyOf(entries); }
    public static void invalidate() { cache.clear(); }
    public static List<Entry> entries(Level level) { return entries(level, false); }
    public static List<Entry> entries(Level level, boolean reversed) {
        return allEntries(level).stream().filter(entry -> entry.reversed() == reversed).toList();
    }
    public static List<Entry> allEntries(Level level) {
        if (level == null) return List.of();
        if (level.isClientSide()) return clientEntries;
        return cache.computeIfAbsent(level, SpiritCompressorRecipes::build);
    }
    private static List<Entry> build(Level level) {
        if (level == null) return List.of();
        ArrayList<Entry> entries = new ArrayList<>();
        for (var holder : com.immortalstorage.immortalstorage.compat.mc2612.CompatRecipeAccess.getAllRecipesFor((RecipeManager) level.recipeAccess(), ModRecipes.SPIRIT_COMPRESSION_TYPE.get())) {
            var recipe = holder.value();
            ItemStack result = recipe.outputStack(level.registryAccess());
            Ingredient inputIngredient = recipe.inputIngredient();
            var additionalInputs = new ArrayList<com.immortalstorage.immortalstorage.recipe.SpiritCompressionIngredient>();
            if (result.is(ModItems.IMMORTAL_YUAN.get())) {
                var allowed = SourceDefinitions.snapshot().definitions().values().stream()
                        .filter(source -> !source.fluid() && SourceDefinitions.outputExists(source))
                        .map(source -> BuiltInRegistries.ITEM.getValue(source.outputId()))
                        .filter(item -> recipe.inputIngredient().test(new ItemStack(item))).toArray(net.minecraft.world.item.Item[]::new);
                if (allowed.length == 0) continue;
                inputIngredient = Ingredient.of(allowed);
                boolean valid = true;
                for (var extra : recipe.additionalInputs()) {
                    var extraAllowed = SourceDefinitions.snapshot().definitions().values().stream()
                            .filter(source -> !source.fluid() && SourceDefinitions.outputExists(source))
                            .map(source -> BuiltInRegistries.ITEM.getValue(source.outputId()))
                            .filter(item -> extra.ingredient().test(new ItemStack(item))).toArray(net.minecraft.world.item.Item[]::new);
                    if (extraAllowed.length == 0) { valid = false; break; }
                    additionalInputs.add(new com.immortalstorage.immortalstorage.recipe.SpiritCompressionIngredient(
                            Ingredient.of(extraAllowed), extra.count()));
                }
                if (!valid) continue;
            } else {
                additionalInputs.addAll(recipe.additionalInputs());
            }
            entries.add(new Entry(holder.id().identifier(), inputIngredient, recipe.inputCount(), result, List.of(), false, List.copyOf(additionalInputs)));
        }
        entries.sort(java.util.Comparator.comparing(entry -> entry.id().toString()));
        for (var source : SourceDefinitions.snapshot().definitions().values().stream()
                .sorted(java.util.Comparator.comparing(definition -> definition.id().toString())).toList()) {
            if (source.fluid() || source.free() || !SourceDefinitions.outputExists(source)) continue;
            // A physical output slot bounds a transaction; large ratios are reduced exactly.
            long divisor = gcd(source.outputsPerBatch(), source.yuanCostPerBatch());
            long input = source.outputsPerBatch() / divisor, yuan = source.yuanCostPerBatch() / divisor;
            if (input > 768 || yuan > Integer.MAX_VALUE) continue;
            var item = BuiltInRegistries.ITEM.getValue(source.outputId());
            if (entries.stream().anyMatch(entry -> entry.input().test(new ItemStack(item)))) continue;
            entries.add(new Entry(Identifier.fromNamespaceAndPath("immortalstorage", "source_compression/" + source.id().getNamespace() + "/" + source.id().getPath()),
                    Ingredient.of(item), input, new ItemStack(ModItems.IMMORTAL_YUAN.get(), (int) yuan)));
        }
        for (var holder : com.immortalstorage.immortalstorage.compat.mc2612.CompatRecipeAccess.getAllRecipesFor((RecipeManager) level.recipeAccess(), ModRecipes.SPIRIT_DECOMPRESSION_TYPE.get())) {
            var recipe = holder.value();
            entries.add(new Entry(holder.id().identifier(), recipe.inputIngredient(), recipe.inputCount(),
                    recipe.outputStack(level.registryAccess()), recipe.additionalResults(), true));
        }
        return List.copyOf(entries);
    }
    public static boolean accepts(Level level, ItemStack stack) { return accepts(level, stack, false); }
    public static boolean accepts(Level level, ItemStack stack, boolean reversed) {
        return !stack.isEmpty() && entries(level, reversed).stream().anyMatch(entry -> entry.inputs().stream().anyMatch(input -> input.ingredient().test(stack)));
    }
    private static long gcd(long a, long b) { while (b != 0) { long t = a % b; a = b; b = t; } return a; }
    private SpiritCompressorRecipes() {}
}
