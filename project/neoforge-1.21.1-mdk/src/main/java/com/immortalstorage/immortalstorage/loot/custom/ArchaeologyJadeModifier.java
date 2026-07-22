package com.immortalstorage.immortalstorage.loot.custom;

import com.immortalstorage.immortalstorage.config.ImmortalStorageConfig;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;

/** Config-driven Ancient Jade injection for every vanilla suspicious-block archaeology table. */
public final class ArchaeologyJadeModifier extends LootModifier {
    public static final MapCodec<ArchaeologyJadeModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(LootItemCondition.DIRECT_CODEC.listOf().fieldOf("conditions")
                    .forGetter(modifier -> List.of(modifier.conditions)))
                    .apply(instance, ArchaeologyJadeModifier::new));

    public ArchaeologyJadeModifier(List<LootItemCondition> conditions) {
        super(conditions.toArray(LootItemCondition[]::new));
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (context.getRandom().nextDouble() < ImmortalStorageConfig.JADE_GUIDE_ARCHAEOLOGY_CHANCE.get()) {
            loot.add(new ItemStack(ModItems.JADE_GUIDE.get()));
        }
        return loot;
    }

    @Override public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
