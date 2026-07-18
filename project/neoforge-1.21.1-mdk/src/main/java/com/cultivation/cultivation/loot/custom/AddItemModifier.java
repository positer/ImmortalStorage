package com.cultivation.cultivation.loot.custom;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;

public class AddItemModifier extends LootModifier {
    public static final MapCodec<AddItemModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            inst.group(
                    LootItemCondition.DIRECT_CODEC.listOf().fieldOf("conditions").forGetter(m -> List.of(m.conditions)),
                    ItemStack.CODEC.fieldOf("item").forGetter(AddItemModifier::getItem),
                    com.mojang.serialization.Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(AddItemModifier::getChance),
                    com.mojang.serialization.Codec.INT.optionalFieldOf("minCount", 1).forGetter(AddItemModifier::getMinCount),
                    com.mojang.serialization.Codec.INT.optionalFieldOf("maxCount", 1).forGetter(AddItemModifier::getMaxCount)
            ).apply(inst, AddItemModifier::new));

    private final ItemStack item;
    private final float chance;
    private final int minCount;
    private final int maxCount;

    public AddItemModifier(List<LootItemCondition> conditions, ItemStack item, float chance, int minCount, int maxCount) {
        super(conditions.toArray(new LootItemCondition[0]));
        this.item = item;
        this.chance = chance;
        this.minCount = Math.max(1, minCount);
        this.maxCount = Math.max(minCount, maxCount);
    }

    public AddItemModifier(LootItemCondition[] conditions) {
        this(List.of(conditions), new ItemStack(net.minecraft.world.item.Items.AIR), 1.0f, 1, 1);
    }

    public ItemStack getItem() { return item; }
    public float getChance() { return chance; }
    public int getMinCount() { return minCount; }
    public int getMaxCount() { return maxCount; }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (item == null || item.isEmpty()) return generatedLoot;
        if (context.getRandom().nextFloat() > chance) return generatedLoot;
        int count = minCount + (maxCount > minCount ? context.getRandom().nextInt(maxCount - minCount + 1) : 0);
        ItemStack copy = item.copyWithCount(count);
        generatedLoot.add(copy);
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
