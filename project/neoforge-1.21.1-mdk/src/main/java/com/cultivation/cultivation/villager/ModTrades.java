package com.cultivation.cultivation.villager;

import com.cultivation.cultivation.CultivationMod;
import com.cultivation.cultivation.item.ModItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Optional;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;

public final class ModTrades {
    private ModTrades() {}

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModVillagers.IMMORTAL_SAGE.get()) return;
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

        trades.computeIfAbsent(1, k -> new java.util.ArrayList<>()).addAll(List.of(
                new RandomCountItemsForEmeralds(Items.EMERALD, 5, 6, ModItems.SPIRIT_IRON.get(), 2, 3, 12, 5, 0.05f),
                new RandomCountItemsForEmeralds(Items.EMERALD, 2, 3, ModItems.CRUDE_PILL_EMBRYO.get(), 1, 2, 12, 3, 0.05f)
        ));
        trades.computeIfAbsent(2, k -> new java.util.ArrayList<>()).addAll(List.of(
                new RandomCountEmeraldForItems(ModItems.CRUDE_PILL.get(), 1, 4, 2, 3, 8, 4, 0.05f),
                new RandomCountItemsAndEmeraldsToItems(
                        ModItems.CRUDE_SPIRIT_IRON.get(), 16,
                        Items.EMERALD, 5,
                        ModItems.SPIRIT_IRON.get(), 16,
                        8, 6, 0.05f)
        ));
        trades.computeIfAbsent(3, k -> new java.util.ArrayList<>()).addAll(List.of(
                new RandomCountItemsAndEmeraldsToItems(
                        Items.RAW_IRON, 16,
                        ModItems.TRUE_YUAN.get(), 5,
                        ModItems.CRUDE_SPIRIT_IRON.get(), 16,
                        6, 8, 0.05f),
                new RandomCountItemsAndEmeraldsToItems(
                        Items.DIAMOND, 6,
                        ModItems.TRUE_YUAN.get(), 20,
                        ModItems.SPIRIT_CRYSTAL.get(), 6,
                        6, 10, 0.05f)
        ));
        trades.computeIfAbsent(4, k -> new java.util.ArrayList<>()).addAll(List.of(
                new CustomTrade(64, ModItems.TRUE_YUAN.get(), 18,
                        new ItemStack(ModItems.SPIRIT_CORE.get()), 4, 15, 0.05f)
        ));
        trades.computeIfAbsent(5, k -> new java.util.ArrayList<>()).addAll(List.of(
                new RandomEnchantedItemForEmeralds(
                        new ItemStack(ModItems.SPIRIT_SWORD.get()),
                        32, 10, 3, 12, 0.05f, true)
        ));
    }

    /** Vanilla librarians receive the data-driven Spirit Repair book at master level. */
    @SubscribeEvent
    public static void onLibrarianTrades(VillagerTradesEvent event) {
        if (event.getType() != net.minecraft.world.entity.npc.VillagerProfession.LIBRARIAN) return;
        event.getTrades().computeIfAbsent(5, ignored -> new java.util.ArrayList<>())
                .add((entity, random) -> entity.level().registryAccess()
                        .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .get(com.cultivation.cultivation.enchantment.ModEnchantments.SPIRIT_REPAIR)
                        .map(holder -> new MerchantOffer(
                                new ItemCost(Items.EMERALD, 24),
                                Optional.of(new ItemCost(Items.BOOK, 1)),
                                net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
                                        new net.minecraft.world.item.enchantment.EnchantmentInstance(holder, 1)),
                                0, 12, 30, 0.2F))
                        .orElse(null));
    }

    public static class RandomCountItemsForEmeralds implements VillagerTrades.ItemListing {
        private final net.minecraft.world.level.ItemLike fromItem;
        private final int minFromCount;
        private final int maxFromCount;
        private final net.minecraft.world.level.ItemLike toItem;
        private final int minToCount;
        private final int maxToCount;
        private final int maxUses;
        private final int xp;
        private final float multiplier;

        public RandomCountItemsForEmeralds(net.minecraft.world.level.ItemLike from, int minFrom, int maxFrom,
                                           net.minecraft.world.level.ItemLike to, int minTo, int maxTo,
                                           int maxUses, int xp, float mult) {
            this.fromItem = from;
            this.minFromCount = minFrom;
            this.maxFromCount = maxFrom;
            this.toItem = to;
            this.minToCount = minTo;
            this.maxToCount = maxTo;
            this.maxUses = maxUses;
            this.xp = xp;
            this.multiplier = mult;
        }

        @Override
        public MerchantOffer getOffer(Entity entity, RandomSource rand) {
            int fromCount = minFromCount + rand.nextInt(maxFromCount - minFromCount + 1);
            int toCount = minToCount + rand.nextInt(maxToCount - minToCount + 1);
            return new MerchantOffer(
                    new ItemCost(fromItem, fromCount),
                    new ItemStack(toItem, toCount),
                    maxUses, xp, multiplier);
        }
    }

    public static class RandomCountEmeraldForItems implements VillagerTrades.ItemListing {
        private final net.minecraft.world.level.ItemLike fromItem;
        private final int minFromCount;
        private final int maxFromCount;
        private final int minEmerald;
        private final int maxEmerald;
        private final int maxUses;
        private final int xp;
        private final float multiplier;

        public RandomCountEmeraldForItems(net.minecraft.world.level.ItemLike from, int minFrom, int maxFrom,
                                          int minEmerald, int maxEmerald, int maxUses, int xp, float mult) {
            this.fromItem = from;
            this.minFromCount = minFrom;
            this.maxFromCount = maxFrom;
            this.minEmerald = minEmerald;
            this.maxEmerald = maxEmerald;
            this.maxUses = maxUses;
            this.xp = xp;
            this.multiplier = mult;
        }

        @Override
        public MerchantOffer getOffer(Entity entity, RandomSource rand) {
            int fromCount = minFromCount + rand.nextInt(maxFromCount - minFromCount + 1);
            int emeraldCount = minEmerald + rand.nextInt(maxEmerald - minEmerald + 1);
            return new MerchantOffer(
                    new ItemCost(Items.EMERALD, emeraldCount),
                    new ItemStack(fromItem, fromCount),
                    maxUses, xp, multiplier);
        }
    }

    public static class RandomCountItemsAndEmeraldsToItems implements VillagerTrades.ItemListing {
        private final net.minecraft.world.level.ItemLike firstItem;
        private final int firstCount;
        private final net.minecraft.world.level.ItemLike secondItem;
        private final int secondCount;
        private final net.minecraft.world.level.ItemLike resultItem;
        private final int resultCount;
        private final int maxUses;
        private final int xp;
        private final float multiplier;

        public RandomCountItemsAndEmeraldsToItems(net.minecraft.world.level.ItemLike first, int firstCount,
                                                   net.minecraft.world.level.ItemLike second, int secondCount,
                                                   net.minecraft.world.level.ItemLike result, int resultCount,
                                                   int maxUses, int xp, float mult) {
            this.firstItem = first;
            this.firstCount = firstCount;
            this.secondItem = second;
            this.secondCount = secondCount;
            this.resultItem = result;
            this.resultCount = resultCount;
            this.maxUses = maxUses;
            this.xp = xp;
            this.multiplier = mult;
        }

        @Override
        public MerchantOffer getOffer(Entity entity, RandomSource rand) {
            return new MerchantOffer(
                    new ItemCost(firstItem, firstCount),
                    Optional.of(new ItemCost(secondItem, secondCount)),
                    new ItemStack(resultItem, resultCount),
                    0, maxUses, xp, multiplier);
        }
    }

    public static class CustomTrade implements VillagerTrades.ItemListing {
        private final int emeraldCount;
        private final net.minecraft.world.level.ItemLike secondItem;
        private final int secondCount;
        private final ItemStack result;
        private final int maxUses;
        private final int xp;
        private final float multiplier;

        public CustomTrade(int emeraldCount, net.minecraft.world.level.ItemLike secondItem, int secondCount,
                           ItemStack result, int maxUses, int xp, float mult) {
            this.emeraldCount = emeraldCount;
            this.secondItem = secondItem;
            this.secondCount = secondCount;
            this.result = result;
            this.maxUses = maxUses;
            this.xp = xp;
            this.multiplier = mult;
        }

        @Override
        public MerchantOffer getOffer(Entity entity, RandomSource rand) {
            return new MerchantOffer(
                    new ItemCost(Items.EMERALD, emeraldCount),
                    Optional.of(new ItemCost(secondItem, secondCount)),
                    result.copy(),
                    0, maxUses, xp, multiplier);
        }
    }

    public static class RandomEnchantedItemForEmeralds implements VillagerTrades.ItemListing {
        private final ItemStack itemStack;
        private final int emeraldCost;
        private final int minLevel;
        private final int maxLevel;
        private final int maxUses;
        private final float multiplier;

        public RandomEnchantedItemForEmeralds(ItemStack stack, int emeraldCost, int maxUses, int minLevel, int maxLevel, float mult, boolean treasure) {
            this.itemStack = stack;
            this.emeraldCost = emeraldCost;
            this.maxUses = maxUses;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.multiplier = mult;
        }

        @Override
        public MerchantOffer getOffer(Entity entity, RandomSource rand) {
            ItemStack out = itemStack.copy();
            int level = 15 + rand.nextInt(15);
            var lookup = entity.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var stream = lookup.listElements()
                    .filter(h -> h.value().canEnchant(out))
                    .map(h -> (net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>) h);
            net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(rand, out, level, stream);
            return new MerchantOffer(
                    new ItemCost(Items.EMERALD, emeraldCost),
                    out, maxUses, 10, multiplier);
        }
    }
}
